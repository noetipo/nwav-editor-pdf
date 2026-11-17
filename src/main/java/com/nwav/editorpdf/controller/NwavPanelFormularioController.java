package com.nwav.editorpdf.controller;

import atlantafx.base.theme.Styles;
import com.nwav.editorpdf.model.NwavCampoFormulario;
import com.nwav.editorpdf.model.NwavTipoCampo;
import com.nwav.editorpdf.service.NwavDocumentoPdf;
import com.nwav.editorpdf.service.NwavEdicionService;
import com.nwav.editorpdf.service.NwavFormularioService;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavPanel;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Panel para rellenar formularios AcroForm campo a campo. */
@Component
@RequiredArgsConstructor
public class NwavPanelFormularioController implements NwavPanel {

    private final NwavVisorController nwavVisor;
    private final NwavFormularioService nwavFormularios;
    private final NwavEdicionService nwavEdicion;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavNotificador nwavNotificador;

    @FXML private Label nwavLblResumen;
    @FXML private VBox nwavListaCampos;
    @FXML private Button nwavBtnAplicar;

    /** Valor original y lector del valor actual de cada campo editable. */
    private final Map<String, String> nwavOriginales = new LinkedHashMap<>();
    private final Map<String, Supplier<String>> nwavLectores = new LinkedHashMap<>();

    @Override
    public void nwavPreparar() {
        nwavListaCampos.getChildren().clear();
        nwavOriginales.clear();
        nwavLectores.clear();
        nwavBtnAplicar.setDisable(true);
        NwavDocumentoPdf nwavDocumento = nwavVisor.nwavDocumentoActual().orElse(null);
        if (nwavDocumento == null) {
            nwavLblResumen.setText("No hay ningún documento abierto.");
            return;
        }
        nwavLblResumen.setText("Leyendo el formulario…");
        nwavTareaFondo.nwavEjecutarSilencioso(() -> nwavFormularios.nwavListar(nwavDocumento), this::nwavMostrar);
    }

    private void nwavMostrar(List<NwavCampoFormulario> nwavCampos) {
        if (nwavCampos.isEmpty()) {
            nwavLblResumen.setText("Este documento no tiene campos de formulario.");
            return;
        }
        long nwavEditables = nwavCampos.stream().filter(nwavCampo -> !nwavCampo.nwavSoloLectura()).count();
        nwavLblResumen.setText(nwavCampos.size() + " campos · " + nwavEditables + " editables. "
                + "Los obligatorios llevan *. Pulsa ⌖ para ver dónde está cada uno.");
        for (NwavCampoFormulario nwavCampo : nwavCampos) {
            nwavListaCampos.getChildren().add(nwavFila(nwavCampo));
        }
        nwavBtnAplicar.setDisable(nwavEditables == 0);
    }

    private VBox nwavFila(NwavCampoFormulario nwavCampo) {
        Label nwavEtiqueta = new Label(nwavCampo.nwavEtiqueta() + (nwavCampo.nwavRequerido() ? " *" : ""));
        nwavEtiqueta.getStyleClass().add("nwav-campo-label");
        nwavEtiqueta.setWrapText(true);
        Label nwavTipo = new Label(nwavCampo.nwavTipo().getNwavDescripcion()
                + (nwavCampo.nwavSoloLectura() ? " · solo lectura" : "")
                + (nwavCampo.nwavPagina() >= 0 ? " · pág. " + (nwavCampo.nwavPagina() + 1) : ""));
        nwavTipo.getStyleClass().add("nwav-panel-ayuda");
        Button nwavIr = new Button(null, new FontIcon(Feather.CROSSHAIR));
        nwavIr.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.SMALL);
        nwavIr.setTooltip(new Tooltip("Ver el campo en la página"));
        nwavIr.setDisable(nwavCampo.nwavPagina() < 0);
        nwavIr.setOnAction(nwavEvento -> nwavVisor.nwavIrAZona(nwavCampo.nwavPagina(), nwavCampo.nwavZona()));
        VBox nwavTextos = new VBox(1, nwavEtiqueta, nwavTipo);
        HBox.setHgrow(nwavTextos, Priority.ALWAYS);
        HBox nwavCabecera = new HBox(6, nwavTextos, nwavIr);
        nwavCabecera.setAlignment(Pos.CENTER_LEFT);

        Control nwavEditor = nwavEditor(nwavCampo);
        nwavEditor.setDisable(nwavCampo.nwavSoloLectura());
        nwavEditor.setMaxWidth(Double.MAX_VALUE);
        VBox nwavFila = new VBox(6, nwavCabecera, nwavEditor);
        nwavFila.getStyleClass().add("nwav-fila-campo");
        return nwavFila;
    }

    private Control nwavEditor(NwavCampoFormulario nwavCampo) {
        String nwavValor = Objects.requireNonNullElse(nwavCampo.nwavValor(), "");
        Control nwavControl;
        Supplier<String> nwavLector;
        if (nwavCampo.nwavTipo() == NwavTipoCampo.NWAV_CASILLA) {
            CheckBox nwavCasilla = new CheckBox("Marcada");
            nwavCasilla.setSelected(Boolean.parseBoolean(nwavValor));
            nwavLector = () -> String.valueOf(nwavCasilla.isSelected());
            nwavCasilla.selectedProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavActualizarBoton());
            nwavControl = nwavCasilla;
        } else if (nwavCampo.nwavTipo() == NwavTipoCampo.NWAV_OPCION || nwavCampo.nwavTipo() == NwavTipoCampo.NWAV_LISTA) {
            ComboBox<String> nwavLista = new ComboBox<>();
            if (nwavCampo.nwavTipo() == NwavTipoCampo.NWAV_OPCION) {
                nwavLista.getItems().add(""); // ninguna opcion elegida
            }
            nwavLista.getItems().addAll(nwavCampo.nwavOpciones());
            nwavLista.setValue(nwavValor);
            nwavLector = () -> Objects.requireNonNullElse(nwavLista.getValue(), "");
            nwavLista.valueProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavActualizarBoton());
            nwavControl = nwavLista;
        } else if (nwavCampo.nwavTipo() == NwavTipoCampo.NWAV_FIRMA_DIGITAL) {
            TextField nwavAviso = new TextField("Firma digital: no se puede rellenar aquí");
            nwavLector = () -> nwavValor;
            nwavControl = nwavAviso;
        } else {
            TextInputControl nwavTexto = nwavCampo.nwavMultilinea() ? new TextArea(nwavValor) : new TextField(nwavValor);
            if (nwavTexto instanceof TextArea nwavArea) {
                nwavArea.setPrefRowCount(3);
                nwavArea.setWrapText(true);
            }
            nwavLector = nwavTexto::getText;
            nwavTexto.textProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavActualizarBoton());
            nwavControl = nwavTexto;
        }
        if (!nwavCampo.nwavSoloLectura()) {
            nwavOriginales.put(nwavCampo.nwavNombre(), nwavValor);
            nwavLectores.put(nwavCampo.nwavNombre(), nwavLector);
        }
        return nwavControl;
    }

    private Map<String, String> nwavCambios() {
        Map<String, String> nwavCambios = new LinkedHashMap<>();
        nwavLectores.forEach((nwavNombre, nwavLector) -> {
            String nwavActual = nwavLector.get();
            if (!Objects.equals(nwavActual, nwavOriginales.get(nwavNombre))) {
                nwavCambios.put(nwavNombre, nwavActual);
            }
        });
        return nwavCambios;
    }

    private void nwavActualizarBoton() {
        int nwavCantidad = nwavCambios().size();
        nwavBtnAplicar.setDisable(nwavCantidad == 0);
        nwavBtnAplicar.setText(nwavCantidad == 0 ? "Aplicar" : "Aplicar " + nwavCantidad
                + (nwavCantidad == 1 ? " cambio" : " cambios"));
    }

    @FXML
    private void nwavOnAplicar() {
        Map<String, String> nwavCambios = nwavCambios();
        if (nwavCambios.isEmpty()) {
            return;
        }
        nwavVisor.nwavEditar("Rellenando formulario…", nwavDoc -> nwavEdicion.nwavRellenarFormulario(nwavDoc, nwavCambios),
                nwavResultado -> {
                    nwavNotificador.nwavCerrarPanel();
                    nwavNotificador.nwavExito(nwavResultado.nwavDescripcion() + ". Guarda una copia para conservarlos.");
                }, null);
    }

    @FXML
    private void nwavOnCerrar() {
        nwavNotificador.nwavCerrarPanel();
    }
}
