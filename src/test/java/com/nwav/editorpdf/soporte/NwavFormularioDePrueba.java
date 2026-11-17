package com.nwav.editorpdf.soporte;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceEntry;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDTerminalField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF de una pagina con formulario AcroForm: "nombre" (texto, etiqueta "Nombre completo"),
 * "acepto" (casilla), "pais" (lista) y "codigo" (texto de solo lectura = "X-1").
 */
public final class NwavFormularioDePrueba {

    private NwavFormularioDePrueba() {
    }

    public static Path nwavCrear(Path nwavCarpeta, String nwavNombre) throws IOException {
        Path nwavDestino = nwavCarpeta.resolve(nwavNombre);
        try (PDDocument nwavPd = new PDDocument()) {
            PDPage nwavPagina = new PDPage(PDRectangle.A4);
            nwavPd.addPage(nwavPagina);
            PDAcroForm nwavFormulario = new PDAcroForm(nwavPd);
            nwavPd.getDocumentCatalog().setAcroForm(nwavFormulario);
            PDResources nwavRecursos = new PDResources();
            nwavRecursos.put(COSName.getPDFName("Helv"), new PDType1Font(Standard14Fonts.FontName.HELVETICA));
            nwavFormulario.setDefaultResources(nwavRecursos);
            nwavFormulario.setDefaultAppearance("/Helv 0 Tf 0 g");
            List<PDAnnotation> nwavWidgets = new ArrayList<>();

            PDTextField nwavNombreCampo = new PDTextField(nwavFormulario);
            nwavNombreCampo.setPartialName("nombre");
            nwavNombreCampo.setAlternateFieldName("Nombre completo");
            nwavNombreCampo.setDefaultAppearance("/Helv 12 Tf 0 g");
            nwavUbicar(nwavNombreCampo, nwavPagina, new PDRectangle(100, 700, 300, 20), nwavWidgets);

            PDCheckBox nwavAcepto = new PDCheckBox(nwavFormulario);
            nwavAcepto.setPartialName("acepto");
            PDAnnotationWidget nwavWidgetCasilla = nwavUbicar(nwavAcepto, nwavPagina,
                    new PDRectangle(100, 650, 15, 15), nwavWidgets);
            nwavWidgetCasilla.setAppearance(nwavAparienciaCasilla(nwavPd));
            nwavWidgetCasilla.setAppearanceState("Off");
            nwavAcepto.getCOSObject().setItem(COSName.V, COSName.Off);

            PDComboBox nwavPais = new PDComboBox(nwavFormulario);
            nwavPais.setPartialName("pais");
            nwavPais.setDefaultAppearance("/Helv 12 Tf 0 g");
            nwavPais.setOptions(List.of("Perú", "Chile", "México"));
            nwavUbicar(nwavPais, nwavPagina, new PDRectangle(100, 600, 150, 20), nwavWidgets);

            PDTextField nwavCodigo = new PDTextField(nwavFormulario);
            nwavCodigo.setPartialName("codigo");
            nwavCodigo.setDefaultAppearance("/Helv 12 Tf 0 g");
            nwavUbicar(nwavCodigo, nwavPagina, new PDRectangle(100, 550, 100, 20), nwavWidgets);

            nwavFormulario.getFields().addAll(List.of(nwavNombreCampo, nwavAcepto, nwavPais, nwavCodigo));
            nwavPagina.setAnnotations(nwavWidgets);
            nwavCodigo.setValue("X-1");
            nwavCodigo.setReadOnly(true);
            nwavPd.save(nwavDestino.toFile());
        }
        return nwavDestino;
    }

    private static PDAnnotationWidget nwavUbicar(PDTerminalField nwavCampo, PDPage nwavPagina, PDRectangle nwavZona,
                                                 List<PDAnnotation> nwavWidgets) {
        PDAnnotationWidget nwavWidget = nwavCampo.getWidgets().get(0);
        nwavWidget.setRectangle(nwavZona);
        nwavWidget.setPage(nwavPagina);
        nwavWidget.setPrinted(true);
        nwavWidgets.add(nwavWidget);
        return nwavWidget;
    }

    /** Apariencias "Yes" (cuadro relleno) y "Off" (vacio) de la casilla. */
    private static PDAppearanceDictionary nwavAparienciaCasilla(PDDocument nwavPd) throws IOException {
        COSDictionary nwavNormal = new COSDictionary();
        for (String nwavEstado : List.of("Yes", "Off")) {
            PDAppearanceStream nwavFlujo = new PDAppearanceStream(nwavPd);
            nwavFlujo.setBBox(new PDRectangle(15, 15));
            nwavFlujo.setResources(new PDResources());
            try (PDPageContentStream nwavContenido = new PDPageContentStream(nwavPd, nwavFlujo)) {
                nwavContenido.addRect(0.5f, 0.5f, 14, 14);
                nwavContenido.stroke();
                if (nwavEstado.equals("Yes")) {
                    nwavContenido.addRect(3, 3, 9, 9);
                    nwavContenido.fill();
                }
            }
            nwavNormal.setItem(nwavEstado, nwavFlujo);
        }
        PDAppearanceDictionary nwavApariencia = new PDAppearanceDictionary();
        nwavApariencia.setNormalAppearance(new PDAppearanceEntry(nwavNormal));
        return nwavApariencia;
    }
}
