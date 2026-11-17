package com.nwav.editorpdf.controller;

import com.nwav.editorpdf.model.NwavHistorialOperacion;
import com.nwav.editorpdf.model.NwavResultadoOperacion;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.NwavHistorialService;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavRefrescable;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Consulta del historial de operaciones ({@code nwav_historial_operacion}). */
@Component
@RequiredArgsConstructor
public class NwavHistorialController implements NwavRefrescable {

    private static final DateTimeFormatter NWAV_FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String NWAV_TODAS = "Todas las operaciones";
    private static final String NWAV_TODOS = "Todos los resultados";
    private static final String NWAV_EXITO = "Correctas";
    private static final String NWAV_ERROR = "Con error";

    private final NwavHistorialService nwavHistorial;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavNotificador nwavNotificador;
    private final NwavMainController nwavMain;

    @FXML private TableView<NwavHistorialOperacion> nwavTabla;
    @FXML private TableColumn<NwavHistorialOperacion, LocalDateTime> nwavColFecha;
    @FXML private TableColumn<NwavHistorialOperacion, String> nwavColOperacion;
    @FXML private TableColumn<NwavHistorialOperacion, String> nwavColOrigen;
    @FXML private TableColumn<NwavHistorialOperacion, String> nwavColDestino;
    @FXML private TableColumn<NwavHistorialOperacion, NwavResultadoOperacion> nwavColResultado;
    @FXML private TableColumn<NwavHistorialOperacion, String> nwavColDetalle;
    @FXML private ComboBox<String> nwavCmbTipo;
    @FXML private ComboBox<String> nwavCmbResultado;
    @FXML private TextField nwavTxtBuscar;
    @FXML private Label nwavLblResumen;

    private final ObservableList<NwavHistorialOperacion> nwavTodas = FXCollections.observableArrayList();
    private final FilteredList<NwavHistorialOperacion> nwavFiltradas = new FilteredList<>(nwavTodas, nwavFila -> true);

    @FXML
    public void initialize() {
        nwavTabla.setItems(nwavFiltradas);
        nwavTabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        nwavTabla.setPlaceholder(nwavCrearVacio());

        nwavColFecha.setCellValueFactory(nwavFila -> new SimpleObjectProperty<>(nwavFila.getValue().getNwavFecha()));
        nwavColFecha.setCellFactory(nwavColumna -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime nwavFecha, boolean nwavVacia) {
                super.updateItem(nwavFecha, nwavVacia);
                setText(nwavVacia || nwavFecha == null ? null : NWAV_FORMATO_FECHA.format(nwavFecha));
            }
        });
        nwavColOperacion.setCellValueFactory(nwavFila -> new SimpleStringProperty(
                nwavFila.getValue().getNwavTipo() == null ? "" : nwavFila.getValue().getNwavTipo().getNwavDescripcion()));
        nwavColOrigen.setCellValueFactory(nwavFila -> new SimpleStringProperty(nwavFila.getValue().getNwavArchivoOrigen()));
        nwavColOrigen.setCellFactory(nwavColumna -> new NwavCeldaRuta());
        nwavColDestino.setCellValueFactory(nwavFila -> new SimpleStringProperty(nwavFila.getValue().getNwavArchivoDestino()));
        nwavColDestino.setCellFactory(nwavColumna -> new NwavCeldaRuta());
        nwavColResultado.setCellValueFactory(nwavFila -> new SimpleObjectProperty<>(nwavFila.getValue().getNwavResultado()));
        nwavColResultado.setCellFactory(nwavColumna -> new TableCell<>() {
            private final Label nwavChip = new Label();

            @Override
            protected void updateItem(NwavResultadoOperacion nwavResultado, boolean nwavVacia) {
                super.updateItem(nwavResultado, nwavVacia);
                if (nwavVacia || nwavResultado == null) {
                    setGraphic(null);
                    return;
                }
                boolean nwavBien = nwavResultado == NwavResultadoOperacion.NWAV_EXITO;
                nwavChip.setText(nwavBien ? "Correcta" : "Error");
                nwavChip.getStyleClass().setAll("label", "nwav-chip", nwavBien ? "nwav-chip-exito" : "nwav-chip-peligro");
                setGraphic(nwavChip);
                setAlignment(Pos.CENTER_LEFT);
            }
        });
        nwavColDetalle.setCellValueFactory(nwavFila -> new SimpleStringProperty(nwavFila.getValue().getNwavDetalle()));
        nwavColDetalle.setCellFactory(nwavColumna -> new TableCell<>() {
            @Override
            protected void updateItem(String nwavTexto, boolean nwavVacia) {
                super.updateItem(nwavTexto, nwavVacia);
                setText(nwavVacia ? null : nwavTexto);
                setTooltip(nwavVacia || nwavTexto == null ? null : new Tooltip(nwavTexto));
            }
        });

        nwavTabla.setRowFactory(nwavTablaFilas -> {
            TableRow<NwavHistorialOperacion> nwavFila = new TableRow<>();
            nwavFila.setOnMouseClicked(nwavEvento -> {
                if (nwavEvento.getButton() == MouseButton.PRIMARY && nwavEvento.getClickCount() == 2
                        && !nwavFila.isEmpty()) {
                    nwavAbrirDestino(nwavFila.getItem());
                }
            });
            return nwavFila;
        });

        nwavCmbTipo.getItems().add(NWAV_TODAS);
        for (NwavTipoOperacion nwavTipo : NwavTipoOperacion.values()) {
            nwavCmbTipo.getItems().add(nwavTipo.getNwavDescripcion());
        }
        nwavCmbTipo.setValue(NWAV_TODAS);
        nwavCmbResultado.getItems().setAll(NWAV_TODOS, NWAV_EXITO, NWAV_ERROR);
        nwavCmbResultado.setValue(NWAV_TODOS);
        nwavCmbTipo.valueProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavFiltrar());
        nwavCmbResultado.valueProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavFiltrar());
        nwavTxtBuscar.textProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavFiltrar());
    }

    @Override
    public void nwavRefrescar() {
        nwavTareaFondo.nwavEjecutar("Cargando historial…", nwavHistorial::nwavListar, nwavLista -> {
            nwavTodas.setAll(nwavLista);
            nwavFiltrar();
        }, null);
    }

    @FXML
    private void nwavOnActualizar() {
        nwavRefrescar();
    }

    @FXML
    private void nwavOnLimpiar() {
        nwavNotificador.nwavConfirmar("Vaciar historial",
                "Se borrarán todos los registros de operaciones. Los archivos PDF no se modifican.",
                "Vaciar historial", () -> nwavTareaFondo.nwavEjecutar("Vaciando historial…",
                        nwavHistorial::nwavLimpiar, nwavCantidad -> {
                            nwavNotificador.nwavExito("Se borraron " + nwavCantidad + " registros.");
                            nwavRefrescar();
                        }, null));
    }

    private void nwavFiltrar() {
        String nwavTipo = nwavCmbTipo.getValue();
        String nwavResultado = nwavCmbResultado.getValue();
        String nwavTexto = nwavTxtBuscar.getText() == null ? "" : nwavTxtBuscar.getText().trim().toLowerCase(Locale.ROOT);
        nwavFiltradas.setPredicate(nwavFila -> {
            boolean nwavTipoOk = NWAV_TODAS.equals(nwavTipo) || nwavFila.getNwavTipo() != null
                    && nwavFila.getNwavTipo().getNwavDescripcion().equals(nwavTipo);
            boolean nwavResultadoOk = NWAV_TODOS.equals(nwavResultado)
                    || NWAV_EXITO.equals(nwavResultado) && nwavFila.getNwavResultado() == NwavResultadoOperacion.NWAV_EXITO
                    || NWAV_ERROR.equals(nwavResultado) && nwavFila.getNwavResultado() == NwavResultadoOperacion.NWAV_ERROR;
            boolean nwavTextoOk = nwavTexto.isEmpty() || List.of(
                            Objects.toString(nwavFila.getNwavArchivoOrigen(), ""),
                            Objects.toString(nwavFila.getNwavArchivoDestino(), ""),
                            Objects.toString(nwavFila.getNwavDetalle(), "")).stream()
                    .anyMatch(nwavCampo -> nwavCampo.toLowerCase(Locale.ROOT).contains(nwavTexto));
            return nwavTipoOk && nwavResultadoOk && nwavTextoOk;
        });
        long nwavErrores = nwavTodas.stream()
                .filter(nwavFila -> nwavFila.getNwavResultado() == NwavResultadoOperacion.NWAV_ERROR).count();
        nwavLblResumen.setText(nwavFiltradas.size() + " de " + nwavTodas.size() + " registros"
                + (nwavErrores > 0 ? " · " + nwavErrores + " con error" : ""));
    }

    /** Doble clic: abre el PDF de destino si existe. */
    private void nwavAbrirDestino(NwavHistorialOperacion nwavFila) {
        String nwavDestino = nwavFila.getNwavArchivoDestino();
        if (nwavDestino == null || !nwavDestino.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return;
        }
        Path nwavRuta = Path.of(nwavDestino);
        if (Files.isRegularFile(nwavRuta)) {
            nwavMain.nwavAbrirArchivo(nwavRuta);
        } else {
            nwavNotificador.nwavAdvertencia("«" + nwavRuta.getFileName() + "» ya no existe.");
        }
    }

    private VBox nwavCrearVacio() {
        FontIcon nwavIcono = new FontIcon(Feather.LIST);
        nwavIcono.getStyleClass().add("nwav-vacio-icono");
        Label nwavTitulo = new Label("Sin operaciones registradas");
        nwavTitulo.getStyleClass().add("nwav-vacio-titulo");
        Label nwavDetalle = new Label("Aquí aparecerán las rotaciones, uniones, extracciones y demás operaciones.");
        nwavDetalle.getStyleClass().add("nwav-vacio-detalle");
        nwavDetalle.setWrapText(true);
        VBox nwavVacio = new VBox(8, nwavIcono, nwavTitulo, nwavDetalle);
        nwavVacio.setAlignment(Pos.CENTER);
        nwavVacio.setMaxWidth(380);
        return nwavVacio;
    }

    /** Celda de texto largo: muestra el nombre del archivo y la ruta completa en un tooltip. */
    private static final class NwavCeldaRuta extends TableCell<NwavHistorialOperacion, String> {
        @Override
        protected void updateItem(String nwavTexto, boolean nwavVacia) {
            super.updateItem(nwavTexto, nwavVacia);
            if (nwavVacia || nwavTexto == null || nwavTexto.isBlank()) {
                setText(nwavVacia ? null : "—");
                setTooltip(null);
                return;
            }
            setText(nwavResumir(nwavTexto));
            setTooltip(new Tooltip(nwavTexto.replace("; ", "\n")));
        }

        private static String nwavResumir(String nwavTexto) {
            if (nwavTexto.contains("; ")) {
                String[] nwavPartes = nwavTexto.split("; ");
                return nwavNombre(nwavPartes[0]) + " + " + (nwavPartes.length - 1) + " más";
            }
            return nwavNombre(nwavTexto);
        }

        private static String nwavNombre(String nwavTexto) {
            if (!nwavTexto.contains("/") && !nwavTexto.contains("\\")) {
                return nwavTexto;
            }
            try {
                Path nwavRuta = Path.of(nwavTexto);
                return nwavRuta.getFileName() == null ? nwavTexto : nwavRuta.getFileName().toString();
            } catch (RuntimeException nwavError) {
                return nwavTexto;
            }
        }
    }
}
