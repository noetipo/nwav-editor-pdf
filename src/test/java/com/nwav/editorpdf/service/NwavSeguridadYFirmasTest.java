package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavContrasenaIncorrectaException;
import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.model.NwavColorRgb;
import com.nwav.editorpdf.model.NwavFirma;
import com.nwav.editorpdf.model.NwavFuente;
import com.nwav.editorpdf.model.NwavMarcaAgua;
import com.nwav.editorpdf.model.NwavOpcionesMarcaAgua;
import com.nwav.editorpdf.model.NwavOpcionesProteccion;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.model.NwavTipoMarcaAgua;
import com.nwav.editorpdf.repository.NwavFirmaRepository;
import com.nwav.editorpdf.repository.NwavMarcaAguaRepository;
import com.nwav.editorpdf.soporte.NwavLectorPdf;
import com.nwav.editorpdf.soporte.NwavPdfDePrueba;
import com.nwav.editorpdf.soporte.NwavPruebaConBaseDatos;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NwavSeguridadYFirmasTest extends NwavPruebaConBaseDatos {
    @Autowired private NwavEdicionService nwavEdicion;
    @Autowired private NwavArchivoPdfService nwavArchivos;
    @Autowired private NwavPdfDocumentoService nwavDocumentos;
    @Autowired private NwavFirmaService nwavFirmas;
    @Autowired private NwavMarcaAguaService nwavMarcas;
    @Autowired private NwavFirmaRepository nwavFirmaRepositorio;
    @Autowired private NwavMarcaAguaRepository nwavMarcaRepositorio;
    @Autowired private NwavPdfRenderService nwavRender;

    @TempDir
    Path nwavCarpeta;

    @BeforeEach
    void nwavLimpiar() {
        nwavFirmaRepositorio.deleteAll();
        nwavMarcaRepositorio.deleteAll();
    }

    private static NwavOpcionesProteccion nwavProteccion(String nwavUsuario, String nwavPropietario,
                                                         boolean nwavImprimir) {
        return new NwavOpcionesProteccion(nwavUsuario, nwavPropietario, nwavImprimir, false, false, false, false);
    }

    @Test
    void nwavProtegerSeAplicaAlGuardarConPermisos() throws Exception {
        NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(NwavPdfDePrueba.nwavCrear(nwavCarpeta, "p.pdf", 2), null);
        nwavEdicion.nwavProteger(nwavDocumento, nwavProteccion("abrir123", "duenio456", false));
        assertThat(nwavDocumento.nwavHayCambiosSinGuardar()).isTrue();
        assertThat(nwavDocumento.getNwavProteccionPendiente()).isNotNull();

        Path nwavCopia = nwavCarpeta.resolve("protegido.pdf");
        try (NwavDocumentoPdf nwavGuardado = nwavArchivos.nwavGuardarComo(nwavDocumento, nwavCopia)) {
            assertThat(nwavGuardado.isNwavCifrado()).isTrue();
            assertThat(nwavGuardado.getNwavProteccionPendiente()).isNull();
        }
        assertThat(nwavDocumentos.nwavRequiereContrasena(nwavCopia)).isTrue();
        try (PDDocument nwavUsuario = Loader.loadPDF(nwavCopia.toFile(), "abrir123")) {
            assertThat(nwavUsuario.getCurrentAccessPermission().isOwnerPermission()).isFalse();
            assertThat(nwavUsuario.getCurrentAccessPermission().canPrint()).isFalse();
        }
        try (PDDocument nwavPropietario = Loader.loadPDF(nwavCopia.toFile(), "duenio456")) {
            assertThat(nwavPropietario.getCurrentAccessPermission().isOwnerPermission()).isTrue();
        }
    }

    @Test
    void nwavDeshacerLaProteccionAntesDeGuardar() throws Exception {
        NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(NwavPdfDePrueba.nwavCrear(nwavCarpeta, "p.pdf", 1), null);
        nwavEdicion.nwavProteger(nwavDocumento, nwavProteccion("abrir123", "", true));
        nwavEdicion.nwavDeshacer(nwavDocumento);
        assertThat(nwavDocumento.getNwavProteccionPendiente()).isNull();

        Path nwavCopia = nwavCarpeta.resolve("libre.pdf");
        nwavArchivos.nwavGuardarComo(nwavDocumento, nwavCopia).close();
        assertThat(nwavDocumentos.nwavRequiereContrasena(nwavCopia)).isFalse();
    }

    @Test
    void nwavQuitarProteccionExigeLaContrasenaDePropietario() throws Exception {
        NwavDocumentoPdf nwavOriginal = nwavDocumentos.nwavAbrir(NwavPdfDePrueba.nwavCrear(nwavCarpeta, "q.pdf", 1), null);
        nwavEdicion.nwavProteger(nwavOriginal, nwavProteccion("abrir123", "duenio456", true));
        Path nwavProtegido = nwavCarpeta.resolve("q-protegido.pdf");
        nwavArchivos.nwavGuardarComo(nwavOriginal, nwavProtegido).close();

        NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavProtegido, "abrir123");
        assertThatThrownBy(() -> nwavEdicion.nwavQuitarProteccion(nwavDocumento, null))
                .isInstanceOf(NwavContrasenaIncorrectaException.class);
        assertThatThrownBy(() -> nwavEdicion.nwavQuitarProteccion(nwavDocumento, "otra"))
                .isInstanceOf(NwavContrasenaIncorrectaException.class);

        nwavEdicion.nwavQuitarProteccion(nwavDocumento, "duenio456");
        Path nwavLibre = nwavCarpeta.resolve("q-libre.pdf");
        try (NwavDocumentoPdf nwavGuardado = nwavArchivos.nwavGuardarComo(nwavDocumento, nwavLibre)) {
            assertThat(nwavGuardado.isNwavCifrado()).isFalse();
        }
        assertThat(nwavDocumentos.nwavRequiereContrasena(nwavLibre)).isFalse();
        assertThat(NwavLectorPdf.nwavTextos(nwavLibre)).containsExactly("Pagina1");
    }

    @Test
    void nwavValidacionesDeProteccion() throws Exception {
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(
                NwavPdfDePrueba.nwavCrear(nwavCarpeta, "v.pdf", 1), null)) {
            assertThatThrownBy(() -> nwavEdicion.nwavProteger(nwavDocumento, nwavProteccion("abc", "", true)))
                    .hasMessageContaining("al menos 4");
            assertThatThrownBy(() -> nwavEdicion.nwavProteger(nwavDocumento, nwavProteccion("igual1", "igual1", true)))
                    .hasMessageContaining("distinta");
            assertThatThrownBy(() -> nwavEdicion.nwavQuitarProteccion(nwavDocumento, "x"))
                    .isInstanceOf(NwavEditorException.class).hasMessageContaining("no está protegido");
        }
    }

    private static byte[] nwavFirmaEscaneada() throws Exception {
        BufferedImage nwavImagen = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D nwavLienzo = nwavImagen.createGraphics();
        nwavLienzo.setColor(Color.WHITE);
        nwavLienzo.fillRect(0, 0, 400, 200);
        nwavLienzo.setColor(new Color(10, 20, 120));
        nwavLienzo.fillRect(100, 80, 150, 30);
        nwavLienzo.dispose();
        ByteArrayOutputStream nwavSalida = new ByteArrayOutputStream();
        ImageIO.write(nwavImagen, "jpg", nwavSalida);
        return nwavSalida.toByteArray();
    }

    @Test
    void nwavPrepararFirmaQuitaElFondoYRecorta() throws Exception {
        byte[] nwavPng = nwavFirmas.nwavPrepararImagen(nwavFirmaEscaneada(), true);
        BufferedImage nwavFirma = ImageIO.read(new ByteArrayInputStream(nwavPng));

        assertThat(nwavFirma.getWidth()).isBetween(150, 162);
        assertThat(nwavFirma.getHeight()).isBetween(30, 42);
        assertThat(nwavFirma.getColorModel().hasAlpha()).isTrue();
        assertThat((nwavFirma.getRGB(0, 0) >>> 24) & 0xFF).as("esquina transparente").isZero();
        assertThat((nwavFirma.getRGB(nwavFirma.getWidth() / 2, nwavFirma.getHeight() / 2) >>> 24) & 0xFF).isEqualTo(255);
    }

    @Test
    void nwavGuardarListarYEliminarFirmas() throws Exception {
        byte[] nwavPng = nwavFirmas.nwavPrepararImagen(nwavFirmaEscaneada(), true);
        NwavFirma nwavGuardada = nwavFirmas.nwavGuardar("Mi firma", nwavPng);

        assertThat(nwavFirmas.nwavListar()).singleElement().satisfies(nwavFirma -> {
            assertThat(nwavFirma.getNwavNombre()).isEqualTo("Mi firma");
            assertThat(nwavFirma.getNwavImagen()).isEqualTo(nwavPng);
        });
        nwavFirmas.nwavEliminar(nwavGuardada.getNwavId());
        assertThat(nwavFirmas.nwavListar()).isEmpty();

        assertThatThrownBy(() -> nwavFirmas.nwavGuardar(" ", nwavPng)).hasMessageContaining("nombre");
        assertThatThrownBy(() -> nwavFirmas.nwavGuardar("x", "no-imagen".getBytes())).hasMessageContaining("imagen");
        assertThatThrownBy(() -> nwavFirmas.nwavPrepararImagen(nwavPngEnBlanco(), true)).hasMessageContaining("vacía");
    }

    private static byte[] nwavPngEnBlanco() throws Exception {
        BufferedImage nwavImagen = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D nwavLienzo = nwavImagen.createGraphics();
        nwavLienzo.setColor(Color.WHITE);
        nwavLienzo.fillRect(0, 0, 50, 50);
        nwavLienzo.dispose();
        ByteArrayOutputStream nwavSalida = new ByteArrayOutputStream();
        ImageIO.write(nwavImagen, "png", nwavSalida);
        return nwavSalida.toByteArray();
    }

    @Test
    void nwavInsertarFirmaEnLaPaginaConTransparencia() throws Exception {
        byte[] nwavPng = nwavFirmas.nwavPrepararImagen(nwavFirmaEscaneada(), true);
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(
                NwavPdfDePrueba.nwavCrear(nwavCarpeta, "firma.pdf", 1), null)) {
            nwavEdicion.nwavInsertarFirma(nwavDocumento, 0, nwavPng, new NwavRectangulo(300, 600, 160, 40));

            BufferedImage nwavPagina = nwavRender.nwavRenderizarPagina(nwavDocumento, 0, 1f);
            Color nwavTrazo = new Color(nwavPagina.getRGB(380, 620));
            assertThat(nwavTrazo.getBlue()).isGreaterThan(nwavTrazo.getRed() + 50);
            assertThat(new Color(nwavPagina.getRGB(302, 602))).isEqualTo(Color.WHITE);
        }
    }

    @Test
    void nwavPlantillasDeMarcaDeAgua() throws Exception {
        NwavOpcionesMarcaAgua nwavTexto = new NwavOpcionesMarcaAgua(NwavTipoMarcaAgua.NWAV_TEXTO, "BORRADOR", null,
                NwavFuente.NWAV_TIMES, 72, new NwavColorRgb(200, 0, 0), 0.25, 30, 1, "");
        nwavMarcas.nwavGuardar("Borrador", nwavTexto);
        nwavMarcas.nwavGuardar("borrador", new NwavOpcionesMarcaAgua(NwavTipoMarcaAgua.NWAV_TEXTO, "BORRADOR 2", null,
                NwavFuente.NWAV_TIMES, 72, new NwavColorRgb(200, 0, 0), 0.25, 30, 1, ""));
        byte[] nwavPng = Files.readAllBytes(NwavLectorPdf.nwavCrearPng(nwavCarpeta, "sello.png", 40, 40));
        nwavMarcas.nwavGuardar("Logo", new NwavOpcionesMarcaAgua(NwavTipoMarcaAgua.NWAV_IMAGEN, null, nwavPng, null, 0,
                null, 0.4, 0, 0.3, ""));

        assertThat(nwavMarcas.nwavListar()).extracting(NwavMarcaAgua::getNwavNombre).containsExactly("borrador", "Logo");
        NwavOpcionesMarcaAgua nwavLeida = NwavMarcaAguaService.nwavAOpciones(nwavMarcas.nwavListar().get(0));
        assertThat(nwavLeida.nwavTexto()).isEqualTo("BORRADOR 2");
        assertThat(nwavLeida.nwavFuente()).isEqualTo(NwavFuente.NWAV_TIMES);
        assertThat(nwavLeida.nwavColor()).isEqualTo(new NwavColorRgb(200, 0, 0));
        assertThat(nwavLeida.nwavRotacion()).isEqualTo(30);
        assertThat(NwavMarcaAguaService.nwavAOpciones(nwavMarcas.nwavListar().get(1)).nwavImagen()).isEqualTo(nwavPng);
    }
}
