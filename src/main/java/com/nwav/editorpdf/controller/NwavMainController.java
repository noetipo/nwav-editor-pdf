package com.nwav.editorpdf.controller;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.util.Animations;
import com.nwav.editorpdf.service.NwavArchivoRecienteService;
import com.nwav.editorpdf.service.NwavArchivoRecienteService.NwavRecienteVista;
import com.nwav.editorpdf.service.NwavConfiguracionService;
import com.nwav.editorpdf.ui.NwavCargadorVistas;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavRefrescable;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class NwavMainController {
    public static final String NWAV_TITULO_APP = "Editor PDF";
    public static final String NWAV_VISTA_INICIO = "nwav-inicio-view";
    public static final String NWAV_VISTA_HERRAMIENTA = "nwav-herramienta-view";
    public static final String NWAV_VISTA_VISOR = "nwav-visor-view";
    public static final String NWAV_VISTA_RECIENTES = "nwav-recientes-view";
    public static final String NWAV_VISTA_PREFERENCIAS = "nwav-preferencias-view";
    public static final String NWAV_VISTA_HISTORIAL = "nwav-historial-view";

    private static final double NWAV_ANCHO_EXPANDIDO = 232;
    private static final double NWAV_ANCHO_CONTRAIDO = 76;
    private static final Duration NWAV_DURACION_MENU = Duration.millis(260);
    private static final int NWAV_RECIENTES_EN_MENU = 10;

    private final NwavCargadorVistas nwavCargador;
    private final NwavNotificador nwavNotificador;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavConfiguracionService nwavConfiguracion;
    private final NwavArchivoRecienteService nwavRecientes;
    private final NwavVisorController nwavVisor;

    @FXML private StackPane nwavRaiz;
    @FXML private VBox nwavSidebar;
    @FXML private StackPane nwavContenido;
    @FXML private ModalPane nwavModalPanel;
    @FXML private ModalPane nwavModalDialogo;
    @FXML private VBox nwavZonaNotificaciones;
    @FXML private StackPane nwavCapaArrastre;

    @FXML private ToggleGroup nwavMenu;
    @FXML private ToggleButton nwavNavInicio;
    @FXML private ToggleButton nwavNavVisor;
    @FXML private ToggleButton nwavNavRecientes;
    @FXML private ToggleButton nwavNavPreferencias;
    @FXML private ToggleButton nwavNavHistorial;
    @FXML private Region nwavIndicador;
    @FXML private VBox nwavMarcaTextos;
    @FXML private Button nwavBtnContraer;

    @FXML private MenuBar nwavBarraMenus;
    @FXML private Menu nwavMnuRecientes;
    @FXML private MenuItem nwavMnuCerrar;
    @FXML private MenuItem nwavMnuGuardar;
    @FXML private MenuItem nwavMnuGuardarComo;
    @FXML private MenuItem nwavMnuDeshacer;
    @FXML private MenuItem nwavMnuRehacer;
    @FXML private Menu nwavMnuPaginas;
    @FXML private Menu nwavMnuVer;
    @FXML private Menu nwavMnuHerramientas;
    @FXML private CheckMenuItem nwavMnuMiniaturas;

    @FXML private Label nwavLblEstadoDocumento;
    @FXML private HBox nwavZonaProgreso;
    @FXML private ProgressBar nwavBarraProgreso;
    @FXML private Label nwavLblProgreso;
    @FXML private Button nwavBtnCancelarTarea;

    private final BooleanProperty nwavVistaVisor = new SimpleBooleanProperty(false);
    private boolean nwavOscuro;
    private boolean nwavContraido;
    private boolean nwavMostrandoHerramienta;
    private boolean nwavVentanaEstrecha;
    private boolean nwavContraidoPorAncho;

    @FXML
    public void initialize() {
        nwavNotificador.nwavRegistrar(nwavModalPanel, nwavModalDialogo, nwavZonaNotificaciones);
        nwavTareaFondo.nwavRegistrar(nwavZonaProgreso, nwavBarraProgreso, nwavLblProgreso);
        nwavModalPanel.setAlignment(Pos.TOP_RIGHT);
        nwavModalDialogo.setAlignment(Pos.CENTER);

        nwavNavInicio.setUserData(NWAV_VISTA_INICIO);
        nwavNavVisor.setUserData(NWAV_VISTA_VISOR);
        nwavNavRecientes.setUserData(NWAV_VISTA_RECIENTES);
        nwavNavPreferencias.setUserData(NWAV_VISTA_PREFERENCIAS);
        nwavNavHistorial.setUserData(NWAV_VISTA_HISTORIAL);
        for (ToggleButton nwavBoton : nwavBotonesMenu()) {
            Tooltip nwavAyuda = new Tooltip(nwavBoton.getText());
            nwavAyuda.setShowDelay(Duration.millis(300));
            nwavBoton.setTooltip(nwavAyuda);
            nwavBoton.boundsInParentProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> {
                if (nwavMenu.getSelectedToggle() == nwavBoton) {
                    nwavIndicador.setTranslateY(nwavAhora.getMinY());
                    nwavIndicador.setPrefHeight(nwavAhora.getHeight());
                }
            });
        }

        nwavVisor.nwavConfigurarAcciones(
                () -> nwavIrA(NWAV_VISTA_RECIENTES), this::nwavActualizarMenuRecientes, this::nwavVolverAlInicio);

        nwavMenu.selectedToggleProperty().addListener((nwavObs, nwavAnterior, nwavActual) -> {
            if (nwavActual == null) {
                nwavMenu.selectToggle(nwavAnterior);
                return;
            }
            if (nwavMostrandoHerramienta) {
                return;
            }
            nwavMostrar((String) nwavActual.getUserData());
            nwavMoverIndicador(nwavActual);
        });
        nwavMenu.selectToggle(nwavNavVisor);
        nwavMenu.selectToggle(nwavNavInicio);
        Platform.runLater(() -> nwavMoverIndicador(nwavNavInicio));
        nwavNavInicio.setOnMouseClicked(nwavEvento -> {
            if (nwavMenu.getSelectedToggle() == nwavNavInicio) {
                nwavMostrar(NWAV_VISTA_INICIO);
            }
        });
        nwavBtnCancelarTarea.visibleProperty().bind(nwavTareaFondo.nwavCancelableProperty().isNotNull());
        nwavBtnCancelarTarea.managedProperty().bind(nwavBtnCancelarTarea.visibleProperty());
        nwavBtnCancelarTarea.setOnAction(nwavEvento -> nwavTareaFondo.nwavCancelar());

        nwavOscuro = nwavConfiguracion.nwavTemaOscuro();
        nwavAplicarTema(nwavOscuro, false);

        nwavLblEstadoDocumento.textProperty().bind(nwavVisor.nwavEstadoProperty());
        nwavMnuCerrar.disableProperty().bind(nwavVisor.nwavHayDocumentoProperty().not());
        nwavMnuGuardar.disableProperty().bind(nwavVisor.nwavPuedeEditarProperty().not()
                .or(nwavVisor.nwavSinGuardarProperty().not()));
        nwavMnuGuardarComo.disableProperty().bind(nwavVisor.nwavPuedeEditarProperty().not());
        nwavMnuDeshacer.disableProperty().bind(nwavVisor.nwavPuedeEditarProperty().not()
                .or(nwavVisor.nwavPuedeDeshacerProperty().not()));
        nwavMnuDeshacer.textProperty().bind(nwavVisor.nwavTextoDeshacerProperty());
        nwavMnuRehacer.disableProperty().bind(nwavVisor.nwavPuedeEditarProperty().not()
                .or(nwavVisor.nwavPuedeRehacerProperty().not()));
        nwavMnuRehacer.textProperty().bind(nwavVisor.nwavTextoRehacerProperty());
        nwavMnuPaginas.getItems().stream()
                .filter(nwavItem -> !(nwavItem instanceof SeparatorMenuItem) && !"nwavMnuUnir".equals(nwavItem.getId()))
                .forEach(nwavItem -> nwavItem.disableProperty().bind(nwavVisor.nwavPuedeEditarProperty().not()));
        nwavMnuVer.getItems().stream()
                .filter(nwavItem -> nwavItem != nwavMnuMiniaturas && !(nwavItem instanceof SeparatorMenuItem))
                .forEach(nwavItem -> nwavItem.disableProperty().bind(nwavVisor.nwavHayDocumentoProperty().not()));
        nwavMnuHerramientas.getItems().stream()
                .filter(nwavItem -> !(nwavItem instanceof SeparatorMenuItem))
                .forEach(nwavItem -> nwavItem.disableProperty().bind(nwavVisor.nwavPuedeEditarProperty().not()));
        nwavMnuMiniaturas.selectedProperty().bindBidirectional(nwavVisor.nwavMiniaturasVisiblesProperty());
        nwavBarraMenus.visibleProperty().bind(nwavVistaVisor.and(nwavVisor.nwavHayDocumentoProperty()));
        nwavBarraMenus.managedProperty().bind(nwavBarraMenus.visibleProperty());

        nwavConfigurarArrastrarSoltar();
        nwavRaiz.sceneProperty().addListener((nwavObs, nwavAntes, nwavEscena) -> {
            if (nwavEscena != null) {
                nwavConfigurarAtajosExtra(nwavEscena);
                nwavEscucharAncho(nwavEscena);
            }
        });
        nwavActualizarMenuRecientes();
    }

    public StringExpression nwavTituloVentana() {
        return Bindings.createStringBinding(() -> {
            String nwavDocumento = nwavVisor.nwavTituloProperty().get();
            return nwavDocumento == null || nwavDocumento.isBlank()
                    ? NWAV_TITULO_APP
                    : nwavDocumento + " — " + NWAV_TITULO_APP;
        }, nwavVisor.nwavTituloProperty());
    }

    public void nwavIrA(String nwavNombreVista) {
        nwavBotonesMenu().stream()
                .filter(nwavBoton -> nwavNombreVista.equals(nwavBoton.getUserData()))
                .findFirst()
                .ifPresent(nwavMenu::selectToggle);
    }

    public void nwavVolverAlInicio() {
        nwavSeleccionarSinMostrar(nwavNavInicio);
        nwavMostrar(NWAV_VISTA_INICIO);
    }

    public void nwavAbrirHerramienta(com.nwav.editorpdf.ui.NwavDefinicionHerramienta nwavDefinicion,
                                     List<Path> nwavArchivos) {
        switch (nwavDefinicion.getNwavModo()) {
            case NWAV_LOTE -> {
                nwavVistaVisor.set(false);
                nwavSeleccionarSinMostrar(nwavNavInicio);
                NwavCargadorVistas.NwavVistaCargada nwavVista = nwavCargador.nwavObtener(NWAV_VISTA_HERRAMIENTA);
                ((NwavHerramientaController) nwavVista.nwavControlador()).nwavPreparar(nwavDefinicion, nwavArchivos);
                nwavContenido.getChildren().setAll(nwavVista.nwavNodo());
                Animations.fadeIn(nwavVista.nwavNodo(), Duration.millis(220)).play();
            }
            case NWAV_VISTA -> {
                nwavVistaVisor.set(false);
                nwavSeleccionarSinMostrar(nwavNavInicio);
                NwavCargadorVistas.NwavVistaCargada nwavVista = nwavCargador.nwavObtener(nwavDefinicion.getNwavVista());
                if (nwavVista.nwavControlador() instanceof com.nwav.editorpdf.ui.NwavVistaHerramienta nwavPropia) {
                    nwavPropia.nwavPreparar(nwavDefinicion, nwavArchivos);
                }
                nwavContenido.getChildren().setAll(nwavVista.nwavNodo());
            }
            case NWAV_EDITOR -> {
                Runnable nwavAccion = nwavDefinicion.getNwavAccionEditor();
                if (!nwavDefinicion.isNwavNecesitaDocumento() || nwavVisor.nwavDocumentoActual().isPresent()
                        && nwavArchivos.isEmpty()) {
                    nwavIrA(NWAV_VISTA_VISOR);
                    nwavAccion.run();
                    return;
                }
                Path nwavArchivo = nwavArchivos.isEmpty() ? nwavElegirPdf(nwavDefinicion.getNwavNombre()) : nwavArchivos.get(0);
                if (nwavArchivo != null) {
                    nwavIrA(NWAV_VISTA_VISOR);
                    nwavVisor.nwavAbrirYEjecutar(nwavArchivo, nwavAccion);
                }
            }
        }
    }

    private void nwavSeleccionarSinMostrar(ToggleButton nwavBoton) {
        if (nwavMenu.getSelectedToggle() != nwavBoton) {
            nwavMostrandoHerramienta = true;
            nwavMenu.selectToggle(nwavBoton);
            nwavMostrandoHerramienta = false;
        }
        Platform.runLater(() -> nwavMoverIndicador(nwavBoton));
    }

    private Path nwavElegirPdf(String nwavTitulo) {
        javafx.stage.FileChooser nwavSelector = new javafx.stage.FileChooser();
        nwavSelector.setTitle(nwavTitulo + ": elige un PDF");
        nwavSelector.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Documentos PDF", "*.pdf", "*.PDF"));
        nwavConfiguracion.nwavCarpetaInicial().map(Path::toFile).ifPresent(nwavSelector::setInitialDirectory);
        File nwavElegido = nwavSelector.showOpenDialog(nwavRaiz.getScene() == null ? null : nwavRaiz.getScene().getWindow());
        return nwavElegido == null ? null : nwavElegido.toPath();
    }

    public void nwavAbrirArchivo(Path nwavRuta) {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavAbrirArchivo(nwavRuta);
    }

    private void nwavMostrar(String nwavNombre) {
        nwavVistaVisor.set(NWAV_VISTA_VISOR.equals(nwavNombre));
        NwavCargadorVistas.NwavVistaCargada nwavVista = nwavCargador.nwavObtener(nwavNombre);
        nwavContenido.getChildren().setAll(nwavVista.nwavNodo());
        if (nwavVista.nwavControlador() instanceof NwavRefrescable nwavRefrescable) {
            nwavRefrescable.nwavRefrescar();
        }
        Animations.fadeIn(nwavVista.nwavNodo(), Duration.millis(260)).play();
    }

    private void nwavMoverIndicador(Toggle nwavToggle) {
        Node nwavBoton = (Node) nwavToggle;
        double nwavDestino = nwavBoton.getBoundsInParent().getMinY();
        nwavIndicador.setPrefHeight(nwavBoton.getBoundsInParent().getHeight());
        new Timeline(new KeyFrame(Duration.millis(280),
                new KeyValue(nwavIndicador.translateYProperty(), nwavDestino, Interpolator.SPLINE(0.4, 0, 0.2, 1))))
                .play();
    }

    @FXML
    private void nwavOnAbrir() {
        nwavAbrirConDialogo();
    }

    public void nwavAbrirConDialogo() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnAbrir();
    }

    @FXML
    private void nwavOnCerrarDocumento() {
        nwavVisor.nwavOnCerrar();
    }

    @FXML
    private void nwavOnSalir() {
        nwavSalir();
    }

    public void nwavSalir() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavConfirmarSiHayCambios(() -> {
            nwavVisor.nwavDescartarCambios();
            Platform.exit();
        });
    }

    @FXML
    private void nwavOnGuardar() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnGuardar();
    }

    @FXML
    private void nwavOnGuardarComo() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnGuardarComo();
    }

    @FXML
    private void nwavOnDeshacer() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnDeshacer();
    }

    @FXML
    private void nwavOnRehacer() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnRehacer();
    }

    @FXML
    private void nwavOnRotarIzquierda() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavRotarSeleccion(-90);
    }

    @FXML
    private void nwavOnRotarDerecha() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavRotarSeleccion(90);
    }

    @FXML
    private void nwavOnGirar180() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavRotarSeleccion(180);
    }

    @FXML
    private void nwavOnDuplicar() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavDuplicarSeleccion();
    }

    @FXML
    private void nwavOnEliminar() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavEliminarSeleccion();
    }

    @FXML
    private void nwavOnInsertar() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnInsertar();
    }

    @FXML
    private void nwavOnExtraer() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnExtraer();
    }

    @FXML
    private void nwavOnDividir() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnDividir();
    }

    @FXML
    private void nwavOnUnir() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnUnir();
    }

    @FXML
    private void nwavOnAcercar() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavAcercar();
    }

    @FXML
    private void nwavOnAlejar() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavAlejar();
    }

    @FXML
    private void nwavOnTamanoReal() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavTamanoReal();
    }

    @FXML
    private void nwavOnAjustarAncho() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavAjustarAncho();
    }

    @FXML
    private void nwavOnAjustarPagina() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavAjustarPagina();
    }

    @FXML
    private void nwavOnPrimeraPagina() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavIrAPrimera();
    }

    @FXML
    private void nwavOnUltimaPagina() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavIrAUltima();
    }

    @FXML
    private void nwavOnPaginaAnterior() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnPaginaAnterior();
    }

    @FXML
    private void nwavOnPaginaSiguiente() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnPaginaSiguiente();
    }

    @FXML
    private void nwavOnBuscar() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnBuscar();
    }

    @FXML
    private void nwavOnFormulario() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnFormulario();
    }

    @FXML
    private void nwavOnFirma() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnFirma();
    }

    @FXML
    private void nwavOnMarcaAgua() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnMarcaAgua();
    }

    @FXML
    private void nwavOnNumerar() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnNumerar();
    }

    @FXML
    private void nwavOnEncabezado() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnEncabezado();
    }

    @FXML
    private void nwavOnProteccion() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnProteccion();
    }

    @FXML
    private void nwavOnMetadatos() {
        nwavIrA(NWAV_VISTA_VISOR);
        nwavVisor.nwavOnMetadatos();
    }

    public void nwavActualizarMenuRecientes() {
        nwavTareaFondo.nwavEjecutarSilencioso(nwavRecientes::nwavListar, this::nwavLlenarMenuRecientes);
    }

    private void nwavLlenarMenuRecientes(List<NwavRecienteVista> nwavLista) {
        List<MenuItem> nwavItems = new ArrayList<>();
        for (NwavRecienteVista nwavVista : nwavLista.stream().limit(NWAV_RECIENTES_EN_MENU).toList()) {
            String nwavNombre = nwavVista.nwavArchivo().getNwavNombre();
            MenuItem nwavItem = new MenuItem(nwavVista.nwavDisponible() ? nwavNombre : nwavNombre + "  (no disponible)");
            nwavItem.setDisable(!nwavVista.nwavDisponible());
            Path nwavRuta = Path.of(nwavVista.nwavArchivo().getNwavRuta());
            nwavItem.setOnAction(nwavEvento -> nwavAbrirArchivo(nwavRuta));
            nwavItems.add(nwavItem);
        }
        if (nwavItems.isEmpty()) {
            MenuItem nwavVacio = new MenuItem("Sin archivos recientes");
            nwavVacio.setDisable(true);
            nwavItems.add(nwavVacio);
        }
        nwavItems.add(new SeparatorMenuItem());
        MenuItem nwavVerTodos = new MenuItem("Ver todos los recientes…");
        nwavVerTodos.setOnAction(nwavEvento -> nwavIrA(NWAV_VISTA_RECIENTES));
        nwavItems.add(nwavVerTodos);
        nwavMnuRecientes.getItems().setAll(nwavItems);
    }

    private void nwavConfigurarArrastrarSoltar() {
        nwavCapaArrastre.setVisible(false);
        nwavRaiz.addEventFilter(DragEvent.DRAG_OVER, nwavEvento -> {
            com.nwav.editorpdf.ui.NwavReceptorArchivos nwavReceptor = nwavReceptorActual();
            if (nwavReceptor != null && nwavEvento.getDragboard().hasFiles()
                    && nwavEvento.getDragboard().getFiles().stream().anyMatch(nwavReceptor::nwavAcepta)) {
                nwavEvento.acceptTransferModes(TransferMode.COPY);
                nwavCapaArrastre.setVisible(true);
                return;
            }
            if (!nwavPdfsArrastrados(nwavEvento.getDragboard()).isEmpty()) {
                nwavEvento.acceptTransferModes(TransferMode.COPY);
                nwavCapaArrastre.setVisible(true);
            }
        });
        nwavRaiz.addEventFilter(DragEvent.DRAG_EXITED, nwavEvento -> nwavCapaArrastre.setVisible(false));
        nwavRaiz.addEventFilter(DragEvent.DRAG_DROPPED, nwavEvento -> {
            nwavCapaArrastre.setVisible(false);
            if (!nwavEvento.getDragboard().hasFiles()) {
                return;
            }
            com.nwav.editorpdf.ui.NwavReceptorArchivos nwavReceptor = nwavReceptorActual();
            if (nwavReceptor != null) {
                nwavReceptor.nwavRecibir(nwavEvento.getDragboard().getFiles());
                nwavEvento.setDropCompleted(true);
                nwavEvento.consume();
                return;
            }
            List<File> nwavPdfs = nwavPdfsArrastrados(nwavEvento.getDragboard());
            if (nwavPdfs.isEmpty()) {
                nwavNotificador.nwavAdvertencia("Solo se pueden abrir archivos PDF.");
                nwavEvento.setDropCompleted(false);
            } else {
                nwavAbrirArchivo(nwavPdfs.get(0).toPath());
                if (nwavPdfs.size() > 1) {
                    nwavNotificador.nwavInfo("Se soltaron " + nwavPdfs.size() + " archivos; se abrió el primero.");
                }
                nwavEvento.setDropCompleted(true);
            }
            nwavEvento.consume();
        });
    }

    private com.nwav.editorpdf.ui.NwavReceptorArchivos nwavReceptorActual() {
        if (nwavContenido.getChildren().isEmpty()) {
            return null;
        }
        Node nwavActual = nwavContenido.getChildren().get(0);
        NwavCargadorVistas.NwavVistaCargada nwavHerramienta = nwavCargador.nwavObtener(NWAV_VISTA_HERRAMIENTA);
        if (nwavHerramienta.nwavNodo() == nwavActual
                && nwavHerramienta.nwavControlador() instanceof com.nwav.editorpdf.ui.NwavReceptorArchivos nwavReceptor) {
            return nwavReceptor;
        }
        return null;
    }

    private static List<File> nwavPdfsArrastrados(Dragboard nwavPortapapeles) {
        if (!nwavPortapapeles.hasFiles()) {
            return List.of();
        }
        return nwavPortapapeles.getFiles().stream()
                .filter(File::isFile)
                .filter(nwavArchivo -> nwavArchivo.getName().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                .toList();
    }

    private void nwavEscucharAncho(Scene nwavEscena) {
        if (nwavEscena.getWindow() != null) {
            nwavVigilarAncho(nwavEscena.getWindow());
        }
        nwavEscena.windowProperty().addListener((nwavObs, nwavAntes, nwavVentana) -> {
            if (nwavVentana != null) {
                nwavVigilarAncho(nwavVentana);
            }
        });
    }

    private void nwavVigilarAncho(javafx.stage.Window nwavVentana) {
        nwavVentana.widthProperty().addListener((nwavObs, nwavAntes, nwavAncho) ->
                nwavAjustarMenuAlAncho(nwavAncho.doubleValue()));
        nwavAjustarMenuAlAncho(nwavVentana.getWidth());
    }

    private void nwavAjustarMenuAlAncho(double nwavAncho) {
        boolean nwavEstrecha = nwavAncho > 0 && nwavAncho < 1000;
        if (nwavEstrecha == nwavVentanaEstrecha) {
            return;
        }
        boolean nwavEraEstrecha = nwavVentanaEstrecha;
        nwavVentanaEstrecha = nwavEstrecha;
        if (nwavEstrecha && !nwavEraEstrecha && !nwavContraido) {
            nwavContraidoPorAncho = true;
            nwavOnContraerMenu();
        } else if (!nwavEstrecha && nwavEraEstrecha && nwavContraidoPorAncho) {
            nwavContraidoPorAncho = false;
            if (nwavContraido) {
                nwavOnContraerMenu();
            }
        }
    }

    private void nwavConfigurarAtajosExtra(Scene nwavEscena) {
        nwavEscena.addEventFilter(KeyEvent.KEY_PRESSED, nwavEvento -> {
            if (!nwavEvento.isShortcutDown() || !nwavVisor.nwavHayDocumentoProperty().get()) {
                return;
            }
            KeyCode nwavTecla = nwavEvento.getCode();
            if (nwavTecla == KeyCode.ADD || nwavTecla == KeyCode.EQUALS) {
                nwavVisor.nwavAcercar();
                nwavEvento.consume();
            } else if (nwavTecla == KeyCode.SUBTRACT) {
                nwavVisor.nwavAlejar();
                nwavEvento.consume();
            }
        });
    }

    public void nwavAplicarTema(boolean nwavModoOscuro, boolean nwavGuardar) {
        nwavOscuro = nwavModoOscuro;
        Application.setUserAgentStylesheet(nwavOscuro
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
        nwavRaiz.getStyleClass().remove("nwav-oscuro");
        if (nwavOscuro) {
            nwavRaiz.getStyleClass().add("nwav-oscuro");
        }
        if (nwavGuardar) {
            nwavTareaFondo.nwavGuardarEnFondo(() -> nwavConfiguracion.nwavGuardarTemaOscuro(nwavModoOscuro));
        }
    }

    public boolean nwavEsOscuro() {
        return nwavOscuro;
    }

    @FXML
    private void nwavOnContraerMenu() {
        nwavContraido = !nwavContraido;
        double nwavAncho = nwavContraido ? NWAV_ANCHO_CONTRAIDO : NWAV_ANCHO_EXPANDIDO;

        Timeline nwavAjuste = new Timeline(new KeyFrame(NWAV_DURACION_MENU,
                new KeyValue(nwavSidebar.prefWidthProperty(), nwavAncho, Interpolator.SPLINE(0.4, 0, 0.2, 1)),
                new KeyValue(nwavSidebar.minWidthProperty(), nwavAncho, Interpolator.SPLINE(0.4, 0, 0.2, 1))));

        ContentDisplay nwavModo = nwavContraido ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT;
        List<Node> nwavTextos = new ArrayList<>(nwavSidebar.lookupAll(".nwav-nav-seccion"));
        nwavTextos.add(nwavMarcaTextos);

        if (nwavContraido) {
            nwavTextos.forEach(nwavNodo -> {
                nwavNodo.setVisible(false);
                nwavNodo.setManaged(false);
            });
            nwavTodosLosBotones().forEach(nwavBoton -> nwavBoton.setContentDisplay(nwavModo));
            nwavAjuste.play();
        } else {
            nwavAjuste.setOnFinished(nwavEvento -> {
                nwavTextos.forEach(nwavNodo -> {
                    nwavNodo.setVisible(true);
                    nwavNodo.setManaged(true);
                });
                nwavTodosLosBotones().forEach(nwavBoton -> nwavBoton.setContentDisplay(nwavModo));
                ParallelTransition nwavAparicion = new ParallelTransition();
                nwavTextos.forEach(nwavNodo ->
                        nwavAparicion.getChildren().add(Animations.fadeIn(nwavNodo, Duration.millis(200))));
                nwavAparicion.play();
                Platform.runLater(() -> nwavMoverIndicador(nwavMenu.getSelectedToggle()));
            });
            nwavAjuste.play();
        }
        ((FontIcon) nwavBtnContraer.getGraphic()).setIconCode(
                nwavContraido ? Feather.CHEVRONS_RIGHT : Feather.CHEVRONS_LEFT);
        Platform.runLater(() -> nwavMoverIndicador(nwavMenu.getSelectedToggle()));
    }

    private List<ToggleButton> nwavBotonesMenu() {
        return List.of(nwavNavInicio, nwavNavVisor, nwavNavRecientes, nwavNavHistorial, nwavNavPreferencias);
    }

    private List<ButtonBase> nwavTodosLosBotones() {
        List<ButtonBase> nwavTodos = new ArrayList<>(nwavBotonesMenu());
        nwavTodos.add(nwavBtnContraer);
        return nwavTodos;
    }
}
