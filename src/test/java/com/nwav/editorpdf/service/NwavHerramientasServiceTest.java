package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.model.NwavAlineacion;
import com.nwav.editorpdf.model.NwavCampoFormulario;
import com.nwav.editorpdf.model.NwavCoincidencia;
import com.nwav.editorpdf.model.NwavColorRgb;
import com.nwav.editorpdf.model.NwavFuente;
import com.nwav.editorpdf.model.NwavInfoDocumento;
import com.nwav.editorpdf.model.NwavMetadatos;
import com.nwav.editorpdf.model.NwavOpcionesEncabezado;
import com.nwav.editorpdf.model.NwavOpcionesMarcaAgua;
import com.nwav.editorpdf.model.NwavTipoCampo;
import com.nwav.editorpdf.model.NwavTipoMarcaAgua;
import com.nwav.editorpdf.service.comando.NwavGestorComandos;
import com.nwav.editorpdf.soporte.NwavFormularioDePrueba;
import com.nwav.editorpdf.soporte.NwavLectorPdf;
import com.nwav.editorpdf.soporte.NwavPdfDePrueba;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class NwavHerramientasServiceTest {
    private final NwavPdfDocumentoService nwavDocumentos = new NwavPdfDocumentoService();
    private final NwavContenidoService nwavContenido = new NwavContenidoService();
    private final NwavSelloService nwavSellos = new NwavSelloService(nwavContenido);
    private final NwavFormularioService nwavFormularios = new NwavFormularioService();
    private final NwavMetadatosService nwavMetadatos = new NwavMetadatosService();
    private final NwavBusquedaService nwavBusqueda = new NwavBusquedaService();
    private final NwavPdfRenderService nwavRender = new NwavPdfRenderService();

    @TempDir
    Path nwavCarpeta;

    private NwavDocumentoPdf nwavDocumento;

    @AfterEach
    void nwavCerrar() {
        if (nwavDocumento != null) {
            nwavDocumento.close();
        }
    }

    private NwavGestorComandos nwavGestor() {
        return nwavDocumento.getNwavGestor();
    }

    private void nwavGuardarEn(Path nwavDestino) throws Exception {
        synchronized (nwavDocumento) {
            nwavDocumento.nwavPdDocument().save(nwavDestino.toFile());
        }
    }

    @Test
    void nwavListaLosCamposConTipoEtiquetaYPosicion() throws Exception {
        nwavDocumento = nwavDocumentos.nwavAbrir(NwavFormularioDePrueba.nwavCrear(nwavCarpeta, "f.pdf"), null);

        var nwavCampos = nwavFormularios.nwavListar(nwavDocumento);

        assertThat(nwavCampos).extracting(NwavCampoFormulario::nwavNombre).containsExactly("nombre", "acepto", "pais", "codigo");
        assertThat(nwavCampos).extracting(NwavCampoFormulario::nwavTipo).containsExactly(
                NwavTipoCampo.NWAV_TEXTO, NwavTipoCampo.NWAV_CASILLA, NwavTipoCampo.NWAV_LISTA, NwavTipoCampo.NWAV_TEXTO);
        NwavCampoFormulario nwavNombre = nwavCampos.get(0);
        assertThat(nwavNombre.nwavEtiqueta()).isEqualTo("Nombre completo");
        assertThat(nwavNombre.nwavPagina()).isZero();
        assertThat(nwavNombre.nwavZona().nwavX()).isCloseTo(100, within(0.5));
        assertThat(nwavNombre.nwavZona().nwavY()).isCloseTo(841.89 - 720, within(0.5));
        assertThat(nwavCampos.get(1).nwavValor()).isEqualTo(NwavCampoFormulario.NWAV_DESMARCADA);
        assertThat(nwavCampos.get(2).nwavOpciones()).containsExactly("Perú", "Chile", "México");
        assertThat(nwavCampos.get(3).nwavSoloLectura()).isTrue();
        assertThat(nwavCampos.get(3).nwavValor()).isEqualTo("X-1");
    }

    @Test
    void nwavRellenarDeshacerYGuardar() throws Exception {
        nwavDocumento = nwavDocumentos.nwavAbrir(NwavFormularioDePrueba.nwavCrear(nwavCarpeta, "f.pdf"), null);
        nwavGestor().nwavEjecutar(nwavFormularios.nwavComandoRellenar(nwavDocumento,
                Map.of("nombre", "José Pérez", "acepto", "true", "pais", "Chile")));

        var nwavCampos = nwavFormularios.nwavListar(nwavDocumento);
        assertThat(nwavCampos.get(0).nwavValor()).isEqualTo("José Pérez");
        assertThat(nwavCampos.get(1).nwavValor()).isEqualTo(NwavCampoFormulario.NWAV_MARCADA);
        assertThat(nwavCampos.get(2).nwavValor()).isEqualTo("Chile");

        nwavGestor().nwavDeshacer();
        nwavCampos = nwavFormularios.nwavListar(nwavDocumento);
        assertThat(nwavCampos.get(0).nwavValor()).isEmpty();
        assertThat(nwavCampos.get(1).nwavValor()).isEqualTo(NwavCampoFormulario.NWAV_DESMARCADA);

        nwavGestor().nwavRehacer();
        Path nwavGuardado = nwavCarpeta.resolve("relleno.pdf");
        nwavGuardarEn(nwavGuardado);
        try (PDDocument nwavReabierto = Loader.loadPDF(nwavGuardado.toFile())) {
            PDAcroForm nwavFormulario = nwavReabierto.getDocumentCatalog().getAcroForm();
            assertThat(nwavFormulario.getField("nombre").getValueAsString()).isEqualTo("José Pérez");
            assertThat(nwavFormulario.getField("pais").getValueAsString()).contains("Chile");
            BufferedImage nwavImagen = new org.apache.pdfbox.rendering.PDFRenderer(nwavReabierto).renderImage(0, 1f);
            int nwavOscuros = 0;
            for (int nwavX = 102; nwavX < 250; nwavX++) {
                for (int nwavY = 124; nwavY < 140; nwavY++) {
                    Color nwavPixel = new Color(nwavImagen.getRGB(nwavX, nwavY));
                    nwavOscuros += nwavPixel.getRed() + nwavPixel.getGreen() + nwavPixel.getBlue() < 300 ? 1 : 0;
                }
            }
            assertThat(nwavOscuros).as("pixeles del texto del campo").isGreaterThan(40);
        }
    }

    @Test
    void nwavCamposDeSoloLecturaOInexistentesSeRechazan() throws Exception {
        nwavDocumento = nwavDocumentos.nwavAbrir(NwavFormularioDePrueba.nwavCrear(nwavCarpeta, "f.pdf"), null);

        assertThatThrownBy(() -> nwavGestor().nwavEjecutar(nwavFormularios.nwavComandoRellenar(nwavDocumento,
                Map.of("codigo", "Y")))).hasMessageContaining("solo lectura");
        assertThatThrownBy(() -> nwavGestor().nwavEjecutar(nwavFormularios.nwavComandoRellenar(nwavDocumento,
                Map.of("noexiste", "Y")))).hasMessageContaining("no existe");
        assertThat(nwavGestor().nwavPuedeDeshacer()).isFalse();
    }

    @Test
    void nwavUnPdfSinFormularioNoTieneCampos() throws Exception {
        nwavDocumento = nwavDocumentos.nwavAbrir(NwavPdfDePrueba.nwavCrear(nwavCarpeta, "sin.pdf", 1), null);
        assertThat(nwavFormularios.nwavListar(nwavDocumento)).isEmpty();
    }

    @Test
    void nwavMarcaDeAguaDeTextoEnLasPaginasElegidas() throws Exception {
        nwavDocumento = nwavDocumentos.nwavAbrir(NwavPdfDePrueba.nwavCrear(nwavCarpeta, "m.pdf", 3), null);
        nwavGestor().nwavEjecutar(nwavSellos.nwavComandoMarcaAgua(nwavDocumento, new NwavOpcionesMarcaAgua(
                NwavTipoMarcaAgua.NWAV_TEXTO, "CONFIDENCIAL", null, NwavFuente.NWAV_HELVETICA, 60,
                NwavColorRgb.NWAV_ROJO, 0.3, 45, 1, "1, 3")));

        var nwavTextos = NwavLectorPdf.nwavTextos(nwavDocumento.nwavPdDocument());
        assertThat(nwavTextos.get(0)).contains("CONFIDENCIAL");
        assertThat(nwavTextos.get(1)).doesNotContain("CONFIDENCIAL");
        assertThat(nwavTextos.get(2)).contains("CONFIDENCIAL");

        nwavGestor().nwavDeshacer();
        assertThat(NwavLectorPdf.nwavTextos(nwavDocumento.nwavPdDocument())).noneMatch(nwavTexto -> nwavTexto.contains("CONFIDENCIAL"));
    }

    @Test
    void nwavMarcaDeAguaDeImagenSemitransparenteEnElCentro() throws Exception {
        nwavDocumento = nwavDocumentos.nwavAbrir(NwavPdfDePrueba.nwavCrear(nwavCarpeta, "i.pdf", 1), null);
        byte[] nwavPng = Files.readAllBytes(NwavLectorPdf.nwavCrearPng(nwavCarpeta, "logo.png", 100, 100));
        nwavGestor().nwavEjecutar(nwavSellos.nwavComandoMarcaAgua(nwavDocumento, new NwavOpcionesMarcaAgua(
                NwavTipoMarcaAgua.NWAV_IMAGEN, null, nwavPng, null, 0, null, 0.5, 0, 0.4, "")));

        BufferedImage nwavImagen = nwavRender.nwavRenderizarPagina(nwavDocumento, 0, 1f);
        Color nwavCentro = new Color(nwavImagen.getRGB(nwavImagen.getWidth() / 2, nwavImagen.getHeight() / 2));
        assertThat(nwavCentro.getBlue()).isGreaterThan(200);
        assertThat(nwavCentro.getRed()).isBetween(120, 170);
        assertThat(new Color(nwavImagen.getRGB(30, nwavImagen.getHeight() / 2))).isEqualTo(Color.WHITE);
    }

    @Test
    void nwavLaMarcaDeAguaSinTextoOImagenSeRechaza() throws Exception {
        nwavDocumento = nwavDocumentos.nwavAbrir(NwavPdfDePrueba.nwavCrear(nwavCarpeta, "x.pdf", 1), null);
        assertThatThrownBy(() -> nwavSellos.nwavComandoMarcaAgua(nwavDocumento, new NwavOpcionesMarcaAgua(
                NwavTipoMarcaAgua.NWAV_TEXTO, "  ", null, null, 40, null, 0.3, 45, 1, "")))
                .isInstanceOf(NwavEditorException.class);
        assertThatThrownBy(() -> nwavSellos.nwavComandoMarcaAgua(nwavDocumento, new NwavOpcionesMarcaAgua(
                NwavTipoMarcaAgua.NWAV_IMAGEN, null, null, null, 40, null, 0.3, 45, 1, "")))
                .hasMessageContaining("imagen");
    }

    @Test
    void nwavNumeraLasPaginasYPoneEncabezado() throws Exception {
        nwavDocumento = nwavDocumentos.nwavAbrir(NwavPdfDePrueba.nwavCrear(nwavCarpeta, "n.pdf", 3), null);
        nwavGestor().nwavEjecutar(nwavSellos.nwavComandoEncabezado(nwavDocumento, new NwavOpcionesEncabezado(
                "Informe {archivo}", NwavAlineacion.NWAV_IZQUIERDA, "Página {n} de {total}", NwavAlineacion.NWAV_CENTRO,
                NwavFuente.NWAV_HELVETICA, 10, NwavColorRgb.NWAV_NEGRO, 30, 1, "")));

        var nwavTextos = NwavLectorPdf.nwavTextos(nwavDocumento.nwavPdDocument());
        assertThat(nwavTextos.get(1)).contains("Página2de3").contains("Informen.pdf");
        assertThat(nwavTextos.get(2)).contains("Página3de3");
    }

    @Test
    void nwavElNumeroInicialDesplazaLaNumeracion() throws Exception {
        nwavDocumento = nwavDocumentos.nwavAbrir(NwavPdfDePrueba.nwavCrear(nwavCarpeta, "n.pdf", 3), null);
        nwavGestor().nwavEjecutar(nwavSellos.nwavComandoEncabezado(nwavDocumento, new NwavOpcionesEncabezado(
                "", null, "{n}/{total}", NwavAlineacion.NWAV_DERECHA, null, 9, null, 20, 5, "2-")));

        var nwavTextos = NwavLectorPdf.nwavTextos(nwavDocumento.nwavPdDocument());
        assertThat(nwavTextos.get(0)).isEqualTo("Pagina1");
        assertThat(nwavTextos.get(1)).contains("6/7");
        assertThat(nwavTextos.get(2)).contains("7/7");
    }

    @Test
    void nwavSustituyeLasMarcas() {
        assertThat(NwavSelloService.nwavSustituir("{n} de {total} · {archivo} · {fecha}", 2, 9, "a.pdf",
                LocalDate.of(2026, 11, 17))).isEqualTo("2 de 9 · a.pdf · 17/11/2026");
    }

    @Test
    void nwavEditaYDeshaceLosMetadatos() throws Exception {
        nwavDocumento = nwavDocumentos.nwavAbrir(NwavPdfDePrueba.nwavCrear(nwavCarpeta, "meta.pdf", 2), null);
        NwavInfoDocumento nwavAntes = nwavMetadatos.nwavLeer(nwavDocumento);
        assertThat(nwavAntes.nwavPaginas()).isEqualTo(2);
        assertThat(nwavAntes.nwavMetadatos().nwavTitulo()).isNull();

        nwavGestor().nwavEjecutar(nwavMetadatos.nwavComandoEditar(nwavDocumento,
                new NwavMetadatos("Informe anual", "Noe", "Ventas", "pdf, nwav", null, null, null, null)));
        NwavMetadatos nwavDespues = nwavMetadatos.nwavLeer(nwavDocumento).nwavMetadatos();
        assertThat(nwavDespues.nwavTitulo()).isEqualTo("Informe anual");
        assertThat(nwavDespues.nwavAutor()).isEqualTo("Noe");
        assertThat(nwavDespues.nwavModificacion()).isNotNull();

        Path nwavGuardado = nwavCarpeta.resolve("meta2.pdf");
        nwavGuardarEn(nwavGuardado);
        try (PDDocument nwavReabierto = Loader.loadPDF(nwavGuardado.toFile())) {
            assertThat(nwavReabierto.getDocumentInformation().getTitle()).isEqualTo("Informe anual");
            assertThat(nwavReabierto.getDocumentInformation().getKeywords()).isEqualTo("pdf, nwav");
        }

        nwavGestor().nwavDeshacer();
        assertThat(nwavMetadatos.nwavLeer(nwavDocumento).nwavMetadatos().nwavTitulo()).isNull();
    }

    @Test
    void nwavBuscaSinDistinguirMayusculasNiTildes() throws Exception {
        nwavDocumento = nwavDocumentos.nwavAbrir(NwavPdfDePrueba.nwavCrear(nwavCarpeta, "b.pdf", 5), null);

        var nwavTodas = nwavBusqueda.nwavBuscar(nwavDocumento, "PÁGINA", false, NwavAvance.NWAV_NINGUNO, () -> false);
        assertThat(nwavTodas).hasSize(5).extracting(NwavCoincidencia::nwavPagina).containsExactly(0, 1, 2, 3, 4);

        var nwavUna = nwavBusqueda.nwavBuscar(nwavDocumento, "pagina   3", false, NwavAvance.NWAV_NINGUNO, () -> false);
        assertThat(nwavUna).singleElement().satisfies(nwavCoincidencia -> {
            assertThat(nwavCoincidencia.nwavPagina()).isEqualTo(2);
            assertThat(nwavCoincidencia.nwavZonas()).singleElement()
                    .satisfies(nwavZona -> assertThat(nwavZona.nwavX()).isCloseTo(72, within(3.0)));
            assertThat(nwavCoincidencia.nwavContexto()).contains("Pagina 3");
        });
    }

    @Test
    void nwavDistinguirMayusculasYCancelar() throws Exception {
        nwavDocumento = nwavDocumentos.nwavAbrir(NwavPdfDePrueba.nwavCrear(nwavCarpeta, "b.pdf", 5), null);

        assertThat(nwavBusqueda.nwavBuscar(nwavDocumento, "pagina", true, NwavAvance.NWAV_NINGUNO, () -> false)).isEmpty();
        assertThat(nwavBusqueda.nwavBuscar(nwavDocumento, "Pagina", true, NwavAvance.NWAV_NINGUNO, () -> false)).hasSize(5);
        assertThat(nwavBusqueda.nwavBuscar(nwavDocumento, "Pagina", false, NwavAvance.NWAV_NINGUNO, () -> true)).isEmpty();
        assertThatThrownBy(() -> nwavBusqueda.nwavBuscar(nwavDocumento, " ", false, NwavAvance.NWAV_NINGUNO, () -> false))
                .isInstanceOf(NwavEditorException.class);
    }

    @Test
    void nwavNormalizaConservandoLaEnie() {
        assertThat(NwavBusquedaService.nwavNormalizar("Ñandú ÁÉÍÓÚ", false)).isEqualTo("ñandu aeiou");
    }
}
