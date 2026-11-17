package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.model.NwavTipoCampo;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.comando.NwavComando;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceCharacteristicsDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceEntry;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDChoice;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDListBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NwavCreacionCamposService {
    private static final String NWAV_FUENTE_FORMULARIO = "Helv";

    public record NwavCampoNuevo(NwavTipoCampo nwavTipo, String nwavNombre, List<String> nwavOpciones, String nwavValor,
                                 boolean nwavMultilinea, boolean nwavObligatorio) {
        public NwavCampoNuevo {
            nwavOpciones = nwavOpciones == null ? List.of()
                    : nwavOpciones.stream().map(String::strip).filter(nwavO -> !nwavO.isEmpty()).distinct().toList();
        }
    }

    public NwavComando nwavComandoCrear(NwavDocumentoPdf nwavDocumento, int nwavIndicePagina, NwavRectangulo nwavZona,
                                        NwavCampoNuevo nwavCampo) {
        if (nwavZona.nwavAncho() < 8 || nwavZona.nwavAlto() < 8) {
            throw new NwavEditorException("La zona del campo es demasiado pequeña.");
        }
        if ((nwavCampo.nwavTipo() == NwavTipoCampo.NWAV_LISTA || nwavCampo.nwavTipo() == NwavTipoCampo.NWAV_OPCION)
                && nwavCampo.nwavOpciones().size() < 2) {
            throw new NwavEditorException("Escribe al menos dos opciones (una por línea).");
        }
        return new NwavComando() {
            private PDField nwavCreado;
            private List<PDAnnotationWidget> nwavWidgets;
            private PDPage nwavPagina;
            private boolean nwavFormularioNuevo;

            @Override
            public void nwavEjecutar() {
                synchronized (nwavDocumento) {
                    nwavDocumento.nwavVerificarAbierto();
                    PDDocument nwavPd = nwavDocumento.nwavPdDocument();
                    nwavPagina = nwavPd.getPage(nwavIndicePagina);
                    PDAcroForm nwavFormulario = nwavPd.getDocumentCatalog().getAcroForm();
                    if (nwavFormulario == null) {
                        nwavFormulario = new PDAcroForm(nwavPd);
                        nwavPd.getDocumentCatalog().setAcroForm(nwavFormulario);
                        nwavFormularioNuevo = true;
                    }
                    nwavPrepararFormulario(nwavFormulario);
                    if (nwavCreado == null) {
                        try {
                            nwavCreado = nwavConstruir(nwavPd, nwavFormulario, nwavPagina, nwavZona, nwavCampo);
                        } catch (IOException nwavError) {
                            throw new NwavEditorException("No se pudo crear el campo: " + nwavError.getMessage(), nwavError);
                        }
                        nwavWidgets = List.copyOf(nwavCreado.getWidgets());
                    }
                    try {
                        for (PDAnnotationWidget nwavWidget : nwavWidgets) {
                            nwavPagina.getAnnotations().add(nwavWidget);
                        }
                    } catch (IOException nwavError) {
                        throw new NwavEditorException("No se pudo añadir el campo a la página.", nwavError);
                    }
                    nwavFormulario.getFields().add(nwavCreado);
                    nwavDocumento.nwavMarcarModificada(nwavPagina.getCOSObject());
                }
            }

            @Override
            public void nwavDeshacer() {
                synchronized (nwavDocumento) {
                    PDDocument nwavPd = nwavDocumento.nwavPdDocument();
                    PDAcroForm nwavFormulario = nwavPd.getDocumentCatalog().getAcroForm();
                    try {
                        List<org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation> nwavQuedan =
                                new ArrayList<>(nwavPagina.getAnnotations());
                        nwavQuedan.removeIf(nwavA -> nwavWidgets.stream()
                                .anyMatch(nwavW -> nwavW.getCOSObject() == nwavA.getCOSObject()));
                        nwavPagina.setAnnotations(nwavQuedan);
                    } catch (IOException nwavError) {
                        throw new NwavEditorException("No se pudo quitar el campo.", nwavError);
                    }
                    if (nwavFormulario != null) {
                        List<PDField> nwavCampos = new ArrayList<>(nwavFormulario.getFields());
                        nwavCampos.removeIf(nwavF -> nwavF.getCOSObject() == nwavCreado.getCOSObject());
                        nwavFormulario.setFields(nwavCampos);
                        if (nwavFormularioNuevo && nwavCampos.isEmpty()) {
                            nwavPd.getDocumentCatalog().setAcroForm(null);
                        }
                    }
                    nwavDocumento.nwavMarcarModificada(nwavPagina.getCOSObject());
                }
            }

            @Override
            public NwavTipoOperacion nwavTipo() {
                return NwavTipoOperacion.NWAV_FORMULARIO;
            }

            @Override
            public String nwavDescripcion() {
                return "Crear campo «" + (nwavCreado != null ? nwavCreado.getPartialName() : nwavCampo.nwavNombre()) + "»";
            }

            @Override
            public int nwavPaginaDestacada() {
                return nwavIndicePagina;
            }

            @Override
            public boolean nwavCambiaEstructura() {
                return false;
            }
        };
    }

    static String nwavNombreLibre(PDAcroForm nwavFormulario, String nwavSugerido, String nwavBase) {
        String nwavLimpio = nwavSugerido == null ? "" : nwavSugerido.strip().replaceAll("[.\\s]+", "_");
        if (nwavLimpio.isEmpty()) {
            nwavLimpio = nwavBase;
        }
        Set<String> nwavUsados = new HashSet<>();
        nwavFormulario.getFieldTree().forEach(nwavF -> nwavUsados.add(nwavF.getFullyQualifiedName()));
        String nwavNombre = nwavLimpio;
        for (int nwavN = 2; nwavUsados.contains(nwavNombre); nwavN++) {
            nwavNombre = nwavLimpio + "_" + nwavN;
        }
        return nwavNombre;
    }

    private static void nwavPrepararFormulario(PDAcroForm nwavFormulario) {
        PDResources nwavRecursos = nwavFormulario.getDefaultResources();
        if (nwavRecursos == null) {
            nwavRecursos = new PDResources();
            nwavFormulario.setDefaultResources(nwavRecursos);
        }
        COSName nwavNombre = COSName.getPDFName(NWAV_FUENTE_FORMULARIO);
        try {
            if (nwavRecursos.getFont(nwavNombre) == null) {
                nwavRecursos.put(nwavNombre, new PDType1Font(Standard14Fonts.FontName.HELVETICA));
            }
        } catch (IOException nwavError) {
            nwavRecursos.put(nwavNombre, new PDType1Font(Standard14Fonts.FontName.HELVETICA));
        }
        if (nwavFormulario.getDefaultAppearance() == null || nwavFormulario.getDefaultAppearance().isBlank()) {
            nwavFormulario.setDefaultAppearance("/" + NWAV_FUENTE_FORMULARIO + " 0 Tf 0 g");
        }
        nwavFormulario.setNeedAppearances(false);
    }

    private static PDField nwavConstruir(PDDocument nwavPd, PDAcroForm nwavFormulario, PDPage nwavPagina,
                                         NwavRectangulo nwavZona, NwavCampoNuevo nwavCampo) throws IOException {
        NwavGeometriaPagina nwavGeometria = NwavGeometriaPagina.nwavDe(nwavPagina);
        PDRectangle nwavCaja = nwavGeometria.nwavRectanguloPdf(nwavZona);
        int nwavRotacion = NwavDocumentoPdf.nwavRotacionNormalizada(nwavPagina);
        float nwavTamano = (float) Math.max(7, Math.min(14, Math.min(nwavZona.nwavAlto(), 40) * 0.6));
        String nwavDa = "/" + NWAV_FUENTE_FORMULARIO + " " + (nwavCampo.nwavMultilinea() ? 10 : Math.round(nwavTamano))
                + " Tf 0 g";
        switch (nwavCampo.nwavTipo()) {
            case NWAV_CASILLA -> {
                PDCheckBox nwavCasilla = new PDCheckBox(nwavFormulario);
                nwavCasilla.setPartialName(nwavNombreLibre(nwavFormulario, nwavCampo.nwavNombre(), "casilla"));
                float nwavLado = (float) Math.min(nwavZona.nwavAncho(), nwavZona.nwavAlto());
                PDRectangle nwavCuadro = nwavGeometria.nwavRectanguloPdf(
                        new NwavRectangulo(nwavZona.nwavX(), nwavZona.nwavY(), nwavLado, nwavLado));
                PDAnnotationWidget nwavWidget = nwavCasilla.getWidgets().get(0);
                nwavWidgetBasico(nwavWidget, nwavPagina, nwavCuadro, nwavRotacion);
                nwavWidget.getAppearanceCharacteristics().setNormalCaption("4");
                nwavWidget.setAppearance(nwavAparienciaCasilla(nwavPd, nwavCuadro, "Si", false));
                nwavWidget.setAppearanceState("Off");
                nwavCasilla.setRequired(nwavCampo.nwavObligatorio());
                if ("true".equalsIgnoreCase(nwavCampo.nwavValor()) || "si".equalsIgnoreCase(nwavCampo.nwavValor())) {
                    nwavCasilla.check();
                }
                return nwavCasilla;
            }
            case NWAV_OPCION -> {
                PDRadioButton nwavGrupo = new PDRadioButton(nwavFormulario);
                nwavGrupo.setPartialName(nwavNombreLibre(nwavFormulario, nwavCampo.nwavNombre(), "opciones"));
                List<String> nwavOpciones = nwavCampo.nwavOpciones();
                double nwavAltoFila = nwavZona.nwavAlto() / nwavOpciones.size();
                List<PDAnnotationWidget> nwavWidgets = new ArrayList<>();
                for (int nwavIndice = 0; nwavIndice < nwavOpciones.size(); nwavIndice++) {
                    PDRectangle nwavFila = nwavGeometria.nwavRectanguloPdf(new NwavRectangulo(nwavZona.nwavX(),
                            nwavZona.nwavY() + nwavIndice * nwavAltoFila, nwavZona.nwavAncho(), nwavAltoFila));
                    PDAnnotationWidget nwavWidget = new PDAnnotationWidget();
                    nwavWidgetBasico(nwavWidget, nwavPagina, nwavFila, nwavRotacion);
                    nwavWidget.setParent(nwavGrupo);
                    String nwavExportar = nwavExportable(nwavOpciones.get(nwavIndice), nwavIndice);
                    nwavWidget.setAppearance(nwavAparienciaOpcion(nwavPd, nwavFila, nwavExportar,
                            nwavOpciones.get(nwavIndice), nwavRotacion));
                    nwavWidget.setAppearanceState("Off");
                    nwavWidgets.add(nwavWidget);
                }
                nwavGrupo.setWidgets(nwavWidgets);
                nwavGrupo.setRequired(nwavCampo.nwavObligatorio());
                nwavGrupo.setRadiosInUnison(false);
                if (nwavCampo.nwavValor() != null && nwavOpciones.contains(nwavCampo.nwavValor())) {
                    nwavGrupo.setValue(nwavExportable(nwavCampo.nwavValor(), nwavOpciones.indexOf(nwavCampo.nwavValor())));
                }
                return nwavGrupo;
            }
            case NWAV_LISTA -> {
                PDChoice nwavLista = nwavZona.nwavAlto() > 3 * nwavTamano ? new PDListBox(nwavFormulario)
                        : new PDComboBox(nwavFormulario);
                nwavLista.setPartialName(nwavNombreLibre(nwavFormulario, nwavCampo.nwavNombre(), "lista"));
                nwavLista.setOptions(nwavCampo.nwavOpciones());
                nwavLista.setDefaultAppearance(nwavDa);
                nwavWidgetBasico(nwavLista.getWidgets().get(0), nwavPagina, nwavCaja, nwavRotacion);
                nwavLista.setRequired(nwavCampo.nwavObligatorio());
                String nwavValor = nwavCampo.nwavValor() != null && nwavCampo.nwavOpciones().contains(nwavCampo.nwavValor())
                        ? nwavCampo.nwavValor() : nwavCampo.nwavOpciones().get(0);
                nwavLista.setValue(nwavValor);
                return nwavLista;
            }
            case NWAV_FIRMA_DIGITAL -> {
                PDSignatureField nwavFirma = new PDSignatureField(nwavFormulario);
                nwavFirma.setPartialName(nwavNombreLibre(nwavFormulario, nwavCampo.nwavNombre(), "firma"));
                PDAnnotationWidget nwavWidget = nwavFirma.getWidgets().get(0);
                nwavWidgetBasico(nwavWidget, nwavPagina, nwavCaja, nwavRotacion);
                nwavWidget.setAppearance(nwavAparienciaVacia(nwavPd, nwavCaja));
                nwavFirma.setRequired(nwavCampo.nwavObligatorio());
                return nwavFirma;
            }
            default -> {
                PDTextField nwavTexto = new PDTextField(nwavFormulario);
                nwavTexto.setPartialName(nwavNombreLibre(nwavFormulario, nwavCampo.nwavNombre(), "texto"));
                nwavTexto.setDefaultAppearance(nwavDa);
                nwavTexto.setMultiline(nwavCampo.nwavMultilinea());
                nwavTexto.setRequired(nwavCampo.nwavObligatorio());
                nwavWidgetBasico(nwavTexto.getWidgets().get(0), nwavPagina, nwavCaja, nwavRotacion);
                nwavTexto.setValue(nwavCampo.nwavValor() == null ? "" : nwavCampo.nwavValor());
                return nwavTexto;
            }
        }
    }

    private static String nwavExportable(String nwavOpcion, int nwavIndice) {
        String nwavLimpio = nwavOpcion.replaceAll("[^\\p{L}\\p{N}_-]+", "_");
        return nwavLimpio.isEmpty() || "Off".equals(nwavLimpio) ? "opcion" + (nwavIndice + 1) : nwavLimpio;
    }

    private static void nwavWidgetBasico(PDAnnotationWidget nwavWidget, PDPage nwavPagina, PDRectangle nwavCaja,
                                         int nwavRotacion) {
        nwavWidget.setRectangle(nwavCaja);
        nwavWidget.setPage(nwavPagina);
        nwavWidget.setPrinted(true);
        PDAppearanceCharacteristicsDictionary nwavMk = new PDAppearanceCharacteristicsDictionary(
                new org.apache.pdfbox.cos.COSDictionary());
        nwavMk.setBorderColour(new PDColor(new float[]{0.45f, 0.55f, 0.7f}, PDDeviceRGB.INSTANCE));
        nwavMk.setBackground(new PDColor(new float[]{0.93f, 0.96f, 1f}, PDDeviceRGB.INSTANCE));
        if (nwavRotacion != 0) {
            nwavMk.getCOSObject().setInt(COSName.R, nwavRotacion);
        }
        nwavWidget.setAppearanceCharacteristics(nwavMk);
        PDBorderStyleDictionary nwavBorde = new PDBorderStyleDictionary();
        nwavBorde.setWidth(1);
        nwavWidget.setBorderStyle(nwavBorde);
    }

    private static PDAppearanceDictionary nwavAparienciaCasilla(PDDocument nwavPd, PDRectangle nwavCaja, String nwavActivo,
                                                                boolean nwavIgnorado) throws IOException {
        float nwavLado = nwavCaja.getWidth();
        PDAppearanceStream nwavApagada = nwavFlujoVacio(nwavPd, nwavLado, nwavCaja.getHeight(), true);
        PDAppearanceStream nwavEncendida = nwavFlujoVacio(nwavPd, nwavLado, nwavCaja.getHeight(), false);
        try (PDPageContentStream nwavFlujo = new PDPageContentStream(nwavPd, nwavEncendida)) {
            nwavMarco(nwavFlujo, nwavLado, nwavCaja.getHeight());
            nwavFlujo.setStrokingColor(0.1f, 0.25f, 0.6f);
            nwavFlujo.setLineWidth(Math.max(1, nwavLado / 9));
            nwavFlujo.moveTo(nwavLado * 0.2f, nwavCaja.getHeight() * 0.52f);
            nwavFlujo.lineTo(nwavLado * 0.42f, nwavCaja.getHeight() * 0.28f);
            nwavFlujo.lineTo(nwavLado * 0.8f, nwavCaja.getHeight() * 0.76f);
            nwavFlujo.stroke();
        }
        return nwavDosEstados(nwavActivo, nwavEncendida, nwavApagada);
    }

    private static PDAppearanceDictionary nwavAparienciaOpcion(PDDocument nwavPd, PDRectangle nwavFila, String nwavActivo,
                                                               String nwavEtiqueta, int nwavRotacion) throws IOException {
        boolean nwavGirada = nwavRotacion == 90 || nwavRotacion == 270;
        float nwavAncho = nwavGirada ? nwavFila.getHeight() : nwavFila.getWidth();
        float nwavAlto = nwavGirada ? nwavFila.getWidth() : nwavFila.getHeight();
        PDAppearanceStream[] nwavEstados = new PDAppearanceStream[2];
        for (int nwavEstado = 0; nwavEstado < 2; nwavEstado++) {
            PDAppearanceStream nwavFlujoAp = new PDAppearanceStream(nwavPd);
            nwavFlujoAp.setBBox(new PDRectangle(nwavAncho, nwavAlto));
            nwavFlujoAp.setResources(new PDResources());
            try (PDPageContentStream nwavFlujo = new PDPageContentStream(nwavPd, nwavFlujoAp)) {
                float nwavRadio = Math.min(nwavAlto * 0.35f, 7);
                float nwavCx = nwavRadio + 2;
                float nwavCy = nwavAlto / 2;
                nwavFlujo.setStrokingColor(0.45f, 0.55f, 0.7f);
                nwavFlujo.setLineWidth(1);
                nwavCirculo(nwavFlujo, nwavCx, nwavCy, nwavRadio);
                nwavFlujo.stroke();
                if (nwavEstado == 0) {
                    nwavFlujo.setNonStrokingColor(0.1f, 0.25f, 0.6f);
                    nwavCirculo(nwavFlujo, nwavCx, nwavCy, nwavRadio * 0.5f);
                    nwavFlujo.fill();
                }
                PDType1Font nwavFuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                float nwavTamano = Math.max(6, Math.min(11, nwavAlto * 0.6f));
                String nwavTexto = nwavRecortarPara(nwavFuente, nwavTamano, nwavEtiqueta,
                        nwavAncho - nwavCx - nwavRadio - 6);
                if (!nwavTexto.isEmpty()) {
                    nwavFlujo.setNonStrokingColor(0f, 0f, 0f);
                    nwavFlujo.beginText();
                    nwavFlujo.setFont(nwavFuente, nwavTamano);
                    nwavFlujo.newLineAtOffset(nwavCx + nwavRadio + 4, nwavCy - nwavTamano * 0.35f);
                    nwavFlujo.showText(nwavTexto);
                    nwavFlujo.endText();
                }
            }
            nwavEstados[nwavEstado] = nwavFlujoAp;
        }
        return nwavDosEstados(nwavActivo, nwavEstados[0], nwavEstados[1]);
    }

    private static PDAppearanceDictionary nwavAparienciaVacia(PDDocument nwavPd, PDRectangle nwavCaja) throws IOException {
        PDAppearanceStream nwavFlujoAp = nwavFlujoVacio(nwavPd, nwavCaja.getWidth(), nwavCaja.getHeight(), true);
        PDAppearanceDictionary nwavApariencia = new PDAppearanceDictionary();
        nwavApariencia.setNormalAppearance(nwavFlujoAp);
        return nwavApariencia;
    }

    private static PDAppearanceStream nwavFlujoVacio(PDDocument nwavPd, float nwavAncho, float nwavAlto, boolean nwavConMarco)
            throws IOException {
        PDAppearanceStream nwavFlujoAp = new PDAppearanceStream(nwavPd);
        nwavFlujoAp.setBBox(new PDRectangle(nwavAncho, nwavAlto));
        nwavFlujoAp.setResources(new PDResources());
        if (nwavConMarco) {
            try (PDPageContentStream nwavFlujo = new PDPageContentStream(nwavPd, nwavFlujoAp)) {
                nwavMarco(nwavFlujo, nwavAncho, nwavAlto);
            }
        }
        return nwavFlujoAp;
    }

    private static void nwavMarco(PDPageContentStream nwavFlujo, float nwavAncho, float nwavAlto) throws IOException {
        nwavFlujo.setNonStrokingColor(0.93f, 0.96f, 1f);
        nwavFlujo.addRect(0, 0, nwavAncho, nwavAlto);
        nwavFlujo.fill();
        nwavFlujo.setStrokingColor(0.45f, 0.55f, 0.7f);
        nwavFlujo.setLineWidth(1);
        nwavFlujo.addRect(0.5f, 0.5f, nwavAncho - 1, nwavAlto - 1);
        nwavFlujo.stroke();
    }

    private static void nwavCirculo(PDPageContentStream nwavFlujo, float nwavCx, float nwavCy, float nwavR) throws IOException {
        float nwavK = 0.5523f * nwavR;
        nwavFlujo.moveTo(nwavCx + nwavR, nwavCy);
        nwavFlujo.curveTo(nwavCx + nwavR, nwavCy + nwavK, nwavCx + nwavK, nwavCy + nwavR, nwavCx, nwavCy + nwavR);
        nwavFlujo.curveTo(nwavCx - nwavK, nwavCy + nwavR, nwavCx - nwavR, nwavCy + nwavK, nwavCx - nwavR, nwavCy);
        nwavFlujo.curveTo(nwavCx - nwavR, nwavCy - nwavK, nwavCx - nwavK, nwavCy - nwavR, nwavCx, nwavCy - nwavR);
        nwavFlujo.curveTo(nwavCx + nwavK, nwavCy - nwavR, nwavCx + nwavR, nwavCy - nwavK, nwavCx + nwavR, nwavCy);
        nwavFlujo.closePath();
    }

    private static PDAppearanceDictionary nwavDosEstados(String nwavActivo, PDAppearanceStream nwavEncendida,
                                                         PDAppearanceStream nwavApagada) {
        org.apache.pdfbox.cos.COSDictionary nwavNormal = new org.apache.pdfbox.cos.COSDictionary();
        nwavNormal.setItem(COSName.getPDFName(nwavActivo), nwavEncendida);
        nwavNormal.setItem(COSName.Off, nwavApagada);
        PDAppearanceDictionary nwavApariencia = new PDAppearanceDictionary();
        nwavApariencia.getCOSObject().setItem(COSName.N, nwavNormal);
        return nwavApariencia;
    }

    private static String nwavRecortarPara(PDType1Font nwavFuente, float nwavTamano, String nwavTexto, float nwavMaximo) {
        StringBuilder nwavValido = new StringBuilder();
        for (char nwavCaracter : nwavTexto.toCharArray()) {
            try {
                nwavFuente.encode(String.valueOf(nwavCaracter));
                nwavValido.append(nwavCaracter);
            } catch (IOException | IllegalArgumentException nwavError) {
                nwavValido.append('?');
            }
        }
        String nwavResultado = nwavValido.toString();
        try {
            while (!nwavResultado.isEmpty() && nwavFuente.getStringWidth(nwavResultado) / 1000 * nwavTamano > nwavMaximo) {
                nwavResultado = nwavResultado.substring(0, nwavResultado.length() - 1);
            }
        } catch (IOException nwavError) {
            return "";
        }
        return nwavResultado;
    }

    static PDAppearanceEntry nwavNormal(PDAnnotationWidget nwavWidget) {
        return nwavWidget.getAppearance().getNormalAppearance();
    }
}
