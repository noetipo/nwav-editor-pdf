package com.nwav.editorpdf.controller;

import atlantafx.base.theme.Styles;
import com.nwav.editorpdf.model.NwavFirma;
import com.nwav.editorpdf.service.NwavFirmaService;
import com.nwav.editorpdf.ui.NwavImagenFx;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavPanel;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class NwavPanelFirmaController implements NwavPanel {
    private static final DateTimeFormatter NWAV_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color NWAV_TINTA = Color.rgb(16, 37, 120);

    private final NwavVisorController nwavVisor;
    private final NwavFirmaService nwavFirmas;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavNotificador nwavNotificador;

    @FXML private ListView<NwavFirma> nwavListaFirmas;
    @FXML private Button nwavBtnColocar;
    @FXML private Button nwavBtnEliminar;
    @FXML private javafx.scene.control.ToggleGroup nwavGrupoNueva;
    @FXML private ToggleButton nwavTglDibujar;
    @FXML private ToggleButton nwavTglCargar;
    @FXML private ToggleButton nwavTglEscribir;
    @FXML private VBox nwavSeccionEscribir;
    @FXML private TextField nwavTxtEscrita;
    @FXML private javafx.scene.control.ComboBox<String> nwavCmbFuenteEscrita;
    @FXML private ImageView nwavVistaEscrita;
    @FXML private VBox nwavSeccionDibujar;
    @FXML private VBox nwavSeccionCargar;
    @FXML private Canvas nwavLienzo;
    @FXML private CheckBox nwavChkQuitarFondo;
    @FXML private ImageView nwavVistaCargada;
    @FXML private Label nwavLblCargada;
    @FXML private TextField nwavTxtNombre;

    private boolean nwavHayTrazos;
    private byte[] nwavImagenCargada;

    @FXML
    public void initialize() {
        nwavSeccionDibujar.visibleProperty().bind(nwavTglDibujar.selectedProperty());
        nwavSeccionDibujar.managedProperty().bind(nwavSeccionDibujar.visibleProperty());
        nwavSeccionCargar.visibleProperty().bind(nwavTglCargar.selectedProperty());
        nwavSeccionCargar.managedProperty().bind(nwavSeccionCargar.visibleProperty());
        nwavSeccionEscribir.visibleProperty().bind(nwavTglEscribir.selectedProperty());
        nwavSeccionEscribir.managedProperty().bind(nwavSeccionEscribir.visibleProperty());
        nwavGrupoNueva.selectedToggleProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> {
            if (nwavAhora == null) {
                nwavGrupoNueva.selectToggle(nwavAntes);
            }
        });
        nwavCmbFuenteEscrita.getItems().setAll(NwavFirmaService.nwavFuentesManuscritas());
        nwavCmbFuenteEscrita.getSelectionModel().selectFirst();
        nwavCmbFuenteEscrita.setCellFactory(nwavLista -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String nwavFamilia, boolean nwavVacia) {
                super.updateItem(nwavFamilia, nwavVacia);
                setText(nwavVacia || nwavFamilia == null ? null : nwavFamilia);
                setFont(nwavVacia || nwavFamilia == null ? javafx.scene.text.Font.getDefault()
                        : javafx.scene.text.Font.font(nwavFamilia, 18));
            }
        });
        nwavTxtEscrita.textProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavPrevisualizarEscrita());
        nwavCmbFuenteEscrita.valueProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavPrevisualizarEscrita());
        nwavListaFirmas.setCellFactory(nwavLista -> new NwavCeldaFirma());
        nwavListaFirmas.setPlaceholder(new Label("Aún no hay firmas guardadas. Dibuja o carga una abajo."));
        nwavListaFirmas.getSelectionModel().selectedItemProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> {
            nwavBtnColocar.setDisable(nwavAhora == null);
            nwavBtnEliminar.setDisable(nwavAhora == null);
        });
        nwavListaFirmas.setOnMouseClicked(nwavEvento -> {
            if (nwavEvento.getClickCount() == 2 && nwavListaFirmas.getSelectionModel().getSelectedItem() != null) {
                nwavOnColocar();
            }
        });
        nwavConfigurarLienzo();
    }

    private void nwavPrevisualizarEscrita() {
        String nwavTexto = nwavTxtEscrita.getText();
        if (nwavTexto == null || nwavTexto.isBlank()) {
            nwavVistaEscrita.setImage(null);
            return;
        }
        byte[] nwavPng = nwavFirmas.nwavFirmaEscrita(nwavTexto, nwavCmbFuenteEscrita.getValue(), null);
        nwavVistaEscrita.setImage(new javafx.scene.image.Image(new java.io.ByteArrayInputStream(nwavPng)));
    }

    private void nwavConfigurarLienzo() {
        GraphicsContext nwavPincel = nwavLienzo.getGraphicsContext2D();
        nwavPincel.setStroke(NWAV_TINTA);
        nwavPincel.setLineWidth(2.6);
        nwavPincel.setLineCap(StrokeLineCap.ROUND);
        nwavPincel.setLineJoin(StrokeLineJoin.ROUND);
        nwavLienzo.setOnMousePressed(nwavEvento -> {
            nwavPincel.beginPath();
            nwavPincel.moveTo(nwavEvento.getX(), nwavEvento.getY());
            nwavPincel.stroke();
        });
        nwavLienzo.setOnMouseDragged(nwavEvento -> {
            nwavPincel.lineTo(nwavEvento.getX(), nwavEvento.getY());
            nwavPincel.stroke();
            nwavHayTrazos = true;
        });
    }

    @Override
    public void nwavPreparar() {
        nwavRecargar();
    }

    private void nwavRecargar() {
        nwavTareaFondo.nwavEjecutarSilencioso(nwavFirmas::nwavListar, nwavLista -> {
            nwavListaFirmas.getItems().setAll(nwavLista);
            if (!nwavLista.isEmpty()) {
                nwavListaFirmas.getSelectionModel().selectFirst();
            }
        });
    }

    @FXML
    private void nwavOnLimpiarLienzo() {
        nwavLienzo.getGraphicsContext2D().clearRect(0, 0, nwavLienzo.getWidth(), nwavLienzo.getHeight());
        nwavHayTrazos = false;
    }

    @FXML
    private void nwavOnElegirImagen() {
        FileChooser nwavSelector = new FileChooser();
        nwavSelector.setTitle("Imagen de la firma");
        nwavSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes",
                "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.PNG", "*.JPG", "*.JPEG"));
        File nwavElegida = nwavSelector.showOpenDialog(nwavVisor.nwavVentana());
        if (nwavElegida == null) {
            return;
        }
        try {
            nwavImagenCargada = Files.readAllBytes(nwavElegida.toPath());
            nwavVistaCargada.setImage(new Image(new ByteArrayInputStream(nwavImagenCargada)));
            nwavLblCargada.setText(nwavElegida.getName());
            if (nwavTxtNombre.getText().isBlank()) {
                nwavTxtNombre.setText(nwavElegida.getName().replaceAll("\\.[^.]+$", ""));
            }
        } catch (java.io.IOException nwavError) {
            nwavNotificador.nwavError("No se pudo leer «" + nwavElegida.getName() + "».");
        }
    }

    @FXML
    private void nwavOnGuardarFirma() {
        String nwavNombre = nwavTxtNombre.getText();
        byte[] nwavOrigen;
        boolean nwavQuitarFondo;
        if (nwavTglEscribir.isSelected()) {
            if (nwavTxtEscrita.getText().isBlank()) {
                nwavNotificador.nwavAdvertencia("Escribe tu nombre para crear la firma.");
                return;
            }
            nwavOrigen = nwavFirmas.nwavFirmaEscrita(nwavTxtEscrita.getText(), nwavCmbFuenteEscrita.getValue(), null);
            nwavQuitarFondo = false;
        } else if (nwavTglDibujar.isSelected()) {
            if (!nwavHayTrazos) {
                nwavNotificador.nwavAdvertencia("Dibuja la firma en el recuadro.");
                return;
            }
            SnapshotParameters nwavParametros = new SnapshotParameters();
            nwavParametros.setFill(Color.TRANSPARENT);
            nwavOrigen = NwavImagenFx.nwavAPng(nwavLienzo.snapshot(nwavParametros, null));
            nwavQuitarFondo = false;
        } else {
            if (nwavImagenCargada == null) {
                nwavNotificador.nwavAdvertencia("Elige primero la imagen de la firma.");
                return;
            }
            nwavOrigen = nwavImagenCargada;
            nwavQuitarFondo = nwavChkQuitarFondo.isSelected();
        }
        nwavTareaFondo.nwavEjecutar("Guardando firma…",
                () -> nwavFirmas.nwavGuardar(nwavNombre, nwavFirmas.nwavPrepararImagen(nwavOrigen, nwavQuitarFondo)),
                nwavGuardada -> {
                    nwavNotificador.nwavExito("Firma «" + nwavGuardada.getNwavNombre() + "» guardada.");
                    nwavOnLimpiarLienzo();
                    nwavTxtNombre.clear();
                    nwavTxtEscrita.clear();
                    nwavImagenCargada = null;
                    nwavVistaCargada.setImage(null);
                    nwavLblCargada.setText("Ninguna imagen elegida");
                    nwavRecargar();
                }, null);
    }

    @FXML
    private void nwavOnColocar() {
        NwavFirma nwavFirma = nwavListaFirmas.getSelectionModel().getSelectedItem();
        if (nwavFirma == null || nwavVisor.nwavDocumentoActual().isEmpty()) {
            return;
        }
        nwavNotificador.nwavCerrarPanel();
        nwavVisor.nwavColocarFirma(nwavFirma.getNwavImagen());
    }

    @FXML
    private void nwavOnEliminar() {
        NwavFirma nwavFirma = nwavListaFirmas.getSelectionModel().getSelectedItem();
        if (nwavFirma == null) {
            return;
        }
        nwavNotificador.nwavConfirmar("Eliminar firma", "Se eliminará la firma guardada «" + nwavFirma.getNwavNombre()
                        + "». Los PDF donde ya la usaste no cambian.", "Eliminar",
                () -> nwavTareaFondo.nwavEjecutar("Eliminando firma…", () -> {
                    nwavFirmas.nwavEliminar(nwavFirma.getNwavId());
                    return null;
                }, nwavNada -> nwavRecargar(), null));
    }

    @FXML
    private void nwavOnCerrar() {
        nwavNotificador.nwavCerrarPanel();
    }

    private static final class NwavCeldaFirma extends ListCell<NwavFirma> {
        private final ImageView nwavVista = new ImageView();
        private final Label nwavNombre = new Label();
        private final Label nwavFecha = new Label();
        private final HBox nwavFila;

        private NwavCeldaFirma() {
            nwavVista.setFitWidth(140);
            nwavVista.setFitHeight(48);
            nwavVista.setPreserveRatio(true);
            nwavNombre.getStyleClass().add("nwav-reciente-nombre");
            nwavFecha.getStyleClass().add(Styles.TEXT_MUTED);
            VBox nwavTextos = new VBox(2, nwavNombre, nwavFecha);
            nwavFila = new HBox(12, nwavVista, nwavTextos);
            nwavFila.setAlignment(Pos.CENTER_LEFT);
            nwavFila.getStyleClass().add("nwav-fila-firma");
        }

        @Override
        protected void updateItem(NwavFirma nwavFirma, boolean nwavVacia) {
            super.updateItem(nwavFirma, nwavVacia);
            if (nwavVacia || nwavFirma == null) {
                setGraphic(null);
                return;
            }
            nwavVista.setImage(new Image(new ByteArrayInputStream(nwavFirma.getNwavImagen())));
            nwavNombre.setText(nwavFirma.getNwavNombre());
            nwavFecha.setText("Guardada el " + NWAV_FORMATO.format(nwavFirma.getNwavFechaCreacion()));
            setGraphic(nwavFila);
        }
    }
}
