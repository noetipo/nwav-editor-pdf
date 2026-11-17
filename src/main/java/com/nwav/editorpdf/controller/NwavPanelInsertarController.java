package com.nwav.editorpdf.controller;

import com.nwav.editorpdf.exception.NwavContrasenaIncorrectaException;
import com.nwav.editorpdf.exception.NwavContrasenaRequeridaException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.service.NwavEdicionService;
import com.nwav.editorpdf.service.NwavEdicionService.NwavResultadoEdicion;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavPanel;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class NwavPanelInsertarController implements NwavPanel {
    private static final int NWAV_AL_PRINCIPIO = 0;
    private static final int NWAV_ANTES_DE_ACTUAL = 1;
    private static final int NWAV_DESPUES_DE_ACTUAL = 2;

    private final NwavVisorController nwavVisor;
    private final NwavEdicionService nwavEdicion;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavNotificador nwavNotificador;

    @FXML private ToggleGroup nwavGrupoOrigen;
    @FXML private ToggleButton nwavTglDesdePdf;
    @FXML private ToggleButton nwavTglDesdeImagenes;
    @FXML private VBox nwavSeccionPdf;
    @FXML private VBox nwavSeccionImagenes;
    @FXML private Label nwavLblPdfElegido;
    @FXML private TextField nwavTxtPaginasOrigen;
    @FXML private ListView<Path> nwavListaImagenes;
    @FXML private ComboBox<String> nwavCmbPosicion;
    @FXML private Button nwavBtnInsertar;

    private Path nwavPdfElegido;
    private String nwavContrasenaPdf;

    @FXML
    public void initialize() {
        nwavSeccionPdf.visibleProperty().bind(nwavTglDesdePdf.selectedProperty());
        nwavSeccionPdf.managedProperty().bind(nwavSeccionPdf.visibleProperty());
        nwavSeccionImagenes.visibleProperty().bind(nwavTglDesdeImagenes.selectedProperty());
        nwavSeccionImagenes.managedProperty().bind(nwavSeccionImagenes.visibleProperty());
        nwavGrupoOrigen.selectedToggleProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> {
            if (nwavAhora == null) {
                nwavGrupoOrigen.selectToggle(nwavAntes);
            }
            nwavActualizarBoton();
        });
        nwavListaImagenes.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        nwavListaImagenes.setCellFactory(nwavLista -> new ListCell<>() {
            @Override
            protected void updateItem(Path nwavRuta, boolean nwavVacia) {
                super.updateItem(nwavRuta, nwavVacia);
                setText(nwavVacia || nwavRuta == null ? null : nwavRuta.getFileName().toString());
            }
        });
        nwavListaImagenes.setPlaceholder(new Label("Añade PNG, JPG, GIF o BMP. Cada imagen será una página."));
        nwavListaImagenes.getItems().addListener((javafx.collections.ListChangeListener<Path>) nwavCambio ->
                nwavActualizarBoton());
    }

    @Override
    public void nwavPreparar() {
        int nwavActual = nwavVisor.nwavPaginaActual() + 1;
        int nwavTotal = nwavVisor.nwavDocumentoActual().map(nwavDoc -> nwavDoc.getNwavNumeroPaginas()).orElse(0);
        nwavCmbPosicion.getItems().setAll("Al principio", "Antes de la página " + nwavActual,
                "Después de la página " + nwavActual, "Al final (después de la página " + nwavTotal + ")");
        nwavCmbPosicion.getSelectionModel().select(NWAV_DESPUES_DE_ACTUAL);
        nwavActualizarBoton();
    }

    @FXML
    private void nwavOnElegirPdf() {
        FileChooser nwavSelector = new FileChooser();
        nwavSelector.setTitle("PDF del que insertar páginas");
        nwavSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documentos PDF", "*.pdf", "*.PDF"));
        nwavVisor.nwavDocumentoActual().map(nwavDoc -> nwavDoc.getNwavRuta().getParent())
                .filter(nwavCarpeta -> nwavCarpeta.toFile().isDirectory())
                .ifPresent(nwavCarpeta -> nwavSelector.setInitialDirectory(nwavCarpeta.toFile()));
        File nwavElegido = nwavSelector.showOpenDialog(nwavVisor.nwavVentana());
        if (nwavElegido != null) {
            nwavPdfElegido = nwavElegido.toPath();
            nwavContrasenaPdf = null;
            nwavLblPdfElegido.setText(nwavElegido.getName());
            nwavActualizarBoton();
        }
    }

    @FXML
    private void nwavOnAnadirImagenes() {
        FileChooser nwavSelector = new FileChooser();
        nwavSelector.setTitle("Imágenes a insertar");
        nwavSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes",
                "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.PNG", "*.JPG", "*.JPEG"));
        List<File> nwavElegidas = nwavSelector.showOpenMultipleDialog(nwavVisor.nwavVentana());
        if (nwavElegidas != null) {
            nwavElegidas.stream().map(File::toPath)
                    .filter(nwavRuta -> !nwavListaImagenes.getItems().contains(nwavRuta))
                    .forEach(nwavListaImagenes.getItems()::add);
        }
    }

    @FXML
    private void nwavOnQuitarImagenes() {
        nwavListaImagenes.getItems().removeAll(List.copyOf(nwavListaImagenes.getSelectionModel().getSelectedItems()));
    }

    private void nwavActualizarBoton() {
        boolean nwavListo = nwavTglDesdePdf.isSelected()
                ? nwavPdfElegido != null
                : !nwavListaImagenes.getItems().isEmpty();
        nwavBtnInsertar.setDisable(!nwavListo || nwavVisor.nwavDocumentoActual().isEmpty());
    }

    private int nwavPosicion() {
        int nwavActual = nwavVisor.nwavPaginaActual();
        return switch (nwavCmbPosicion.getSelectionModel().getSelectedIndex()) {
            case NWAV_AL_PRINCIPIO -> 0;
            case NWAV_ANTES_DE_ACTUAL -> nwavActual;
            case NWAV_DESPUES_DE_ACTUAL -> nwavActual + 1;
            default -> Integer.MAX_VALUE;
        };
    }

    @FXML
    private void nwavOnInsertar() {
        int nwavPosicion = nwavPosicion();
        Consumer<NwavResultadoEdicion> nwavAlTerminar = nwavResultado -> {
            nwavNotificador.nwavCerrarPanel();
            nwavNotificador.nwavExito(nwavResultado.nwavDescripcion() + ".");
        };
        if (nwavTglDesdePdf.isSelected()) {
            Path nwavOrigen = nwavPdfElegido;
            String nwavClave = nwavContrasenaPdf;
            String nwavRangos = nwavTxtPaginasOrigen.getText();
            nwavVisor.nwavEditar("Insertando páginas…",
                    nwavDoc -> nwavEdicion.nwavInsertarPdf(nwavDoc, nwavOrigen, nwavClave, nwavRangos, nwavPosicion),
                    nwavAlTerminar, nwavError -> nwavErrorPdf(nwavOrigen, nwavError));
        } else {
            List<Path> nwavImagenes = List.copyOf(nwavListaImagenes.getItems());
            nwavVisor.nwavEditar("Insertando imágenes…",
                    nwavDoc -> nwavEdicion.nwavInsertarImagenes(nwavDoc, nwavImagenes, nwavPosicion),
                    nwavResultado -> {
                        nwavListaImagenes.getItems().clear();
                        nwavAlTerminar.accept(nwavResultado);
                    }, null);
        }
    }

    private void nwavErrorPdf(Path nwavOrigen, Throwable nwavError) {
        if (nwavError instanceof NwavContrasenaRequeridaException
                || nwavError instanceof NwavContrasenaIncorrectaException) {
            nwavNotificador.nwavPedirContrasena(String.valueOf(nwavOrigen.getFileName()),
                    nwavError instanceof NwavContrasenaIncorrectaException, nwavClave -> {
                        nwavContrasenaPdf = nwavClave;
                        nwavOnInsertar();
                    }, null);
            return;
        }
        nwavNotificador.nwavError(NwavTraductorErrores.nwavMensaje(nwavError));
    }

    @FXML
    private void nwavOnCerrar() {
        nwavNotificador.nwavCerrarPanel();
    }
}
