package com.nwav.editorpdf.controller;

import com.nwav.editorpdf.service.NwavArchivoPdfService;
import com.nwav.editorpdf.service.NwavConfiguracionService;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavPanel;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Panel "Unir PDF": combina varios archivos, en el orden de la lista, en uno nuevo. */
@Component
@RequiredArgsConstructor
public class NwavPanelUnirController implements NwavPanel {

    private final NwavVisorController nwavVisor;
    private final NwavArchivoPdfService nwavArchivos;
    private final NwavConfiguracionService nwavConfiguracion;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavNotificador nwavNotificador;

    @FXML private ListView<Path> nwavListaArchivos;
    @FXML private Button nwavBtnAnadirAbierto;
    @FXML private Button nwavBtnQuitar;
    @FXML private Button nwavBtnSubir;
    @FXML private Button nwavBtnBajar;
    @FXML private CheckBox nwavChkAbrir;
    @FXML private Label nwavLblResumen;
    @FXML private Button nwavBtnUnir;

    @FXML
    public void initialize() {
        nwavListaArchivos.setCellFactory(nwavLista -> new ListCell<>() {
            private final Label nwavNombre = new Label();
            private final Label nwavCarpeta = new Label();
            private final VBox nwavContenido = new VBox(2, nwavNombre, nwavCarpeta);

            {
                nwavNombre.getStyleClass().add("nwav-reciente-nombre");
                nwavCarpeta.getStyleClass().add("nwav-reciente-ruta");
            }

            @Override
            protected void updateItem(Path nwavRuta, boolean nwavVacia) {
                super.updateItem(nwavRuta, nwavVacia);
                if (nwavVacia || nwavRuta == null) {
                    setGraphic(null);
                    return;
                }
                nwavNombre.setText((getIndex() + 1) + ". " + nwavRuta.getFileName());
                nwavCarpeta.setText(String.valueOf(nwavRuta.getParent()));
                setGraphic(nwavContenido);
            }
        });
        nwavListaArchivos.setPlaceholder(new Label("Añade al menos dos PDF. Se unen en el orden de la lista."));
        nwavListaArchivos.getItems().addListener((ListChangeListener<Path>) nwavCambio -> nwavActualizar());
        nwavListaArchivos.getSelectionModel().selectedIndexProperty()
                .addListener((nwavObs, nwavAntes, nwavAhora) -> nwavActualizar());
    }

    @Override
    public void nwavPreparar() {
        nwavListaArchivos.getItems().removeIf(nwavRuta -> !Files.isRegularFile(nwavRuta));
        nwavBtnAnadirAbierto.setDisable(nwavVisor.nwavDocumentoActual().isEmpty());
        nwavActualizar();
    }

    private void nwavActualizar() {
        int nwavCantidad = nwavListaArchivos.getItems().size();
        int nwavElegido = nwavListaArchivos.getSelectionModel().getSelectedIndex();
        nwavBtnQuitar.setDisable(nwavElegido < 0);
        nwavBtnSubir.setDisable(nwavElegido <= 0);
        nwavBtnBajar.setDisable(nwavElegido < 0 || nwavElegido >= nwavCantidad - 1);
        nwavBtnUnir.setDisable(nwavCantidad < 2);
        nwavLblResumen.setText(nwavCantidad < 2 ? "Faltan " + (2 - nwavCantidad) + " archivo(s) para poder unir."
                : nwavCantidad + " archivos listos para unir.");
    }

    @FXML
    private void nwavOnAnadir() {
        FileChooser nwavSelector = new FileChooser();
        nwavSelector.setTitle("PDF a unir");
        nwavSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documentos PDF", "*.pdf", "*.PDF"));
        nwavConfiguracion.nwavCarpetaInicial().map(Path::toFile).ifPresent(nwavSelector::setInitialDirectory);
        List<File> nwavElegidos = nwavSelector.showOpenMultipleDialog(nwavVisor.nwavVentana());
        if (nwavElegidos != null) {
            nwavElegidos.forEach(nwavArchivo -> nwavListaArchivos.getItems().add(nwavArchivo.toPath()));
        }
    }

    @FXML
    private void nwavOnAnadirAbierto() {
        nwavVisor.nwavDocumentoActual().ifPresent(nwavDocumento -> {
            nwavListaArchivos.getItems().add(nwavDocumento.getNwavRuta());
            if (nwavDocumento.nwavHayCambiosSinGuardar()) {
                nwavNotificador.nwavAdvertencia("Se usará la versión guardada en disco de «"
                        + nwavDocumento.getNwavNombre() + "», sin los cambios pendientes.");
            }
        });
    }

    @FXML
    private void nwavOnQuitar() {
        int nwavElegido = nwavListaArchivos.getSelectionModel().getSelectedIndex();
        if (nwavElegido >= 0) {
            nwavListaArchivos.getItems().remove(nwavElegido);
        }
    }

    @FXML
    private void nwavOnSubir() {
        nwavMoverElegido(-1);
    }

    @FXML
    private void nwavOnBajar() {
        nwavMoverElegido(1);
    }

    private void nwavMoverElegido(int nwavDesplazamiento) {
        int nwavDesde = nwavListaArchivos.getSelectionModel().getSelectedIndex();
        int nwavHasta = nwavDesde + nwavDesplazamiento;
        if (nwavDesde < 0 || nwavHasta < 0 || nwavHasta >= nwavListaArchivos.getItems().size()) {
            return;
        }
        Path nwavRuta = nwavListaArchivos.getItems().remove(nwavDesde);
        nwavListaArchivos.getItems().add(nwavHasta, nwavRuta);
        nwavListaArchivos.getSelectionModel().clearAndSelect(nwavHasta);
    }

    @FXML
    private void nwavOnUnir() {
        List<Path> nwavOrigenes = List.copyOf(nwavListaArchivos.getItems());
        if (nwavOrigenes.size() < 2) {
            return;
        }
        FileChooser nwavSelector = new FileChooser();
        nwavSelector.setTitle("Guardar PDF unido");
        nwavSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        Path nwavCarpeta = nwavOrigenes.get(0).getParent();
        if (nwavCarpeta != null && nwavCarpeta.toFile().isDirectory()) {
            nwavSelector.setInitialDirectory(nwavCarpeta.toFile());
        }
        nwavSelector.setInitialFileName("unido.pdf");
        File nwavElegido = nwavSelector.showSaveDialog(nwavVisor.nwavVentana());
        if (nwavElegido == null) {
            return;
        }
        Path nwavDestino = NwavVisorController.nwavConExtensionPdf(nwavElegido.toPath());
        boolean nwavAbrir = nwavChkAbrir.isSelected();
        nwavTareaFondo.nwavEjecutarConProgreso("Uniendo " + nwavOrigenes.size() + " archivos…",
                nwavAvance -> nwavArchivos.nwavUnir(nwavOrigenes, nwavDestino, nwavAvance),
                nwavRuta -> {
                    nwavListaArchivos.getItems().clear();
                    nwavNotificador.nwavCerrarPanel();
                    nwavNotificador.nwavExito("Se creó «" + nwavRuta.getFileName() + "».");
                    if (nwavAbrir) {
                        nwavVisor.nwavAbrirArchivo(nwavRuta);
                    }
                }, null);
    }

    @FXML
    private void nwavOnCerrar() {
        nwavNotificador.nwavCerrarPanel();
    }
}
