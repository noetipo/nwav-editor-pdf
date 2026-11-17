package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.model.NwavCampoFormulario;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.model.NwavTipoCampo;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.comando.NwavComando;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDChoice;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDPushButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTerminalField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NwavFormularioService {
    public List<NwavCampoFormulario> nwavListar(NwavDocumentoPdf nwavDocumento) {
        synchronized (nwavDocumento) {
            nwavDocumento.nwavVerificarAbierto();
            PDDocument nwavPd = nwavDocumento.nwavPdDocument();
            PDAcroForm nwavFormulario = nwavPd.getDocumentCatalog().getAcroForm();
            if (nwavFormulario == null) {
                return List.of();
            }
            List<NwavCampoFormulario> nwavCampos = new ArrayList<>();
            for (PDField nwavCampo : nwavFormulario.getFieldTree()) {
                if (!(nwavCampo instanceof PDTerminalField nwavTerminal) || nwavCampo instanceof PDPushButton) {
                    continue;
                }
                NwavTipoCampo nwavTipo = nwavTipo(nwavCampo);
                if (nwavTipo == null) {
                    continue;
                }
                List<String> nwavOpciones = List.of();
                if (nwavCampo instanceof PDRadioButton nwavRadio) {
                    nwavOpciones = new ArrayList<>(nwavRadio.getExportValues());
                } else if (nwavCampo instanceof PDChoice nwavLista) {
                    nwavOpciones = new ArrayList<>(nwavLista.getOptionsExportValues());
                }
                int nwavPagina = -1;
                NwavRectangulo nwavZona = null;
                if (!nwavTerminal.getWidgets().isEmpty()) {
                    PDAnnotationWidget nwavWidget = nwavTerminal.getWidgets().get(0);
                    nwavPagina = nwavPaginaDe(nwavPd, nwavWidget);
                    if (nwavPagina >= 0 && nwavWidget.getRectangle() != null) {
                        nwavZona = NwavGeometriaPagina.nwavDe(nwavPd.getPage(nwavPagina))
                                .nwavRectanguloVisual(nwavWidget.getRectangle());
                    }
                }
                String nwavEtiqueta = nwavCampo.getAlternateFieldName() != null && !nwavCampo.getAlternateFieldName().isBlank()
                        ? nwavCampo.getAlternateFieldName() : nwavCampo.getPartialName();
                nwavCampos.add(new NwavCampoFormulario(nwavCampo.getFullyQualifiedName(), nwavEtiqueta, nwavTipo,
                        nwavValor(nwavCampo), nwavOpciones,
                        nwavCampo instanceof PDTextField nwavTexto && nwavTexto.isMultiline(),
                        nwavCampo.isReadOnly() || nwavTipo == NwavTipoCampo.NWAV_FIRMA_DIGITAL,
                        nwavCampo.isRequired(), nwavPagina, nwavZona));
            }
            return nwavCampos;
        }
    }

    public NwavComando nwavComandoRellenar(NwavDocumentoPdf nwavDocumento, Map<String, String> nwavValores) {
        if (nwavValores == null || nwavValores.isEmpty()) {
            throw new NwavEditorException("No hay cambios en el formulario.");
        }
        Map<String, String> nwavNuevos = new LinkedHashMap<>(nwavValores);
        return new NwavComando() {
            private Map<String, String> nwavAnteriores;

            @Override
            public void nwavEjecutar() {
                synchronized (nwavDocumento) {
                    PDAcroForm nwavFormulario = nwavFormulario(nwavDocumento);
                    if (nwavAnteriores == null) {
                        nwavAnteriores = new LinkedHashMap<>();
                        for (String nwavNombre : nwavNuevos.keySet()) {
                            PDField nwavCampo = nwavCampo(nwavFormulario, nwavNombre);
                            if (nwavCampo.isReadOnly()) {
                                throw new NwavEditorException("El campo «" + nwavNombre + "» es de solo lectura.");
                            }
                            nwavAnteriores.put(nwavNombre, nwavValor(nwavCampo));
                        }
                    }
                    nwavAsignarTodos(nwavDocumento, nwavFormulario, nwavNuevos, nwavAnteriores);
                }
            }

            @Override
            public void nwavDeshacer() {
                synchronized (nwavDocumento) {
                    nwavAsignarTodos(nwavDocumento, nwavFormulario(nwavDocumento), nwavAnteriores, null);
                }
            }

            @Override
            public NwavTipoOperacion nwavTipo() {
                return NwavTipoOperacion.NWAV_FORMULARIO;
            }

            @Override
            public String nwavDescripcion() {
                return nwavNuevos.size() == 1 ? "Rellenar 1 campo" : "Rellenar " + nwavNuevos.size() + " campos";
            }

            @Override
            public boolean nwavCambiaEstructura() {
                return false;
            }
        };
    }

    private static PDAcroForm nwavFormulario(NwavDocumentoPdf nwavDocumento) {
        nwavDocumento.nwavVerificarAbierto();
        PDAcroForm nwavFormulario = nwavDocumento.nwavPdDocument().getDocumentCatalog().getAcroForm();
        if (nwavFormulario == null) {
            throw new NwavEditorException("El documento no tiene formulario.");
        }
        return nwavFormulario;
    }

    private static PDField nwavCampo(PDAcroForm nwavFormulario, String nwavNombre) {
        PDField nwavCampo = nwavFormulario.getField(nwavNombre);
        if (nwavCampo == null) {
            throw new NwavEditorException("El campo «" + nwavNombre + "» no existe en el formulario.");
        }
        return nwavCampo;
    }

    private static void nwavAsignarTodos(NwavDocumentoPdf nwavDocumento, PDAcroForm nwavFormulario,
                                         Map<String, String> nwavValores, Map<String, String> nwavRespaldo) {
        List<String> nwavHechos = new ArrayList<>();
        try {
            for (Map.Entry<String, String> nwavEntrada : nwavValores.entrySet()) {
                nwavAsignar(nwavFormulario, nwavCampo(nwavFormulario, nwavEntrada.getKey()), nwavEntrada.getValue());
                nwavHechos.add(nwavEntrada.getKey());
            }
        } catch (RuntimeException nwavError) {
            if (nwavRespaldo != null) {
                for (String nwavNombre : nwavHechos) {
                    try {
                        nwavAsignar(nwavFormulario, nwavCampo(nwavFormulario, nwavNombre), nwavRespaldo.get(nwavNombre));
                    } catch (RuntimeException nwavIgnorado) {
                        nwavLog.debug("No se pudo restaurar {}", nwavNombre, nwavIgnorado);
                    }
                }
            }
            throw nwavError;
        } finally {
            nwavMarcarPaginasDeWidgets(nwavDocumento, nwavFormulario, nwavValores.keySet());
        }
    }

    static void nwavAsignar(PDAcroForm nwavFormulario, PDField nwavCampo, String nwavValor) {
        String nwavTexto = nwavValor == null ? "" : nwavValor;
        try {
            if (nwavCampo instanceof PDCheckBox nwavCasilla) {
                if (Boolean.parseBoolean(nwavTexto)) {
                    nwavCasilla.check();
                } else {
                    nwavCasilla.unCheck();
                }
            } else if (nwavCampo instanceof PDRadioButton nwavRadio) {
                if (nwavTexto.isEmpty()) {
                    nwavRadio.getCOSObject().setItem(COSName.V, COSName.Off);
                    for (PDAnnotationWidget nwavWidget : nwavRadio.getWidgets()) {
                        nwavWidget.setAppearanceState(COSName.Off.getName());
                    }
                } else {
                    nwavRadio.setValue(nwavTexto);
                }
            } else if (nwavCampo instanceof PDChoice nwavLista) {
                nwavLista.setValue(nwavTexto);
            } else if (nwavCampo instanceof PDTextField nwavCampoTexto) {
                nwavAsignarTexto(nwavFormulario, nwavCampoTexto, nwavTexto);
            } else {
                throw new NwavEditorException("El campo «" + nwavCampo.getFullyQualifiedName()
                        + "» no se puede rellenar desde el editor.");
            }
        } catch (IOException | IllegalArgumentException nwavError) {
            throw new NwavEditorException("No se pudo asignar «" + nwavTexto + "» al campo «"
                    + nwavCampo.getFullyQualifiedName() + "»: " + nwavError.getMessage(), nwavError);
        }
    }

    private static void nwavAsignarTexto(PDAcroForm nwavFormulario, PDTextField nwavCampo, String nwavTexto)
            throws IOException {
        if (nwavCampo.getMaxLen() > 0 && nwavTexto.length() > nwavCampo.getMaxLen()) {
            throw new NwavEditorException("«" + nwavCampo.getFullyQualifiedName() + "» admite como máximo "
                    + nwavCampo.getMaxLen() + " caracteres.");
        }
        try {
            nwavCampo.setValue(nwavTexto);
        } catch (IOException | IllegalArgumentException nwavError) {
            nwavLog.debug("Sin apariencia para {}: {}", nwavCampo.getFullyQualifiedName(), nwavError.toString());
            nwavCampo.getCOSObject().setString(COSName.V, nwavTexto);
            nwavFormulario.setNeedAppearances(true);
        }
    }

    static String nwavValor(PDField nwavCampo) {
        if (nwavCampo instanceof PDCheckBox nwavCasilla) {
            return nwavCasilla.isChecked() ? NwavCampoFormulario.NWAV_MARCADA : NwavCampoFormulario.NWAV_DESMARCADA;
        }
        if (nwavCampo instanceof PDRadioButton nwavRadio) {
            String nwavValor = nwavRadio.getValue();
            return nwavValor == null || "Off".equals(nwavValor) ? "" : nwavValor;
        }
        if (nwavCampo instanceof PDChoice nwavLista) {
            return nwavLista.getValue().isEmpty() ? "" : nwavLista.getValue().get(0);
        }
        return nwavCampo.getValueAsString();
    }

    private static NwavTipoCampo nwavTipo(PDField nwavCampo) {
        if (nwavCampo instanceof PDTextField) {
            return NwavTipoCampo.NWAV_TEXTO;
        }
        if (nwavCampo instanceof PDCheckBox) {
            return NwavTipoCampo.NWAV_CASILLA;
        }
        if (nwavCampo instanceof PDRadioButton) {
            return NwavTipoCampo.NWAV_OPCION;
        }
        if (nwavCampo instanceof PDChoice) {
            return NwavTipoCampo.NWAV_LISTA;
        }
        if (nwavCampo instanceof PDSignatureField) {
            return NwavTipoCampo.NWAV_FIRMA_DIGITAL;
        }
        return null;
    }

    private static int nwavPaginaDe(PDDocument nwavPd, PDAnnotationWidget nwavWidget) {
        PDPage nwavPagina = nwavWidget.getPage();
        if (nwavPagina != null) {
            int nwavIndice = nwavPd.getPages().indexOf(nwavPagina);
            if (nwavIndice >= 0) {
                return nwavIndice;
            }
        }
        for (int nwavIndice = 0; nwavIndice < nwavPd.getNumberOfPages(); nwavIndice++) {
            try {
                for (PDAnnotation nwavAnotacion : nwavPd.getPage(nwavIndice).getAnnotations()) {
                    if (nwavAnotacion.getCOSObject() == nwavWidget.getCOSObject()) {
                        return nwavIndice;
                    }
                }
            } catch (IOException nwavError) {
                nwavLog.debug("No se pudieron leer las anotaciones de la pagina {}", nwavIndice + 1, nwavError);
            }
        }
        return -1;
    }

    private static void nwavMarcarPaginasDeWidgets(NwavDocumentoPdf nwavDocumento, PDAcroForm nwavFormulario,
                                                   Iterable<String> nwavNombres) {
        PDDocument nwavPd = nwavDocumento.nwavPdDocument();
        for (String nwavNombre : nwavNombres) {
            PDField nwavCampo = nwavFormulario.getField(nwavNombre);
            if (nwavCampo instanceof PDTerminalField nwavTerminal) {
                for (PDAnnotationWidget nwavWidget : nwavTerminal.getWidgets()) {
                    int nwavPagina = nwavPaginaDe(nwavPd, nwavWidget);
                    if (nwavPagina >= 0) {
                        nwavDocumento.nwavMarcarModificada(nwavPd.getPage(nwavPagina).getCOSObject());
                    }
                }
            }
        }
    }
}
