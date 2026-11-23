package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavAlineacion;
import com.nwav.editorpdf.model.NwavColorRgb;
import com.nwav.editorpdf.model.NwavFuente;
import com.nwav.editorpdf.model.NwavOpcionesEncabezado;
import com.nwav.editorpdf.model.NwavOpcionesMarcaAgua;
import com.nwav.editorpdf.model.NwavPosicionMarca;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.model.NwavTipoCampo;
import com.nwav.editorpdf.model.NwavTipoMarcaAgua;
import com.nwav.editorpdf.service.NwavCreacionCamposService.NwavCampoNuevo;
import com.nwav.editorpdf.soporte.NwavLectorPdf;
import com.nwav.editorpdf.soporte.NwavPdfDePrueba;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class NwavEditarAvanzadoTest {
    @TempDir
    Path nwavCarpeta;

    private final NwavPdfDocumentoService nwavDocumentos = new NwavPdfDocumentoService();
    private final NwavSelloService nwavSellos = new NwavSelloService(new NwavContenidoService());
    private final NwavEdicionLoteService nwavLote = new NwavEdicionLoteService(nwavDocumentos, nwavSellos);
    private final NwavCreacionCamposService nwavCampos = new NwavCreacionCamposService();

    private static NwavOpcionesMarcaAgua nwavMarca(String nwavTexto, NwavPosicionMarca nwavPosicion, double nwavGiro) {
        return new NwavOpcionesMarcaAgua(NwavTipoMarcaAgua.NWAV_TEXTO, nwavTexto, null, NwavFuente.NWAV_HELVETICA, 24,
                new NwavColorRgb(200, 0, 0), 0.4, nwavGiro, 0.4, "", nwavPosicion);
    }

    @Test
    void nwavPosicionesDeLaMarcaQuedanDentroDeLaPagina() {
        for (NwavPosicionMarca nwavPosicion : NwavPosicionMarca.values()) {
            List<float[]> nwavCentros = NwavSelloService.nwavCentros(nwavPosicion, 595, 842, 120, 24, 0);
            if (nwavPosicion == NwavPosicionMarca.NWAV_MOSAICO) {
                assertThat(nwavCentros.size()).as("mosaico").isGreaterThan(10);
                continue;
            }
            assertThat(nwavCentros).hasSize(1);
            float[] nwavC = nwavCentros.get(0);
            assertThat(nwavC[0] - 60).as(nwavPosicion + " x").isGreaterThanOrEqualTo(0);
            assertThat(nwavC[0] + 60).isLessThanOrEqualTo(595);
            assertThat(nwavC[1] - 12).as(nwavPosicion + " y").isGreaterThanOrEqualTo(0);
            assertThat(nwavC[1] + 12).isLessThanOrEqualTo(842);
        }
        float[] nwavArriba = NwavSelloService.nwavCentros(NwavPosicionMarca.NWAV_ARRIBA_IZQUIERDA, 595, 842, 120, 24, 0).get(0);
        assertThat(nwavArriba[0]).isCloseTo(28 + 60, within(0.1f));
        assertThat(nwavArriba[1]).isCloseTo(842 - 28 - 12, within(0.1f));
    }

    @Test
    void nwavMarcaEnMosaicoYNumerosPorLoteSinTocarElOriginal() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "doc.pdf", 2);
        byte[] nwavAntes = java.nio.file.Files.readAllBytes(nwavOriginal);

        Path nwavConMarca = nwavLote.nwavMarcaAgua(nwavOriginal, null, nwavMarca("BORRADOR", NwavPosicionMarca.NWAV_MOSAICO, 30),
                nwavCarpeta.resolve("marca.pdf"));
        try (PDDocument nwavPd = Loader.loadPDF(nwavConMarca.toFile())) {
            String nwavTexto = new PDFTextStripper().getText(nwavPd);
            int nwavVeces = nwavTexto.split("BORRADOR", -1).length - 1;
            assertThat(nwavVeces).isGreaterThan(2 * 10);
        }

        Path nwavNumerado = nwavLote.nwavEncabezado(nwavOriginal, null, new NwavOpcionesEncabezado("", null,
                        "Página {n} de {total}", NwavAlineacion.NWAV_DERECHA, NwavFuente.NWAV_HELVETICA, 10, null, 30, 1, ""),
                nwavCarpeta.resolve("num.pdf"));
        assertThat(NwavLectorPdf.nwavTextos(nwavNumerado).get(1)).contains("Página2de2");
        assertThat(java.nio.file.Files.readAllBytes(nwavOriginal)).isEqualTo(nwavAntes);
    }

    @Test
    void nwavCrearCamposDeTodosLosTiposYDeshacer() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrearConRotacion(nwavCarpeta, "form.pdf", 2, 90);
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavOriginal, null)) {
            var nwavGestor = nwavDocumento.getNwavGestor();
            nwavGestor.nwavEjecutar(nwavCampos.nwavComandoCrear(nwavDocumento, 0, new NwavRectangulo(72, 100, 200, 22),
                    new NwavCampoNuevo(NwavTipoCampo.NWAV_TEXTO, "nombre", null, "Ana", false, true)));
            nwavGestor.nwavEjecutar(nwavCampos.nwavComandoCrear(nwavDocumento, 0, new NwavRectangulo(72, 140, 14, 14),
                    new NwavCampoNuevo(NwavTipoCampo.NWAV_CASILLA, "acepto", null, null, false, false)));
            nwavGestor.nwavEjecutar(nwavCampos.nwavComandoCrear(nwavDocumento, 0, new NwavRectangulo(72, 170, 150, 20),
                    new NwavCampoNuevo(NwavTipoCampo.NWAV_LISTA, "pais", List.of("España", "México", "Chile"), "México",
                            false, false)));
            nwavGestor.nwavEjecutar(nwavCampos.nwavComandoCrear(nwavDocumento, 0, new NwavRectangulo(72, 200, 150, 60),
                    new NwavCampoNuevo(NwavTipoCampo.NWAV_OPCION, "talla", List.of("S", "M", "L"), "M", false, false)));
            nwavGestor.nwavEjecutar(nwavCampos.nwavComandoCrear(nwavDocumento, 1, new NwavRectangulo(72, 300, 200, 60),
                    new NwavCampoNuevo(NwavTipoCampo.NWAV_FIRMA_DIGITAL, "firma", null, null, false, false)));
            nwavGestor.nwavEjecutar(nwavCampos.nwavComandoCrear(nwavDocumento, 0, new NwavRectangulo(72, 300, 200, 22),
                    new NwavCampoNuevo(NwavTipoCampo.NWAV_TEXTO, "nombre", null, "", true, false)));

            assertThatThrownBy(() -> nwavCampos.nwavComandoCrear(nwavDocumento, 0, new NwavRectangulo(0, 0, 100, 20),
                    new NwavCampoNuevo(NwavTipoCampo.NWAV_LISTA, "x", List.of("solo una"), null, false, false)))
                    .hasMessageContaining("dos opciones");

            Path nwavGuardado = nwavCarpeta.resolve("con_campos.pdf");
            synchronized (nwavDocumento) {
                NwavEscrituraSegura.nwavGuardarPdf(nwavDocumento.nwavPdDocument(), nwavGuardado);
            }
            try (PDDocument nwavPd = Loader.loadPDF(nwavGuardado.toFile())) {
                PDAcroForm nwavForm = nwavPd.getDocumentCatalog().getAcroForm();
                assertThat(nwavForm.getField("nombre")).isInstanceOf(PDTextField.class);
                assertThat(nwavForm.getField("nombre").getValueAsString()).isEqualTo("Ana");
                assertThat(nwavForm.getField("nombre").isRequired()).isTrue();
                assertThat(nwavForm.getField("nombre_2")).isInstanceOf(PDTextField.class);
                assertThat(((PDTextField) nwavForm.getField("nombre_2")).isMultiline()).isTrue();
                PDCheckBox nwavCasilla = (PDCheckBox) nwavForm.getField("acepto");
                assertThat(nwavCasilla.isChecked()).isFalse();
                nwavCasilla.check();
                assertThat(nwavCasilla.isChecked()).isTrue();
                assertThat(nwavForm.getField("pais")).isInstanceOf(PDComboBox.class);
                assertThat(((PDComboBox) nwavForm.getField("pais")).getValue()).containsExactly("México");
                PDRadioButton nwavTalla = (PDRadioButton) nwavForm.getField("talla");
                assertThat(nwavTalla.getWidgets()).hasSize(3);
                assertThat(nwavTalla.getValue()).isEqualTo("M");
                assertThat(nwavForm.getField("firma")).isInstanceOf(PDSignatureField.class);
                assertThat(nwavPd.getPage(1).getAnnotations()).hasSize(1);
                assertThat(nwavPd.getPage(0).getAnnotations()).hasSize(7);
            }

            for (int nwavPaso = 0; nwavPaso < 6; nwavPaso++) {
                nwavGestor.nwavDeshacer();
            }
            synchronized (nwavDocumento) {
                assertThat(nwavDocumento.nwavPdDocument().getDocumentCatalog().getAcroForm()).isNull();
                assertThat(nwavDocumento.nwavPdDocument().getPage(0).getAnnotations()).isEmpty();
            }
        }
    }
}
