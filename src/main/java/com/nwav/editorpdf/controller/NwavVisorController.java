package com.nwav.editorpdf.controller;

import com.nwav.editorpdf.exception.NwavArchivoNoEncontradoException;
import com.nwav.editorpdf.exception.NwavContrasenaIncorrectaException;
import com.nwav.editorpdf.exception.NwavContrasenaRequeridaException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.model.NwavArchivoReciente;
import com.nwav.editorpdf.model.NwavModoZoom;
import com.nwav.editorpdf.model.NwavZoomPreferido;
import com.nwav.editorpdf.service.NwavArchivoRecienteService;
import com.nwav.editorpdf.service.NwavConfiguracionService;
import com.nwav.editorpdf.service.NwavDocumentoPdf;
import com.nwav.editorpdf.service.NwavPdfDocumentoService;
import com.nwav.editorpdf.service.NwavPdfRenderService;
import com.nwav.editorpdf.ui.NwavCeldaMiniatura;
import com.nwav.editorpdf.ui.NwavImagenFx;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.stream.IntStream;

/**
 * Visor de PDF: scroll continuo, zoom, navegacion por pagina y panel de miniaturas.
 * <p>
 * Solo coordina la interfaz: abrir y dibujar paginas lo hacen los servicios, siempre
 * en segundo plano. Solo se renderizan las paginas cercanas a la zona visible; las
 * lejanas sueltan su imagen para no acumular memoria con documentos grandes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NwavVisorController {

    /** 100 % = tamano real en una pantalla de 96 ppp. */
    public static final double NWAV_PIXELES_POR_PUNTO = 96.0 / 72.0;
    public static final double NWAV_ESPACIO_PAGINAS = 16;
    public static final double NWAV_MARGEN_DOCUMENTO = 24;
    public static final double NWAV_ANCHO_MINIATURA = 116;
    public static final double[] NWAV_PASOS_ZOOM = {
            0.10, 0.25, 0.33, 0.50, 0.67, 0.75, 0.90, 1.00, 1.10, 1.25, 1.50, 1.75, 2.00, 2.50, 3.00, 4.00, 5.00,
            6.00, 8.00};
    private static final int NWAV_PAGINAS_ANTICIPADAS = 2;
    private static final int NWAV_PAGINAS_CONSERVADAS = 6;
    private static final int NWAV_MINIATURAS_EN_MEMORIA = 400;

    private final NwavPdfDocumentoService nwavPdfService;
    private final NwavPdfRenderService nwavRenderService;
    private final NwavArchivoRecienteService nwavRecientes;
    private final NwavConfiguracionService nwavConfiguracion;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavNotificador nwavNotificador;

    @FXML private Button nwavBtnCerrar;
    @FXML private Button nwavBtnAnterior;
    @FXML private Button nwavBtnSiguiente;
    @FXML private Button nwavBtnAlejar;
    @FXML private Button nwavBtnAcercar;
    @FXML private ToggleButton nwavTglMiniaturas;
    @FXML private ToggleButton nwavTglAjustarAncho;
    @FXML private ToggleButton nwavTglAjustarPagina;
    @FXML private ToggleGroup nwavGrupoAjuste;
    @FXML private TextField nwavTxtPagina;
    @FXML private Label nwavLblTotalPaginas;
    @FXML private ComboBox<String> nwavCmbZoom;
    @FXML private Label nwavLblArchivo;
    @FXML private VBox nwavPanelMiniaturas;
    @FXML private ListView<Integer> nwavListaMiniaturas;
    @FXML private ScrollPane nwavScroll;
    @FXML private VBox nwavContenedorPaginas;
    @FXML private VBox nwavPanelVacio;

    /** Pagina del visor: un marco con su imagen (o nada, si aun no se dibujo). */
    private static final class NwavNodoPagina {
        private final StackPane nwavMarco = new StackPane();
        private final ImageView nwavVista = new ImageView();
        private final Label nwavAviso = new Label();
        private float nwavEscalaMostrada;
        private float nwavEscalaPedida;
    }

    /** Resultado de abrir un archivo en segundo plano. */
    private record NwavApertura(NwavDocumentoPdf nwavDocumento, Optional<NwavArchivoReciente> nwavPrevio,
                                NwavZoomPreferido nwavZoomDefecto, boolean nwavRestaurar, String nwavAviso) {
    }

    // Estado (solo se toca desde el hilo de JavaFX salvo los campos volatile).
    private NwavDocumentoPdf nwavDocumento;
    private int nwavGeneracion;
    private volatile int nwavGeneracionActiva;
    private volatile int nwavPrimeraVisible;
    private volatile int nwavUltimaVisible;
    private NwavModoZoom nwavModoZoom = NwavModoZoom.NWAV_AJUSTAR_ANCHO;
    private double nwavFactorZoom = 1.0;
    private final List<NwavNodoPagina> nwavNodos = new ArrayList<>();
    private double[] nwavInicioY = new double[0];
    private double nwavAltoTotal;
    private int nwavPaginaActual;
    private boolean nwavActualizandoControles;
    private final Map<Integer, Image> nwavMiniaturasFx = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Image> nwavMasAntigua) {
            return size() > NWAV_MINIATURAS_EN_MEMORIA;
        }
    };
    private PauseTransition nwavPausaGuardado;
    private PauseTransition nwavPausaAjuste;

    private final BooleanProperty nwavHayDocumento = new SimpleBooleanProperty(false);
    private final BooleanProperty nwavMiniaturasVisibles = new SimpleBooleanProperty(true);
    private final StringProperty nwavTitulo = new SimpleStringProperty("");
    private final StringProperty nwavEstado = new SimpleStringProperty("Sin documento");

    private Runnable nwavAccionVerRecientes = () -> { };
    private Runnable nwavAlCambiarRecientes = () -> { };

    // =============================================================== inicio

    @FXML
    public void initialize() {
        nwavPausaGuardado = new PauseTransition(Duration.millis(1200));
        nwavPausaGuardado.setOnFinished(nwavEvento -> nwavGuardarEstadoEnFondo());
        nwavPausaAjuste = new PauseTransition(Duration.millis(90));
        nwavPausaAjuste.setOnFinished(nwavEvento -> nwavReaplicarAjuste());

        nwavContenedorPaginas.setPadding(new Insets(NWAV_MARGEN_DOCUMENTO));
        nwavContenedorPaginas.setSpacing(NWAV_ESPACIO_PAGINAS);
        // El contenedor ocupa al menos el ancho visible para centrar las paginas,
        // pero puede crecer y mostrar scroll horizontal con zooms grandes.
        nwavScroll.viewportBoundsProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> {
            nwavContenedorPaginas.setMinWidth(nwavAhora.getWidth());
            if (nwavAntes.getWidth() != nwavAhora.getWidth() || nwavAntes.getHeight() != nwavAhora.getHeight()) {
                if (nwavModoZoom != NwavModoZoom.NWAV_PORCENTAJE) {
                    nwavPausaAjuste.playFromStart();
                }
                nwavActualizarVisibles();
            }
        });
        nwavScroll.vvalueProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavActualizarVisibles());
        nwavScroll.addEventFilter(ScrollEvent.SCROLL, nwavEvento -> {
            if (nwavEvento.isShortcutDown() && nwavDocumento != null && nwavEvento.getDeltaY() != 0) {
                if (nwavEvento.getDeltaY() > 0) {
                    nwavAcercar();
                } else {
                    nwavAlejar();
                }
                nwavEvento.consume();
            }
        });
        nwavScroll.addEventHandler(ZoomEvent.ZOOM, nwavEvento -> {
            if (nwavDocumento != null && nwavEvento.getZoomFactor() > 0) {
                nwavEstablecerZoom(NwavModoZoom.NWAV_PORCENTAJE, nwavFactorZoom * nwavEvento.getZoomFactor());
            }
        });
        nwavScroll.setOnKeyPressed(nwavEvento -> {
            if (nwavDocumento == null) {
                return;
            }
            if (nwavEvento.getCode() == KeyCode.HOME) {
                nwavIrAPagina(0);
                nwavEvento.consume();
            } else if (nwavEvento.getCode() == KeyCode.END) {
                nwavIrAPagina(nwavNodos.size() - 1);
                nwavEvento.consume();
            }
        });

        nwavCmbZoom.getItems().setAll(IntStream.of(25, 50, 75, 100, 125, 150, 200, 300, 400)
                .mapToObj(NwavVisorController::nwavTextoPorcentaje).toList());
        nwavCmbZoom.setOnAction(nwavEvento -> {
            if (!nwavActualizandoControles) {
                nwavAplicarZoomEscrito(nwavCmbZoom.getEditor().getText());
            }
        });
        nwavCmbZoom.getEditor().focusedProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> {
            if (!nwavAhora) {
                nwavMostrarZoomEnControles();
            }
        });
        nwavGrupoAjuste.selectedToggleProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> {
            if (nwavActualizandoControles || nwavDocumento == null) {
                return;
            }
            if (nwavAhora == nwavTglAjustarAncho) {
                nwavEstablecerZoom(NwavModoZoom.NWAV_AJUSTAR_ANCHO, nwavFactorZoom);
            } else if (nwavAhora == nwavTglAjustarPagina) {
                nwavEstablecerZoom(NwavModoZoom.NWAV_AJUSTAR_PAGINA, nwavFactorZoom);
            } else {
                nwavEstablecerZoom(NwavModoZoom.NWAV_PORCENTAJE, nwavFactorZoom);
            }
        });

        nwavTxtPagina.setOnAction(nwavEvento -> nwavAplicarPaginaEscrita());
        nwavTxtPagina.focusedProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> {
            if (!nwavAhora) {
                nwavMostrarPaginaEnControles();
            }
        });

        nwavListaMiniaturas.setCellFactory(nwavLista ->
                new NwavCeldaMiniatura(nwavProveedorMiniaturas(), NWAV_ANCHO_MINIATURA));
        nwavListaMiniaturas.getSelectionModel().selectedItemProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> {
            if (!nwavActualizandoControles && nwavAhora != null && nwavAhora != nwavPaginaActual) {
                nwavIrAPagina(nwavAhora);
            }
        });

        nwavMiniaturasVisibles.set(nwavConfiguracion.nwavMiniaturasVisibles());
        nwavTglMiniaturas.selectedProperty().bindBidirectional(nwavMiniaturasVisibles);
        nwavPanelMiniaturas.visibleProperty().bind(nwavMiniaturasVisibles.and(nwavHayDocumento));
        nwavPanelMiniaturas.managedProperty().bind(nwavPanelMiniaturas.visibleProperty());
        nwavMiniaturasVisibles.addListener((nwavObs, nwavAntes, nwavAhora) ->
                nwavTareaFondo.nwavGuardarEnFondo(() -> nwavConfiguracion.nwavGuardarMiniaturasVisibles(nwavAhora)));

        nwavPanelVacio.visibleProperty().bind(nwavHayDocumento.not());
        nwavScroll.visibleProperty().bind(nwavHayDocumento);
        for (var nwavControl : List.of(nwavBtnCerrar, nwavBtnAnterior, nwavBtnSiguiente, nwavBtnAlejar,
                nwavBtnAcercar, nwavTglAjustarAncho, nwavTglAjustarPagina, nwavTxtPagina, nwavCmbZoom)) {
            nwavControl.disableProperty().bind(nwavHayDocumento.not());
        }
        nwavMostrarZoomEnControles();
        nwavMostrarPaginaEnControles();
    }

    /** El controlador principal conecta aqui la navegacion y el menu de recientes. */
    public void nwavConfigurarAcciones(Runnable nwavVerRecientes, Runnable nwavAlCambiarRecientes) {
        this.nwavAccionVerRecientes = nwavVerRecientes;
        this.nwavAlCambiarRecientes = nwavAlCambiarRecientes;
    }

    public ReadOnlyBooleanProperty nwavHayDocumentoProperty() {
        return nwavHayDocumento;
    }

    public BooleanProperty nwavMiniaturasVisiblesProperty() {
        return nwavMiniaturasVisibles;
    }

    public ReadOnlyStringProperty nwavTituloProperty() {
        return nwavTitulo;
    }

    public ReadOnlyStringProperty nwavEstadoProperty() {
        return nwavEstado;
    }

    // ============================================================ abrir/cerrar

    @FXML
    public void nwavOnAbrir() {
        Window nwavVentana = nwavScroll.getScene() == null ? null : nwavScroll.getScene().getWindow();
        FileChooser nwavSelector = new FileChooser();
        nwavSelector.setTitle("Abrir documento PDF");
        nwavSelector.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documentos PDF", "*.pdf", "*.PDF"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        nwavConfiguracion.nwavCarpetaInicial().map(Path::toFile).ifPresent(nwavSelector::setInitialDirectory);
        File nwavElegido = nwavSelector.showOpenDialog(nwavVentana);
        if (nwavElegido != null) {
            nwavAbrirArchivo(nwavElegido.toPath());
        }
    }

    @FXML
    private void nwavOnVerRecientes() {
        nwavAccionVerRecientes.run();
    }

    public void nwavAbrirArchivo(Path nwavRuta) {
        nwavAbrirArchivo(nwavRuta, null);
    }

    private void nwavAbrirArchivo(Path nwavRuta, String nwavContrasena) {
        String nwavNombre = String.valueOf(nwavRuta.getFileName());
        nwavTareaFondo.nwavEjecutar("Abriendo «" + nwavNombre + "»…",
                () -> nwavAbrirEnFondo(nwavRuta, nwavContrasena),
                this::nwavMostrarDocumento,
                nwavError -> nwavErrorAlAbrir(nwavRuta, nwavError));
    }

    /** Se ejecuta en segundo plano: abre el PDF y consulta/actualiza los recientes. */
    private NwavApertura nwavAbrirEnFondo(Path nwavRuta, String nwavContrasena) {
        NwavDocumentoPdf nwavNuevo = nwavPdfService.nwavAbrir(nwavRuta, nwavContrasena);
        try {
            Optional<NwavArchivoReciente> nwavPrevio = nwavRecientes.nwavBuscar(nwavRuta);
            nwavRecientes.nwavRegistrarApertura(nwavRuta);
            Path nwavCarpeta = nwavRuta.toAbsolutePath().getParent();
            if (nwavCarpeta != null) {
                nwavConfiguracion.nwavGuardarUltimaCarpeta(nwavCarpeta);
            }
            return new NwavApertura(nwavNuevo, nwavPrevio, nwavConfiguracion.nwavZoomPorDefecto(),
                    nwavConfiguracion.nwavRestaurarPosicion(), null);
        } catch (RuntimeException nwavError) {
            // Sin base de datos el documento se puede ver igual; solo se avisa.
            nwavLog.warn("No se pudo registrar {} en recientes", nwavRuta, nwavError);
            return new NwavApertura(nwavNuevo, Optional.empty(), NwavZoomPreferido.NWAV_POR_DEFECTO, false,
                    "No se pudo guardar en recientes: " + NwavTraductorErrores.nwavMensaje(nwavError));
        }
    }

    private void nwavErrorAlAbrir(Path nwavRuta, Throwable nwavError) {
        String nwavNombre = String.valueOf(nwavRuta.getFileName());
        if (nwavError instanceof NwavContrasenaRequeridaException
                || nwavError instanceof NwavContrasenaIncorrectaException) {
            boolean nwavReintento = nwavError instanceof NwavContrasenaIncorrectaException;
            nwavNotificador.nwavPedirContrasena(nwavNombre, nwavReintento,
                    nwavClave -> nwavAbrirArchivo(nwavRuta, nwavClave), null);
            return;
        }
        nwavLog.warn("No se pudo abrir {}: {}", nwavRuta, nwavError.toString());
        nwavNotificador.nwavError(NwavTraductorErrores.nwavTraducir(nwavError, nwavNombre).getMessage());
        if (nwavError instanceof NwavArchivoNoEncontradoException) {
            nwavAlCambiarRecientes.run();
        }
    }

    private void nwavMostrarDocumento(NwavApertura nwavApertura) {
        nwavCerrarDocumentoActual(true);

        NwavDocumentoPdf nwavNuevo = nwavApertura.nwavDocumento();
        nwavDocumento = nwavNuevo;
        nwavGeneracion++;
        nwavGeneracionActiva = nwavGeneracion;
        nwavConstruirPaginas();
        nwavActualizandoControles = true;
        nwavListaMiniaturas.getItems().setAll(
                IntStream.range(0, nwavNuevo.getNwavNumeroPaginas()).boxed().toList());
        nwavActualizandoControles = false;
        nwavLblTotalPaginas.setText("/ " + nwavNuevo.getNwavNumeroPaginas());
        nwavLblArchivo.setText(nwavNuevo.getNwavNombre());
        nwavTitulo.set(nwavNuevo.getNwavNombre());
        nwavHayDocumento.set(true);

        NwavZoomPreferido nwavZoom = nwavApertura.nwavZoomDefecto();
        int nwavPagina = 0;
        if (nwavApertura.nwavRestaurar() && nwavApertura.nwavPrevio().isPresent()) {
            NwavArchivoReciente nwavPrevio = nwavApertura.nwavPrevio().get();
            nwavZoom = nwavPrevio.getNwavZoomPreferido();
            nwavPagina = Math.min(nwavPrevio.getNwavUltimaPagina(), nwavNuevo.getNwavNumeroPaginas() - 1);
        }
        nwavPaginaActual = nwavPagina;
        NwavZoomPreferido nwavZoomInicial = nwavZoom;
        int nwavPaginaInicial = nwavPagina;
        // Tras el primer layout el visor ya conoce su tamano para los modos de ajuste.
        Platform.runLater(() -> {
            if (nwavDocumento != nwavNuevo) {
                return;
            }
            nwavEstablecerZoomSinAnclar(nwavZoomInicial.nwavModo(), nwavZoomInicial.nwavFactor());
            nwavIrAPagina(nwavPaginaInicial);
            nwavScroll.requestFocus();
        });

        if (nwavPaginaInicial > 0) {
            nwavNotificador.nwavInfo("Continuando en la página " + (nwavPaginaInicial + 1) + ".");
        }
        if (nwavApertura.nwavAviso() != null) {
            nwavNotificador.nwavAdvertencia(nwavApertura.nwavAviso());
        }
        nwavAlCambiarRecientes.run();
    }

    @FXML
    public void nwavOnCerrar() {
        nwavCerrarDocumentoActual(true);
    }

    /**
     * Cierra el documento actual: guarda su posicion, vacia la interfaz y libera el
     * PDDocument y sus caches en segundo plano (espera a que acabe cualquier renderizado).
     */
    private void nwavCerrarDocumentoActual(boolean nwavGuardarEstado) {
        if (nwavDocumento == null) {
            return;
        }
        NwavDocumentoPdf nwavAnterior = nwavDocumento;
        Path nwavRuta = nwavAnterior.getNwavRuta();
        int nwavPagina = nwavPaginaActual;
        NwavModoZoom nwavModo = nwavModoZoom;
        double nwavFactor = nwavFactorZoom;

        nwavDocumento = null;
        nwavGeneracion++;
        nwavGeneracionActiva = nwavGeneracion;
        nwavPausaGuardado.stop();
        nwavNodos.clear();
        nwavContenedorPaginas.getChildren().clear();
        nwavInicioY = new double[0];
        nwavMiniaturasFx.clear();
        nwavActualizandoControles = true;
        nwavListaMiniaturas.getItems().clear();
        nwavActualizandoControles = false;
        nwavHayDocumento.set(false);
        nwavTitulo.set("");
        nwavLblArchivo.setText("");
        nwavLblTotalPaginas.setText("/ 0");
        nwavPaginaActual = 0;
        nwavMostrarPaginaEnControles();
        nwavEstado.set("Sin documento");

        nwavTareaFondo.nwavGuardarEnFondo(() -> {
            try {
                if (nwavGuardarEstado) {
                    nwavRecientes.nwavGuardarEstado(nwavRuta, nwavPagina, nwavModo, nwavFactor);
                }
            } finally {
                nwavAnterior.close();
            }
        });
    }

    /** Al salir de la aplicacion: guarda la posicion y cierra, de forma sincrona. */
    public void nwavCerrarAlSalir() {
        NwavDocumentoPdf nwavAnterior = nwavDocumento;
        if (nwavAnterior == null) {
            return;
        }
        nwavDocumento = null;
        try {
            nwavRecientes.nwavGuardarEstado(nwavAnterior.getNwavRuta(), nwavPaginaActual, nwavModoZoom, nwavFactorZoom);
        } catch (RuntimeException nwavError) {
            nwavLog.warn("No se pudo guardar la posicion de {}", nwavAnterior.getNwavRuta(), nwavError);
        } finally {
            nwavAnterior.close();
        }
    }

    private void nwavGuardarEstadoEnFondo() {
        if (nwavDocumento == null) {
            return;
        }
        Path nwavRuta = nwavDocumento.getNwavRuta();
        int nwavPagina = nwavPaginaActual;
        NwavModoZoom nwavModo = nwavModoZoom;
        double nwavFactor = nwavFactorZoom;
        nwavTareaFondo.nwavGuardarEnFondo(() ->
                nwavRecientes.nwavGuardarEstado(nwavRuta, nwavPagina, nwavModo, nwavFactor));
    }

    // ================================================================ paginas

    private void nwavConstruirPaginas() {
        nwavNodos.clear();
        List<StackPane> nwavMarcos = new ArrayList<>();
        for (int nwavIndice = 0; nwavIndice < nwavDocumento.getNwavNumeroPaginas(); nwavIndice++) {
            NwavNodoPagina nwavNodo = new NwavNodoPagina();
            nwavNodo.nwavVista.setSmooth(true);
            nwavNodo.nwavAviso.setText("Página " + (nwavIndice + 1));
            nwavNodo.nwavAviso.getStyleClass().add("nwav-pagina-aviso");
            nwavNodo.nwavMarco.getStyleClass().add("nwav-pagina-pdf");
            nwavNodo.nwavMarco.getChildren().addAll(nwavNodo.nwavAviso, nwavNodo.nwavVista);
            nwavNodos.add(nwavNodo);
            nwavMarcos.add(nwavNodo.nwavMarco);
        }
        nwavContenedorPaginas.getChildren().setAll(nwavMarcos);
        nwavAplicarTamanos();
    }

    /** Da a cada pagina su tamano segun el zoom y recalcula la posicion vertical de cada una. */
    private void nwavAplicarTamanos() {
        double nwavPx = nwavFactorZoom * NWAV_PIXELES_POR_PUNTO;
        nwavInicioY = new double[nwavNodos.size()];
        double nwavY = NWAV_MARGEN_DOCUMENTO;
        for (int nwavIndice = 0; nwavIndice < nwavNodos.size(); nwavIndice++) {
            NwavDocumentoPdf.NwavTamanoPagina nwavTamano = nwavDocumento.nwavTamano(nwavIndice);
            double nwavAncho = Math.round(nwavTamano.nwavAncho() * nwavPx);
            double nwavAlto = Math.round(nwavTamano.nwavAlto() * nwavPx);
            NwavNodoPagina nwavNodo = nwavNodos.get(nwavIndice);
            nwavNodo.nwavMarco.setMinSize(nwavAncho, nwavAlto);
            nwavNodo.nwavMarco.setPrefSize(nwavAncho, nwavAlto);
            nwavNodo.nwavMarco.setMaxSize(nwavAncho, nwavAlto);
            nwavNodo.nwavVista.setFitWidth(nwavAncho);
            nwavNodo.nwavVista.setFitHeight(nwavAlto);
            nwavInicioY[nwavIndice] = nwavY;
            nwavY += nwavAlto + NWAV_ESPACIO_PAGINAS;
        }
        nwavAltoTotal = nwavY - NWAV_ESPACIO_PAGINAS + NWAV_MARGEN_DOCUMENTO;
    }

    private double nwavAltoPagina(int nwavIndice) {
        return nwavNodos.get(nwavIndice).nwavMarco.getPrefHeight();
    }

    private double nwavDesplazamientoMaximo() {
        return Math.max(0, nwavAltoTotal - nwavScroll.getViewportBounds().getHeight());
    }

    private double nwavYSuperior() {
        return nwavScroll.getVvalue() * nwavDesplazamientoMaximo();
    }

    private void nwavDesplazarA(double nwavY) {
        double nwavMaximo = nwavDesplazamientoMaximo();
        nwavScroll.setVvalue(nwavMaximo <= 0 ? 0 : Math.max(0, Math.min(1, nwavY / nwavMaximo)));
    }

    /** Ultima pagina cuyo borde superior queda por encima de {@code nwavY} (busqueda binaria). */
    private int nwavPaginaEn(double nwavY) {
        int nwavBajo = 0;
        int nwavAlto = nwavInicioY.length - 1;
        int nwavResultado = 0;
        while (nwavBajo <= nwavAlto) {
            int nwavMedio = (nwavBajo + nwavAlto) >>> 1;
            if (nwavInicioY[nwavMedio] <= nwavY) {
                nwavResultado = nwavMedio;
                nwavBajo = nwavMedio + 1;
            } else {
                nwavAlto = nwavMedio - 1;
            }
        }
        return nwavResultado;
    }

    public void nwavIrAPagina(int nwavIndice) {
        if (nwavDocumento == null || nwavNodos.isEmpty()) {
            return;
        }
        int nwavDestino = Math.max(0, Math.min(nwavNodos.size() - 1, nwavIndice));
        nwavDesplazarA(nwavInicioY[nwavDestino] - NWAV_ESPACIO_PAGINAS / 2);
        nwavCambiarPaginaActual(nwavDestino);
        nwavActualizarVisibles();
    }

    @FXML
    public void nwavOnPaginaAnterior() {
        nwavIrAPagina(nwavPaginaActual - 1);
    }

    @FXML
    public void nwavOnPaginaSiguiente() {
        nwavIrAPagina(nwavPaginaActual + 1);
    }

    public void nwavIrAPrimera() {
        nwavIrAPagina(0);
    }

    public void nwavIrAUltima() {
        nwavIrAPagina(nwavNodos.size() - 1);
    }

    /**
     * Calcula que paginas estan a la vista, pide las que faltan y suelta la imagen de
     * las que quedaron lejos.
     */
    private void nwavActualizarVisibles() {
        if (nwavDocumento == null || nwavNodos.isEmpty()) {
            return;
        }
        double nwavAltoVisible = nwavScroll.getViewportBounds().getHeight();
        double nwavArriba = nwavYSuperior();
        int nwavPrimera = nwavPaginaEn(nwavArriba);
        int nwavUltima = nwavPaginaEn(nwavArriba + nwavAltoVisible);
        nwavPrimeraVisible = nwavPrimera;
        nwavUltimaVisible = nwavUltima;

        int nwavActual = nwavScroll.getVvalue() >= 0.999 && nwavDesplazamientoMaximo() > 0
                ? nwavUltima
                : nwavPaginaEn(nwavArriba + nwavAltoVisible * 0.35);
        nwavCambiarPaginaActual(nwavActual);

        int nwavDesde = Math.max(0, nwavPrimera - NWAV_PAGINAS_ANTICIPADAS);
        int nwavHasta = Math.min(nwavNodos.size() - 1, nwavUltima + NWAV_PAGINAS_ANTICIPADAS);
        for (int nwavIndice = nwavDesde; nwavIndice <= nwavHasta; nwavIndice++) {
            nwavSolicitarRender(nwavIndice);
        }
        for (int nwavIndice = 0; nwavIndice < nwavNodos.size(); nwavIndice++) {
            boolean nwavLejos = nwavIndice < nwavPrimera - NWAV_PAGINAS_CONSERVADAS
                    || nwavIndice > nwavUltima + NWAV_PAGINAS_CONSERVADAS;
            NwavNodoPagina nwavNodo = nwavNodos.get(nwavIndice);
            if (nwavLejos && nwavNodo.nwavVista.getImage() != null) {
                nwavNodo.nwavVista.setImage(null);
                nwavNodo.nwavEscalaMostrada = 0;
            }
        }
    }

    private float nwavEscalaRender() {
        double nwavPantalla = 1.0;
        if (nwavScroll.getScene() != null && nwavScroll.getScene().getWindow() != null) {
            nwavPantalla = nwavScroll.getScene().getWindow().getOutputScaleX();
        }
        return (float) (nwavFactorZoom * NWAV_PIXELES_POR_PUNTO * nwavPantalla);
    }

    private void nwavSolicitarRender(int nwavIndice) {
        NwavNodoPagina nwavNodo = nwavNodos.get(nwavIndice);
        float nwavEscala = nwavEscalaRender();
        if (nwavNodo.nwavEscalaMostrada == nwavEscala || nwavNodo.nwavEscalaPedida == nwavEscala) {
            return;
        }
        nwavNodo.nwavEscalaPedida = nwavEscala;
        NwavDocumentoPdf nwavDoc = nwavDocumento;
        int nwavGen = nwavGeneracion;
        nwavTareaFondo.nwavRenderizarPagina(() -> {
            // Si entretanto se cambio de documento o la pagina salio de la vista, no se dibuja.
            boolean nwavVigente = nwavGen == nwavGeneracionActiva
                    && nwavIndice >= nwavPrimeraVisible - NWAV_PAGINAS_ANTICIPADAS - 1
                    && nwavIndice <= nwavUltimaVisible + NWAV_PAGINAS_ANTICIPADAS + 1;
            if (!nwavVigente) {
                return null;
            }
            return NwavImagenFx.nwavConvertir(nwavRenderService.nwavRenderizarPagina(nwavDoc, nwavIndice, nwavEscala));
        }, nwavImagen -> {
            if (nwavGen != nwavGeneracion) {
                return;
            }
            if (nwavNodo.nwavEscalaPedida == nwavEscala) {
                nwavNodo.nwavEscalaPedida = 0;
            }
            if (nwavImagen != null) {
                nwavNodo.nwavVista.setImage(nwavImagen);
                nwavNodo.nwavEscalaMostrada = nwavEscala;
            }
        }, nwavError -> {
            if (nwavGen != nwavGeneracion) {
                return;
            }
            nwavNodo.nwavEscalaPedida = 0;
            nwavNodo.nwavAviso.setText("No se pudo dibujar la página " + (nwavIndice + 1));
            nwavLog.warn("Fallo al dibujar la pagina {}", nwavIndice + 1, nwavError);
        });
    }

    private void nwavCambiarPaginaActual(int nwavIndice) {
        if (nwavIndice == nwavPaginaActual && !nwavTxtPagina.getText().isEmpty()) {
            nwavActualizarEstado();
            return;
        }
        nwavPaginaActual = nwavIndice;
        nwavMostrarPaginaEnControles();
        nwavSincronizarMiniatura();
        nwavActualizarEstado();
        nwavPausaGuardado.playFromStart();
    }

    private void nwavSincronizarMiniatura() {
        if (nwavListaMiniaturas.getItems().isEmpty()) {
            return;
        }
        nwavActualizandoControles = true;
        nwavListaMiniaturas.getSelectionModel().select(nwavPaginaActual);
        nwavActualizandoControles = false;
        VirtualFlow<?> nwavFlujo = (VirtualFlow<?>) nwavListaMiniaturas.lookup(".virtual-flow");
        if (nwavFlujo != null) {
            IndexedCell<?> nwavPrimera = nwavFlujo.getFirstVisibleCell();
            IndexedCell<?> nwavUltima = nwavFlujo.getLastVisibleCell();
            if (nwavPrimera == null || nwavUltima == null
                    || nwavPaginaActual <= nwavPrimera.getIndex() || nwavPaginaActual >= nwavUltima.getIndex()) {
                nwavListaMiniaturas.scrollTo(Math.max(0, nwavPaginaActual - 1));
            }
        }
    }

    private void nwavAplicarPaginaEscrita() {
        if (nwavDocumento == null) {
            return;
        }
        int nwavTotal = nwavNodos.size();
        try {
            int nwavNumero = Integer.parseInt(nwavTxtPagina.getText().trim());
            if (nwavNumero < 1 || nwavNumero > nwavTotal) {
                throw new NumberFormatException();
            }
            nwavIrAPagina(nwavNumero - 1);
            nwavScroll.requestFocus();
        } catch (NumberFormatException nwavError) {
            nwavNotificador.nwavAdvertencia("Escribe un número de página entre 1 y " + nwavTotal + ".");
            nwavMostrarPaginaEnControles();
        }
    }

    private void nwavMostrarPaginaEnControles() {
        nwavTxtPagina.setText(nwavDocumento == null ? "" : String.valueOf(nwavPaginaActual + 1));
    }

    private void nwavActualizarEstado() {
        if (nwavDocumento == null) {
            nwavEstado.set("Sin documento");
            return;
        }
        nwavEstado.set("Página " + (nwavPaginaActual + 1) + " de " + nwavNodos.size()
                + "  ·  " + nwavTextoPorcentaje((int) Math.round(nwavFactorZoom * 100))
                + (nwavDocumento.isNwavCifrado() ? "  ·  Protegido" : ""));
    }

    // =================================================================== zoom

    @FXML
    public void nwavOnAcercar() {
        nwavAcercar();
    }

    @FXML
    public void nwavOnAlejar() {
        nwavAlejar();
    }

    public void nwavAcercar() {
        for (double nwavPaso : NWAV_PASOS_ZOOM) {
            if (nwavPaso > nwavFactorZoom + 0.001) {
                nwavEstablecerZoom(NwavModoZoom.NWAV_PORCENTAJE, nwavPaso);
                return;
            }
        }
    }

    public void nwavAlejar() {
        for (int nwavIndice = NWAV_PASOS_ZOOM.length - 1; nwavIndice >= 0; nwavIndice--) {
            if (NWAV_PASOS_ZOOM[nwavIndice] < nwavFactorZoom - 0.001) {
                nwavEstablecerZoom(NwavModoZoom.NWAV_PORCENTAJE, NWAV_PASOS_ZOOM[nwavIndice]);
                return;
            }
        }
    }

    public void nwavTamanoReal() {
        nwavEstablecerZoom(NwavModoZoom.NWAV_PORCENTAJE, 1.0);
    }

    public void nwavAjustarAncho() {
        nwavEstablecerZoom(NwavModoZoom.NWAV_AJUSTAR_ANCHO, nwavFactorZoom);
    }

    public void nwavAjustarPagina() {
        nwavEstablecerZoom(NwavModoZoom.NWAV_AJUSTAR_PAGINA, nwavFactorZoom);
    }

    private void nwavAplicarZoomEscrito(String nwavTexto) {
        String nwavDigitos = nwavTexto == null ? "" : nwavTexto.replaceAll("[^0-9.,]", "").replace(',', '.');
        try {
            double nwavPorcentaje = Double.parseDouble(nwavDigitos);
            nwavEstablecerZoom(NwavModoZoom.NWAV_PORCENTAJE, nwavPorcentaje / 100.0);
        } catch (NumberFormatException nwavError) {
            nwavNotificador.nwavAdvertencia("Escribe un porcentaje de zoom, por ejemplo 125 %.");
            nwavMostrarZoomEnControles();
        }
    }

    /** Cambia el zoom manteniendo a la vista el mismo punto de la pagina actual. */
    private void nwavEstablecerZoom(NwavModoZoom nwavModo, double nwavFactor) {
        if (nwavDocumento == null || nwavNodos.isEmpty()) {
            return;
        }
        int nwavAncla = nwavPaginaActual;
        double nwavFraccion = (nwavYSuperior() - nwavInicioY[nwavAncla]) / Math.max(1, nwavAltoPagina(nwavAncla));
        nwavEstablecerZoomSinAnclar(nwavModo, nwavFactor);
        nwavDesplazarA(nwavInicioY[nwavAncla] + nwavFraccion * nwavAltoPagina(nwavAncla));
        nwavActualizarVisibles();
    }

    private void nwavEstablecerZoomSinAnclar(NwavModoZoom nwavModo, double nwavFactor) {
        nwavModoZoom = nwavModo;
        double nwavNuevo = switch (nwavModo) {
            case NWAV_AJUSTAR_ANCHO -> nwavFactorAjusteAncho();
            case NWAV_AJUSTAR_PAGINA -> nwavFactorAjustePagina();
            case NWAV_PORCENTAJE -> nwavFactor;
        };
        nwavFactorZoom = new NwavZoomPreferido(nwavModo, nwavNuevo).nwavFactor();
        nwavAplicarTamanos();
        nwavMostrarZoomEnControles();
        nwavActualizarEstado();
        nwavPausaGuardado.playFromStart();
    }

    /** En los modos de ajuste, recalcula el zoom cuando cambia el tamano de la ventana. */
    private void nwavReaplicarAjuste() {
        if (nwavDocumento != null && nwavModoZoom != NwavModoZoom.NWAV_PORCENTAJE) {
            nwavEstablecerZoom(nwavModoZoom, nwavFactorZoom);
        }
    }

    private double nwavFactorAjusteAncho() {
        double nwavDisponible = nwavScroll.getViewportBounds().getWidth() - 2 * NWAV_MARGEN_DOCUMENTO;
        double nwavMasAncha = nwavDocumento.getNwavTamanos().stream()
                .mapToDouble(NwavDocumentoPdf.NwavTamanoPagina::nwavAncho).max().orElse(612);
        return nwavDisponible <= 0 ? nwavFactorZoom : nwavDisponible / (nwavMasAncha * NWAV_PIXELES_POR_PUNTO);
    }

    private double nwavFactorAjustePagina() {
        double nwavAltoDisponible = nwavScroll.getViewportBounds().getHeight() - 2 * NWAV_MARGEN_DOCUMENTO;
        NwavDocumentoPdf.NwavTamanoPagina nwavTamano = nwavDocumento.nwavTamano(nwavPaginaActual);
        if (nwavAltoDisponible <= 0) {
            return nwavFactorZoom;
        }
        double nwavPorAlto = nwavAltoDisponible / (nwavTamano.nwavAlto() * NWAV_PIXELES_POR_PUNTO);
        return Math.min(nwavPorAlto, nwavFactorAjusteAncho());
    }

    private void nwavMostrarZoomEnControles() {
        nwavActualizandoControles = true;
        try {
            nwavCmbZoom.getEditor().setText(nwavTextoPorcentaje((int) Math.round(nwavFactorZoom * 100)));
            nwavTglAjustarAncho.setSelected(nwavModoZoom == NwavModoZoom.NWAV_AJUSTAR_ANCHO);
            nwavTglAjustarPagina.setSelected(nwavModoZoom == NwavModoZoom.NWAV_AJUSTAR_PAGINA);
        } finally {
            nwavActualizandoControles = false;
        }
    }

    private static String nwavTextoPorcentaje(int nwavPorcentaje) {
        return String.format(Locale.ROOT, "%d %%", nwavPorcentaje);
    }

    // ============================================================ miniaturas

    private NwavCeldaMiniatura.NwavProveedorMiniaturas nwavProveedorMiniaturas() {
        return new NwavCeldaMiniatura.NwavProveedorMiniaturas() {
            @Override
            public Image nwavEnCache(int nwavIndice) {
                return nwavMiniaturasFx.get(nwavIndice);
            }

            @Override
            public double nwavProporcion(int nwavIndice) {
                if (nwavDocumento == null || nwavIndice >= nwavDocumento.getNwavNumeroPaginas()) {
                    return 1.414;
                }
                NwavDocumentoPdf.NwavTamanoPagina nwavTamano = nwavDocumento.nwavTamano(nwavIndice);
                return nwavTamano.nwavAlto() / Math.max(1f, nwavTamano.nwavAncho());
            }

            @Override
            public void nwavSolicitar(int nwavIndice, IntSupplier nwavIndiceDeseado, Consumer<Image> nwavAlRecibir) {
                nwavSolicitarMiniatura(nwavIndice, nwavIndiceDeseado, nwavAlRecibir);
            }
        };
    }

    private void nwavSolicitarMiniatura(int nwavIndice, IntSupplier nwavIndiceDeseado, Consumer<Image> nwavAlRecibir) {
        NwavDocumentoPdf nwavDoc = nwavDocumento;
        if (nwavDoc == null) {
            return;
        }
        int nwavGen = nwavGeneracion;
        double nwavPantalla = nwavScroll.getScene() == null || nwavScroll.getScene().getWindow() == null
                ? 1.0 : nwavScroll.getScene().getWindow().getOutputScaleX();
        int nwavAnchoPx = (int) Math.round(NWAV_ANCHO_MINIATURA * nwavPantalla);
        nwavTareaFondo.nwavRenderizarMiniatura(() -> {
            // La celda pudo reutilizarse para otra pagina mientras esperaba turno.
            if (nwavGen != nwavGeneracionActiva || nwavIndiceDeseado.getAsInt() != nwavIndice) {
                return null;
            }
            return NwavImagenFx.nwavConvertir(nwavRenderService.nwavRenderizarMiniatura(nwavDoc, nwavIndice, nwavAnchoPx));
        }, nwavImagen -> {
            if (nwavImagen == null || nwavGen != nwavGeneracion) {
                return;
            }
            nwavMiniaturasFx.put(nwavIndice, nwavImagen);
            nwavAlRecibir.accept(nwavImagen);
        }, nwavError -> nwavLog.debug("Fallo la miniatura {}", nwavIndice + 1, nwavError));
    }
}
