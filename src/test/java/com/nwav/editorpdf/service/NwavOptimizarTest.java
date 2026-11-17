package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavCanceladoException;
import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.model.NwavNivelCompresion;
import com.nwav.editorpdf.model.NwavOpcionesCompresion;
import com.nwav.editorpdf.service.herramienta.NwavDependenciasService;
import com.nwav.editorpdf.soporte.NwavLectorPdf;
import com.nwav.editorpdf.soporte.NwavPdfConImagenes;
import com.nwav.editorpdf.soporte.NwavPdfDePrueba;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NwavOptimizarTest {
    private final NwavPdfDocumentoService nwavDocumentos = new NwavPdfDocumentoService();
    private final NwavCompresorService nwavCompresor = new NwavCompresorService(nwavDocumentos);
    private final NwavReparacionService nwavReparacion = new NwavReparacionService();

    @TempDir
    Path nwavCarpeta;

    private static List<PDImageXObject> nwavImagenes(PDDocument nwavPd, int nwavPagina) throws Exception {
        PDResources nwavRecursos = nwavPd.getPage(nwavPagina).getResources();
        List<PDImageXObject> nwavLista = new ArrayList<>();
        for (COSName nwavNombre : nwavRecursos.getXObjectNames()) {
            if (nwavRecursos.getXObject(nwavNombre) instanceof PDImageXObject nwavImagen) {
                nwavLista.add(nwavImagen);
            }
        }
        return nwavLista;
    }

    @Test
    void nwavComprimirReduceElTamanoYConservaPaginasYTexto() throws Exception {
        Path nwavOriginal = NwavPdfConImagenes.nwavCrearPesado(nwavCarpeta, "pesado.pdf");
        Path nwavDestino = nwavCarpeta.resolve("pesado_comprimido.pdf");

        NwavCompresorService.NwavResultadoCompresion nwavResultado = nwavCompresor.nwavComprimir(nwavOriginal, null,
                NwavOpcionesCompresion.nwavDe(NwavNivelCompresion.NWAV_RECOMENDADA, false, false), nwavDestino,
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);

        assertThat(nwavResultado.nwavGuardado()).isTrue();
        assertThat(nwavResultado.nwavDespues()).isLessThan(nwavResultado.nwavAntes());
        assertThat(Files.size(nwavDestino)).isEqualTo(nwavResultado.nwavDespues());
        assertThat(nwavResultado.nwavReduccion()).isGreaterThan(30);
        assertThat(nwavResultado.nwavImagenesDuplicadas()).isGreaterThanOrEqualTo(1);
        assertThat(nwavResultado.nwavResumen()).contains("−");
        assertThat(NwavLectorPdf.nwavTextos(nwavDestino))
                .containsExactly("Pagina1textodeprueba", "Pagina2textodeprueba");

        try (PDDocument nwavComprimido = Loader.loadPDF(nwavDestino.toFile())) {
            assertThat(nwavComprimido.getNumberOfPages()).isEqualTo(2);
            List<PDImageXObject> nwavImagenes = nwavImagenes(nwavComprimido, 0);
            PDImageXObject nwavFoto = nwavImagenes.stream().filter(nwavImagen -> "jpg".equals(nwavImagen.getSuffix())
                    && nwavImagen.getCOSObject().getCOSStream(COSName.SMASK) == null).findFirst().orElseThrow();
            assertThat(nwavFoto.getWidth()).isBetween(440, 460);
            assertThat(nwavFoto.getSuffix()).isEqualTo("jpg");
            assertThat(nwavImagenes).anySatisfy(nwavImagen ->
                    assertThat(nwavImagen.getCOSObject().getCOSStream(COSName.SMASK)).isNotNull());
            assertThat(nwavImagenes).anySatisfy(nwavImagen -> assertThat(nwavImagen.getBitsPerComponent()).isEqualTo(1));
            PDImageXObject nwavFotoPagina2 = nwavImagenes(nwavComprimido, 1).get(0);
            assertThat(nwavFotoPagina2.getCOSObject()).isSameAs(nwavFoto.getCOSObject());
        }
        assertThat(Files.size(nwavOriginal)).as("el original no cambia").isEqualTo(nwavResultado.nwavAntes());
    }

    @Test
    void nwavElNivelExtremoReduceMasQueElBajo() throws Exception {
        Path nwavOriginal = NwavPdfConImagenes.nwavCrearPesado(nwavCarpeta, "p.pdf");
        long nwavBaja = nwavCompresor.nwavComprimir(nwavOriginal, null,
                NwavOpcionesCompresion.nwavDe(NwavNivelCompresion.NWAV_BAJA, false, false),
                nwavCarpeta.resolve("baja.pdf"), NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA).nwavDespues();
        long nwavExtrema = nwavCompresor.nwavComprimir(nwavOriginal, null,
                NwavOpcionesCompresion.nwavDe(NwavNivelCompresion.NWAV_EXTREMA, false, false),
                nwavCarpeta.resolve("extrema.pdf"), NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA).nwavDespues();

        assertThat(nwavExtrema).isLessThan(nwavBaja);
        try (PDDocument nwavPd = Loader.loadPDF(nwavCarpeta.resolve("extrema.pdf").toFile())) {
            assertThat(nwavPd.getDocumentInformation().getTitle()).as("extrema quita metadatos").isNull();
        }
    }

    @Test
    void nwavEscalaDeGrisesAfectaImagenesYColores() throws Exception {
        Path nwavOriginal = NwavPdfConImagenes.nwavCrearPesado(nwavCarpeta, "color.pdf");
        Path nwavDestino = nwavCarpeta.resolve("gris.pdf");
        nwavCompresor.nwavComprimir(nwavOriginal, null, new NwavOpcionesCompresion(150, 0.75f, true, false), nwavDestino,
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);

        try (PDDocument nwavPd = Loader.loadPDF(nwavDestino.toFile())) {
            BufferedImage nwavImagen = new PDFRenderer(nwavPd).renderImage(0, 1f);
            Color nwavRectangulo = new Color(nwavImagen.getRGB(450, 842 - 725));
            Color nwavFoto = new Color(nwavImagen.getRGB(180, 842 - 470));
            for (Color nwavColor : List.of(nwavRectangulo, nwavFoto)) {
                assertThat(Math.abs(nwavColor.getRed() - nwavColor.getGreen())).isLessThan(8);
                assertThat(Math.abs(nwavColor.getGreen() - nwavColor.getBlue())).isLessThan(8);
            }
        }
    }

    @Test
    void nwavSiNoMejoraNoSeGuardaNada() throws Exception {
        Path nwavOriginal = NwavPdfConImagenes.nwavCrearPesado(nwavCarpeta, "o.pdf");
        Path nwavPrimera = nwavCarpeta.resolve("primera.pdf");
        NwavOpcionesCompresion nwavOpciones = NwavOpcionesCompresion.nwavDe(NwavNivelCompresion.NWAV_EXTREMA, false, true);
        nwavCompresor.nwavComprimir(nwavOriginal, null, nwavOpciones, nwavPrimera, NwavAvance.NWAV_NINGUNO,
                NwavCancelacion.NWAV_NUNCA);

        Path nwavSegunda = nwavCarpeta.resolve("segunda.pdf");
        NwavCompresorService.NwavResultadoCompresion nwavResultado = nwavCompresor.nwavComprimir(nwavPrimera, null,
                nwavOpciones, nwavSegunda, NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);

        if (!nwavResultado.nwavGuardado()) {
            assertThat(nwavSegunda).doesNotExist();
            assertThat(nwavResultado.nwavResumen()).contains("No se guardó");
        } else {
            assertThat(nwavResultado.nwavDespues()).isLessThan(nwavResultado.nwavAntes());
        }
    }

    @Test
    void nwavNuncaSobrescribeElOriginalYSePuedeCancelar() throws Exception {
        Path nwavOriginal = NwavPdfConImagenes.nwavCrearPesado(nwavCarpeta, "x.pdf");
        NwavOpcionesCompresion nwavOpciones = NwavOpcionesCompresion.nwavDe(NwavNivelCompresion.NWAV_RECOMENDADA, false, false);

        assertThatThrownBy(() -> nwavCompresor.nwavComprimir(nwavOriginal, null, nwavOpciones, nwavOriginal,
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA)).isInstanceOf(NwavEditorException.class);
        assertThatThrownBy(() -> nwavCompresor.nwavComprimir(nwavOriginal, null, nwavOpciones,
                nwavCarpeta.resolve("c.pdf"), NwavAvance.NWAV_NINGUNO, () -> true))
                .isInstanceOf(NwavCanceladoException.class);
        assertThat(nwavCarpeta.resolve("c.pdf")).doesNotExist();
    }

    @Test
    void nwavComprimirUnPdfProtegidoLoMantieneProtegido() throws Exception {
        Path nwavProtegido = NwavPdfDePrueba.nwavCrearCifrado(nwavCarpeta, "prot.pdf", 2, "clave");
        Path nwavDestino = nwavCarpeta.resolve("prot_c.pdf");
        NwavCompresorService.NwavResultadoCompresion nwavResultado = nwavCompresor.nwavComprimir(nwavProtegido, "clave",
                NwavOpcionesCompresion.nwavDe(NwavNivelCompresion.NWAV_RECOMENDADA, false, false), nwavDestino,
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);
        if (nwavResultado.nwavGuardado()) {
            assertThat(nwavDocumentos.nwavRequiereContrasena(nwavDestino)).isTrue();
        }
    }

    @Test
    void nwavRepararRecuperaUnPdfTruncado() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "sano.pdf", 3);
        byte[] nwavBytes = Files.readAllBytes(nwavOriginal);
        Path nwavRoto = Files.write(nwavCarpeta.resolve("roto.pdf"),
                java.util.Arrays.copyOf(nwavBytes, (int) (nwavBytes.length * 0.9)));
        Path nwavDestino = nwavCarpeta.resolve("roto_reparado.pdf");

        NwavReparacionService.NwavResultadoReparacion nwavResultado = nwavReparacion.nwavReparar(nwavRoto, null,
                nwavDestino, NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);

        assertThat(nwavResultado.nwavPaginas()).isGreaterThanOrEqualTo(1);
        try (PDDocument nwavReparado = Loader.loadPDF(nwavDestino.toFile())) {
            assertThat(nwavReparado.getNumberOfPages()).isEqualTo(nwavResultado.nwavPaginas());
        }
        assertThat(nwavResultado.nwavResumen()).contains("recuperada");
    }

    @Test
    void nwavRepararInformaCuandoNoEsRecuperable() throws Exception {
        Path nwavBasura = Files.writeString(nwavCarpeta.resolve("basura.pdf"), "esto no es un pdf en absoluto");

        assertThatThrownBy(() -> nwavReparacion.nwavReparar(nwavBasura, null, nwavCarpeta.resolve("r.pdf"),
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA))
                .isInstanceOf(NwavEditorException.class)
                .hasMessageContaining("no se puede recuperar");
    }

    @Test
    void nwavOcrConvierteUnEscaneadoEnTextoBuscable() throws Exception {
        NwavConfiguracionService nwavConfiguracion = org.mockito.Mockito.mock(NwavConfiguracionService.class);
        org.mockito.Mockito.when(nwavConfiguracion.nwavObtener(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(java.util.Optional.empty());
        NwavDependenciasService nwavDependencias = new NwavDependenciasService(nwavConfiguracion,
                nwavCarpeta.resolve("datos").toString());
        assumeTrue(nwavDependencias.nwavTesseract().nwavDisponible(), "Tesseract no está instalado: se omite el OCR");

        NwavOcrService nwavOcr = new NwavOcrService(nwavDependencias, nwavDocumentos);
        Path nwavEscaneado = NwavPdfConImagenes.nwavCrearEscaneado(nwavCarpeta, "scan.pdf", "HOLA MUNDO 2026");
        assertThat(NwavLectorPdf.nwavTextos(nwavEscaneado).get(0)).isEmpty();

        NwavOcrService.NwavResultadoOcr nwavResultado = nwavOcr.nwavAplicar(nwavEscaneado, null,
                new NwavOcrService.NwavOpcionesOcr(List.of("spa", "eng"), false, true, true, true, true, true),
                nwavCarpeta.resolve("scan_ocr.pdf"), nwavCarpeta.resolve("scan_ocr.txt"),
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);

        assertThat(nwavResultado.nwavPaginasReconocidas()).isEqualTo(1);
        assertThat(NwavLectorPdf.nwavTextos(nwavResultado.nwavPdf()).get(0)).contains("HOLA").contains("MUNDO");
        assertThat(Files.readString(nwavResultado.nwavTexto())).contains("HOLA MUNDO");
    }

    @Test
    void nwavAgruparPalabrasEnLineas() {
        List<NwavOcrService.NwavPalabra> nwavPalabras = List.of(
                new NwavOcrService.NwavPalabra("mundo", new java.awt.Rectangle(200, 100, 80, 30)),
                new NwavOcrService.NwavPalabra("Hola", new java.awt.Rectangle(100, 102, 80, 30)),
                new NwavOcrService.NwavPalabra("segunda", new java.awt.Rectangle(100, 200, 120, 30)));
        assertThat(NwavOcrService.nwavLineas(nwavPalabras)).isEqualTo("Hola mundo\nsegunda");
    }
}
