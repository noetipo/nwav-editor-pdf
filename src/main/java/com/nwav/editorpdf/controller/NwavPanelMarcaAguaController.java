package com.nwav.editorpdf.controller;

import com.nwav.editorpdf.model.NwavColorRgb;
import com.nwav.editorpdf.model.NwavFuente;
import com.nwav.editorpdf.model.NwavMarcaAgua;
import com.nwav.editorpdf.model.NwavOpcionesMarcaAgua;
import com.nwav.editorpdf.model.NwavTipoMarcaAgua;
import com.nwav.editorpdf.service.NwavEdicionService;
import com.nwav.editorpdf.service.NwavMarcaAguaService;
import com.nwav.editorpdf.ui.NwavEstadoHerramientas;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavPanel;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

/** Panel "Marca de agua": texto o imagen, con plantillas guardadas en SQLite. */
@Component
@RequiredArgsConstructor
public class NwavPanelMarcaAguaController implements NwavPanel {

    private final NwavVisorController nwavVisor;
    private final NwavEdicionService nwavEdicion;
    private final NwavMarcaAguaService nwavPlantillas;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavNotificador nwavNotificador;

    @FXML private ComboBox<NwavMarcaAgua> nwavCmbPlantillas;
    @FXML private Button nwavBtnEliminarPlantilla;
    @FXML private javafx.scene.control.ToggleGroup nwavGrupoTipo;
    @FXML private ToggleButton nwavTglTexto;
    @FXML private ToggleButton nwavTglImagen;
    @FXML private VBox nwavSeccionTexto;
    @FXML private VBox nwavSeccionImagen;
    @FXML private TextField nwavTxtTexto;
    @FXML private ComboBox<NwavFuente> nwavCmbFuente;
    @FXML private Spinner<Integer> nwavSpnTamano;
    @FXML private ColorPicker nwavSelectorColor;
    @FXML private Label nwavLblImagen;
    @FXML private ImageView nwavVistaImagen;
    @FXML private Slider nwavSldEscala;
    @FXML private Label nwavLblEscala;
    @FXML private Slider nwavSldOpacidad;
    @FXML private Label nwavLblOpacidad;
    @FXML private Slider nwavSldRotacion;
    @FXML private Label nwavLblRotacion;
    @FXML private TextField nwavTxtPaginas;
    @FXML private TextField nwavTxtNombrePlantilla;

    private byte[] nwavImagen;

    @FXML
    public void initialize() {
        nwavSeccionTexto.visibleProperty().bind(nwavTglTexto.selectedProperty());
        nwavSeccionTexto.managedProperty().bind(nwavSeccionTexto.visibleProperty());
        nwavSeccionImagen.visibleProperty().bind(nwavTglImagen.selectedProperty());
        nwavSeccionImagen.managedProperty().bind(nwavSeccionImagen.visibleProperty());
        nwavGrupoTipo.selectedToggleProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> {
            if (nwavAhora == null) {
                nwavGrupoTipo.selectToggle(nwavAntes);
            }
        });
        nwavCmbFuente.getItems().setAll(NwavFuente.values());
        nwavCmbFuente.setValue(NwavFuente.NWAV_HELVETICA);
        nwavSpnTamano.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 300, 60));
        nwavSelectorColor.setValue(Color.rgb(150, 150, 150));
        nwavEnlazar(nwavSldEscala, nwavLblEscala, "%.0f %% del ancho", 100);
        nwavEnlazar(nwavSldOpacidad, nwavLblOpacidad, "%.0f %%", 100);
        nwavEnlazar(nwavSldRotacion, nwavLblRotacion, "%.0f°", 1);
        nwavCmbPlantillas.setCellFactory(nwavLista -> new NwavCeldaPlantilla());
        nwavCmbPlantillas.setButtonCell(new NwavCeldaPlantilla());
        nwavCmbPlantillas.valueProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> {
            nwavBtnEliminarPlantilla.setDisable(nwavAhora == null);
            if (nwavAhora != null) {
                nwavCargarPlantilla(nwavAhora);
            }
        });
        nwavBtnEliminarPlantilla.setDisable(true);
    }

    private static void nwavEnlazar(Slider nwavDeslizador, Label nwavEtiqueta, String nwavFormato, double nwavFactor) {
        nwavEtiqueta.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> String.format(Locale.ROOT, nwavFormato, nwavDeslizador.getValue() * nwavFactor),
                nwavDeslizador.valueProperty()));
    }

    @Override
    public void nwavPreparar() {
        nwavCargarPlantillas(null);
    }

    private void nwavCargarPlantillas(String nwavSeleccionar) {
        nwavTareaFondo.nwavEjecutarSilencioso(nwavPlantillas::nwavListar, nwavLista -> {
            nwavCmbPlantillas.getItems().setAll(nwavLista);
            nwavLista.stream().filter(nwavPlantilla -> nwavPlantilla.getNwavNombre().equals(nwavSeleccionar))
                    .findFirst().ifPresent(nwavCmbPlantillas::setValue);
        });
    }

    private void nwavCargarPlantilla(NwavMarcaAgua nwavPlantilla) {
        NwavOpcionesMarcaAgua nwavOpciones = NwavMarcaAguaService.nwavAOpciones(nwavPlantilla);
        (nwavOpciones.nwavTipo() == NwavTipoMarcaAgua.NWAV_IMAGEN ? nwavTglImagen : nwavTglTexto).setSelected(true);
        nwavTxtTexto.setText(nwavOpciones.nwavTexto() == null ? "" : nwavOpciones.nwavTexto());
        nwavCmbFuente.setValue(nwavOpciones.nwavFuente());
        nwavSpnTamano.getValueFactory().setValue((int) Math.round(nwavOpciones.nwavTamano()));
        NwavColorRgb nwavColor = nwavOpciones.nwavColor();
        nwavSelectorColor.setValue(Color.rgb(nwavColor.nwavRojo(), nwavColor.nwavVerde(), nwavColor.nwavAzul()));
        nwavSldOpacidad.setValue(nwavOpciones.nwavOpacidad());
        nwavSldRotacion.setValue(nwavOpciones.nwavRotacion());
        nwavSldEscala.setValue(nwavOpciones.nwavEscala());
        nwavPonerImagen(nwavOpciones.nwavImagen(), nwavPlantilla.getNwavNombre());
        nwavTxtNombrePlantilla.setText(nwavPlantilla.getNwavNombre());
    }

    @FXML
    private void nwavOnElegirImagen() {
        FileChooser nwavSelector = new FileChooser();
        nwavSelector.setTitle("Imagen de la marca de agua");
        nwavSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes",
                "*.png", "*.jpg", "*.jpeg", "*.PNG", "*.JPG", "*.JPEG"));
        File nwavElegida = nwavSelector.showOpenDialog(nwavVisor.nwavVentana());
        if (nwavElegida == null) {
            return;
        }
        try {
            nwavPonerImagen(Files.readAllBytes(nwavElegida.toPath()), nwavElegida.getName());
        } catch (IOException nwavError) {
            nwavNotificador.nwavError("No se pudo leer «" + nwavElegida.getName() + "».");
        }
    }

    private void nwavPonerImagen(byte[] nwavBytes, String nwavNombre) {
        nwavImagen = nwavBytes;
        nwavVistaImagen.setImage(nwavBytes == null ? null : new Image(new ByteArrayInputStream(nwavBytes)));
        nwavLblImagen.setText(nwavBytes == null ? "Ninguna imagen elegida" : nwavNombre);
    }

    private NwavOpcionesMarcaAgua nwavOpciones() {
        return new NwavOpcionesMarcaAgua(nwavTglImagen.isSelected() ? NwavTipoMarcaAgua.NWAV_IMAGEN : NwavTipoMarcaAgua.NWAV_TEXTO,
                nwavTxtTexto.getText(), nwavImagen, nwavCmbFuente.getValue(), nwavSpnTamano.getValue(),
                NwavEstadoHerramientas.nwavConvertir(nwavSelectorColor.getValue()), nwavSldOpacidad.getValue(),
                nwavSldRotacion.getValue(), nwavSldEscala.getValue(), nwavTxtPaginas.getText());
    }

    @FXML
    private void nwavOnGuardarPlantilla() {
        String nwavNombre = nwavTxtNombrePlantilla.getText();
        NwavOpcionesMarcaAgua nwavOpciones = nwavOpciones();
        nwavTareaFondo.nwavEjecutar("Guardando plantilla…", () -> nwavPlantillas.nwavGuardar(nwavNombre, nwavOpciones),
                nwavGuardada -> {
                    nwavNotificador.nwavExito("Plantilla «" + nwavGuardada.getNwavNombre() + "» guardada.");
                    nwavCargarPlantillas(nwavGuardada.getNwavNombre());
                }, null);
    }

    @FXML
    private void nwavOnEliminarPlantilla() {
        NwavMarcaAgua nwavPlantilla = nwavCmbPlantillas.getValue();
        if (nwavPlantilla == null) {
            return;
        }
        nwavTareaFondo.nwavEjecutar("Eliminando plantilla…", () -> {
            nwavPlantillas.nwavEliminar(nwavPlantilla.getNwavId());
            return null;
        }, nwavNada -> {
            nwavNotificador.nwavInfo("Plantilla «" + nwavPlantilla.getNwavNombre() + "» eliminada.");
            nwavCmbPlantillas.setValue(null);
            nwavCargarPlantillas(null);
        }, null);
    }

    @FXML
    private void nwavOnAplicar() {
        NwavOpcionesMarcaAgua nwavOpciones = nwavOpciones();
        nwavVisor.nwavEditar("Aplicando marca de agua…", nwavDoc -> nwavEdicion.nwavMarcaAgua(nwavDoc, nwavOpciones),
                nwavResultado -> {
                    nwavNotificador.nwavCerrarPanel();
                    nwavNotificador.nwavExito(nwavResultado.nwavDescripcion() + ". Puedes deshacerlo con Ctrl/⌘ + Z.");
                }, null);
    }

    @FXML
    private void nwavOnCerrar() {
        nwavNotificador.nwavCerrarPanel();
    }

    private static final class NwavCeldaPlantilla extends ListCell<NwavMarcaAgua> {
        @Override
        protected void updateItem(NwavMarcaAgua nwavPlantilla, boolean nwavVacia) {
            super.updateItem(nwavPlantilla, nwavVacia);
            setText(nwavVacia || nwavPlantilla == null ? "Elegir una plantilla guardada…"
                    : nwavPlantilla.getNwavNombre() + (nwavPlantilla.getNwavTipo() == NwavTipoMarcaAgua.NWAV_IMAGEN
                    ? "  (imagen)" : "  («" + nwavPlantilla.getNwavTexto() + "»)"));
        }
    }
}
