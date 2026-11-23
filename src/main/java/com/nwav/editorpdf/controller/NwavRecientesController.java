package com.nwav.editorpdf.controller;

import atlantafx.base.theme.Styles;
import com.nwav.editorpdf.model.NwavArchivoReciente;
import com.nwav.editorpdf.model.NwavModoZoom;
import com.nwav.editorpdf.service.NwavArchivoRecienteService;
import com.nwav.editorpdf.service.NwavArchivoRecienteService.NwavRecienteVista;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavRefrescable;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class NwavRecientesController implements NwavRefrescable {
    private static final DateTimeFormatter NWAV_FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NwavArchivoRecienteService nwavRecientes;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavNotificador nwavNotificador;
    private final NwavMainController nwavMain;

    @FXML private ListView<NwavRecienteVista> nwavListaRecientes;
    @FXML private TextField nwavTxtBuscar;
    @FXML private Label nwavLblResumen;
    @FXML private Button nwavBtnQuitarNoDisponibles;

    private final ObservableList<NwavRecienteVista> nwavTodos = FXCollections.observableArrayList();
    private FilteredList<NwavRecienteVista> nwavFiltrados;

    @FXML
    public void initialize() {
        nwavFiltrados = new FilteredList<>(nwavTodos, nwavVista -> true);
        nwavListaRecientes.setItems(nwavFiltrados);
        nwavListaRecientes.setCellFactory(nwavLista -> new NwavCeldaReciente());
        nwavListaRecientes.setPlaceholder(nwavCrearVacio());
        nwavTxtBuscar.textProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> {
            String nwavFiltro = nwavAhora == null ? "" : nwavAhora.trim().toLowerCase(Locale.ROOT);
            nwavFiltrados.setPredicate(nwavVista -> nwavFiltro.isEmpty()
                    || nwavVista.nwavArchivo().getNwavRuta().toLowerCase(Locale.ROOT).contains(nwavFiltro));
        });
    }

    @Override
    public void nwavRefrescar() {
        nwavTareaFondo.nwavEjecutar("Cargando archivos recientes…", nwavRecientes::nwavListar,
                this::nwavMostrarLista, null);
    }

    private void nwavMostrarLista(List<NwavRecienteVista> nwavLista) {
        nwavTodos.setAll(nwavLista);
        long nwavNoDisponibles = nwavLista.stream().filter(nwavVista -> !nwavVista.nwavDisponible()).count();
        nwavLblResumen.setText(nwavLista.size() + (nwavLista.size() == 1 ? " archivo" : " archivos")
                + (nwavNoDisponibles > 0 ? " · " + nwavNoDisponibles + " no disponible"
                + (nwavNoDisponibles == 1 ? "" : "s") : ""));
        nwavBtnQuitarNoDisponibles.setVisible(nwavNoDisponibles > 0);
        nwavBtnQuitarNoDisponibles.setManaged(nwavNoDisponibles > 0);
    }

    private void nwavAbrir(NwavRecienteVista nwavVista) {
        if (!nwavVista.nwavDisponible()) {
            nwavNotificador.nwavAdvertencia("«" + nwavVista.nwavArchivo().getNwavNombre()
                    + "» ya no está en su ubicación. Puedes quitarlo de la lista.");
            return;
        }
        nwavMain.nwavAbrirArchivo(Path.of(nwavVista.nwavArchivo().getNwavRuta()));
    }

    private void nwavQuitar(NwavRecienteVista nwavVista) {
        Long nwavId = nwavVista.nwavArchivo().getNwavId();
        nwavTareaFondo.nwavEjecutar("Quitando de recientes…", () -> {
            nwavRecientes.nwavQuitar(nwavId);
            return null;
        }, nwavNada -> {
            nwavTodos.remove(nwavVista);
            nwavMostrarLista(List.copyOf(nwavTodos));
            nwavMain.nwavActualizarMenuRecientes();
        }, null);
    }

    @FXML
    private void nwavOnQuitarNoDisponibles() {
        nwavNotificador.nwavConfirmar("Quitar archivos no disponibles",
                "Se quitarán de la lista los archivos que ya no existen en disco. Los archivos no se modifican.",
                "Quitar", () -> nwavTareaFondo.nwavEjecutar("Limpiando recientes…",
                        nwavRecientes::nwavQuitarNoDisponibles, nwavCantidad -> {
                            nwavNotificador.nwavExito(nwavCantidad == 1
                                    ? "Se quitó 1 archivo no disponible."
                                    : "Se quitaron " + nwavCantidad + " archivos no disponibles.");
                            nwavRefrescar();
                            nwavMain.nwavActualizarMenuRecientes();
                        }, null));
    }

    @FXML
    private void nwavOnAbrirOtro() {
        nwavMain.nwavIrA(NwavMainController.NWAV_VISTA_VISOR);
        nwavMain.nwavAbrirConDialogo();
    }

    private VBox nwavCrearVacio() {
        FontIcon nwavIcono = new FontIcon(Feather.CLOCK);
        nwavIcono.getStyleClass().add("nwav-vacio-icono");
        Label nwavTitulo = new Label("Todavía no hay archivos recientes");
        nwavTitulo.getStyleClass().add("nwav-vacio-titulo");
        Label nwavDetalle = new Label("Los PDF que abras aparecerán aquí, con la página y el zoom en que los dejaste.");
        nwavDetalle.getStyleClass().add("nwav-vacio-detalle");
        nwavDetalle.setWrapText(true);
        VBox nwavVacio = new VBox(8, nwavIcono, nwavTitulo, nwavDetalle);
        nwavVacio.setAlignment(Pos.CENTER);
        nwavVacio.setMaxWidth(360);
        return nwavVacio;
    }

    static String nwavDescribirZoom(NwavArchivoReciente nwavArchivo) {
        return nwavArchivo.getNwavModoZoom() == NwavModoZoom.NWAV_PORCENTAJE
                ? Math.round(nwavArchivo.getNwavZoom() * 100) + " %"
                : nwavArchivo.getNwavModoZoom().getNwavDescripcion().toLowerCase(Locale.ROOT);
    }

    private final class NwavCeldaReciente extends ListCell<NwavRecienteVista> {
        private final FontIcon nwavIcono = new FontIcon();
        private final StackPane nwavFondoIcono = new StackPane(nwavIcono);
        private final Label nwavNombre = new Label();
        private final Label nwavChip = new Label("No disponible");
        private final Label nwavRuta = new Label();
        private final Label nwavDetalle = new Label();
        private final Button nwavBtnAbrir = new Button("Abrir");
        private final Button nwavBtnQuitar = new Button(null, new FontIcon(Feather.X));
        private final HBox nwavFila;

        private NwavCeldaReciente() {
            nwavFondoIcono.getStyleClass().add("nwav-reciente-icono");
            nwavNombre.getStyleClass().add("nwav-reciente-nombre");
            nwavChip.getStyleClass().addAll("nwav-chip", "nwav-chip-peligro");
            nwavRuta.getStyleClass().add("nwav-reciente-ruta");
            nwavDetalle.getStyleClass().add("nwav-reciente-detalle");
            nwavBtnAbrir.getStyleClass().addAll(Styles.ACCENT, Styles.SMALL);
            nwavBtnQuitar.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.SMALL);
            nwavBtnQuitar.setTooltip(new Tooltip("Quitar de la lista (no borra el archivo)"));

            HBox nwavCabecera = new HBox(8, nwavNombre, nwavChip);
            nwavCabecera.setAlignment(Pos.CENTER_LEFT);
            VBox nwavTextos = new VBox(3, nwavCabecera, nwavRuta, nwavDetalle);
            nwavTextos.setMinWidth(0);
            HBox.setHgrow(nwavTextos, Priority.ALWAYS);
            Region nwavEspacio = new Region();
            HBox.setHgrow(nwavEspacio, Priority.SOMETIMES);
            nwavFila = new HBox(14, nwavFondoIcono, nwavTextos, nwavEspacio, nwavBtnAbrir, nwavBtnQuitar);
            nwavFila.setAlignment(Pos.CENTER_LEFT);
            nwavFila.getStyleClass().add("nwav-reciente");

            nwavBtnAbrir.setOnAction(nwavEvento -> nwavAbrir(getItem()));
            nwavBtnQuitar.setOnAction(nwavEvento -> nwavQuitar(getItem()));
            setOnMouseClicked(nwavEvento -> {
                if (nwavEvento.getButton() == MouseButton.PRIMARY && nwavEvento.getClickCount() == 2
                        && getItem() != null) {
                    nwavAbrir(getItem());
                }
            });
        }

        @Override
        protected void updateItem(NwavRecienteVista nwavVista, boolean nwavVacia) {
            super.updateItem(nwavVista, nwavVacia);
            if (nwavVacia || nwavVista == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            NwavArchivoReciente nwavArchivo = nwavVista.nwavArchivo();
            boolean nwavDisponible = nwavVista.nwavDisponible();
            nwavIcono.setIconCode(nwavDisponible ? Feather.FILE_TEXT : Feather.FILE_MINUS);
            nwavNombre.setText(nwavArchivo.getNwavNombre());
            nwavChip.setVisible(!nwavDisponible);
            nwavChip.setManaged(!nwavDisponible);
            nwavRuta.setText(nwavArchivo.getNwavRuta());
            nwavDetalle.setText("Abierto el " + NWAV_FORMATO_FECHA.format(nwavArchivo.getNwavUltimaApertura())
                    + "  ·  Página " + (nwavArchivo.getNwavUltimaPagina() + 1)
                    + "  ·  Zoom " + nwavDescribirZoom(nwavArchivo));
            nwavBtnAbrir.setDisable(!nwavDisponible);
            nwavFila.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("nwav-no-disponible"),
                    !nwavDisponible);
            setText(null);
            setGraphic(nwavFila);
        }
    }
}
