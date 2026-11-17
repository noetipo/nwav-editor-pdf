package com.nwav.editorpdf.controller;

import atlantafx.base.theme.Styles;
import com.nwav.editorpdf.exception.NwavContrasenaIncorrectaException;
import com.nwav.editorpdf.exception.NwavContrasenaRequeridaException;
import com.nwav.editorpdf.service.NwavConfiguracionService;
import com.nwav.editorpdf.service.herramienta.NwavDependenciasService;
import com.nwav.editorpdf.service.herramienta.NwavEjecutorHerramientasService;
import com.nwav.editorpdf.service.herramienta.NwavOpcionesHerramientaService;
import com.nwav.editorpdf.service.herramienta.NwavResultadoArchivo;
import com.nwav.editorpdf.ui.NwavContextoHerramienta;
import com.nwav.editorpdf.ui.NwavDefinicionHerramienta;
import com.nwav.editorpdf.ui.NwavEscritorio;
import com.nwav.editorpdf.ui.NwavFormularioOpciones;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavReceptorArchivos;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NwavHerramientaController implements NwavReceptorArchivos, NwavContextoHerramienta {
    private final NwavEjecutorHerramientasService nwavEjecutor;
    private final NwavOpcionesHerramientaService nwavOpcionesGuardadas;
    private final NwavConfiguracionService nwavConfiguracion;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavNotificador nwavNotificador;
    private final NwavEscritorio nwavEscritorio;
    private final NwavVisorController nwavVisor;
    private final NwavMainController nwavMain;
    private final NwavCatalogoHerramientas nwavCatalogo;

    @FXML private StackPane nwavIconoHerramienta;
    @FXML private Label nwavLblTitulo;
    @FXML private Label nwavLblDescripcion;
    @FXML private VBox nwavAvisoDependencia;
    @FXML private Label nwavLblDependencia;
    @FXML private Label nwavLblAviso;
    @FXML private ListView<Path> nwavListaArchivos;
    @FXML private Label nwavLblAyudaArchivos;
    @FXML private Button nwavBtnUsarAbierto;
    @FXML private Button nwavBtnSubir;
    @FXML private Button nwavBtnBajar;
    @FXML private Button nwavBtnQuitar;
    @FXML private VBox nwavContenedorOpciones;
    @FXML private VBox nwavContenedorExtra;
    @FXML private javafx.scene.control.ToggleGroup nwavGrupoSalida;
    @FXML private RadioButton nwavRdbMismaCarpeta;
    @FXML private RadioButton nwavRdbOtraCarpeta;
    @FXML private Label nwavLblCarpeta;
    @FXML private Button nwavBtnProcesar;
    @FXML private VBox nwavTarjetaResultados;
    @FXML private VBox nwavListaResultados;

    private NwavDefinicionHerramienta nwavDefinicion;
    private NwavFormularioOpciones nwavFormulario;
    private Path nwavCarpetaSalida;
    private final Map<Path, String> nwavContrasenas = new HashMap<>();

    @FXML
    public void initialize() {
        nwavListaArchivos.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        nwavListaArchivos.setCellFactory(nwavLista -> new NwavCeldaArchivo());
        nwavListaArchivos.getItems().addListener((ListChangeListener<Path>) nwavCambio -> nwavActualizarBotones());
        nwavListaArchivos.getSelectionModel().selectedIndexProperty()
                .addListener((nwavObs, nwavAntes, nwavAhora) -> nwavActualizarBotones());
        nwavRdbOtraCarpeta.selectedProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> {
            if (nwavAhora && nwavCarpetaSalida == null) {
                nwavOnElegirCarpeta();
            }
            nwavActualizarCarpeta();
        });
    }

    public void nwavPreparar(NwavDefinicionHerramienta nwavNueva, List<Path> nwavArchivos) {
        boolean nwavMisma = nwavDefinicion == nwavNueva;
        nwavDefinicion = nwavNueva;
        nwavLblTitulo.setText(nwavNueva.getNwavNombre());
        nwavLblDescripcion.setText(nwavNueva.getNwavDescripcion());
        FontIcon nwavIcono = new FontIcon(nwavNueva.getNwavIcono());
        nwavIconoHerramienta.getChildren().setAll(nwavIcono);
        nwavIconoHerramienta.getStyleClass().removeIf(nwavClase -> nwavClase.startsWith("nwav-cat-"));
        nwavIconoHerramienta.getStyleClass().add(nwavNueva.getNwavCategoria().getNwavClaseEstilo());

        NwavDependenciasService.NwavEstado nwavEstado = nwavNueva.nwavEstado();
        nwavAvisoDependencia.setVisible(!nwavEstado.nwavDisponible());
        nwavAvisoDependencia.setManaged(!nwavEstado.nwavDisponible());
        nwavLblDependencia.setText(nwavEstado.nwavDetalle() + " " + nwavEstado.nwavInstrucciones());
        nwavLblAviso.setText(nwavNueva.getNwavAviso() == null ? "" : nwavNueva.getNwavAviso());
        nwavLblAviso.setVisible(nwavNueva.getNwavAviso() != null);
        nwavLblAviso.setManaged(nwavNueva.getNwavAviso() != null);

        if (!nwavMisma) {
            nwavListaArchivos.getItems().clear();
            nwavContrasenas.clear();
            nwavTarjetaResultados.setVisible(false);
            nwavTarjetaResultados.setManaged(false);
            Map<String, String> nwavGuardadas = nwavOpcionesGuardadas.nwavCargar(nwavNueva.getNwavId());
            nwavFormulario = new NwavFormularioOpciones(nwavNueva.getNwavOpciones(), nwavGuardadas);
            nwavContenedorOpciones.getChildren().setAll(nwavNueva.getNwavOpciones().isEmpty()
                    ? List.of(new Label("Esta herramienta no tiene opciones.")) : List.of(nwavFormulario.nwavNodo()));
            nwavContenedorExtra.getChildren().clear();
            if (nwavNueva.getNwavExtra() != null) {
                nwavContenedorExtra.getChildren().add(nwavNueva.getNwavExtra().apply(this));
            }
        }
        nwavRecibirRutas(nwavArchivos);
        String nwavExtensiones = String.join(", ", nwavNueva.getNwavExtensiones().stream()
                .map(nwavExtension -> nwavExtension.toUpperCase(Locale.ROOT)).toList());
        nwavLblAyudaArchivos.setText((nwavNueva.isNwavVariosArchivos()
                ? "Puedes añadir varios archivos (" + nwavExtensiones + ")"
                : "Elige un archivo (" + nwavExtensiones + ")")
                + (nwavNueva.isNwavOrdenImporta() ? ". El orden de la lista es el del resultado." : ".")
                + " Los originales nunca se modifican.");
        nwavBtnUsarAbierto.setVisible(nwavNueva.nwavAceptaArchivo("x.pdf"));
        nwavBtnUsarAbierto.setManaged(nwavBtnUsarAbierto.isVisible());
        nwavActualizarCarpeta();
        nwavActualizarBotones();
    }

    @FXML
    private void nwavOnAnadir() {
        FileChooser nwavSelector = new FileChooser();
        nwavSelector.setTitle("Elegir archivos");
        nwavSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos compatibles",
                nwavDefinicion.getNwavExtensiones().stream()
                        .flatMap(nwavExtension -> java.util.stream.Stream.of("*." + nwavExtension,
                                "*." + nwavExtension.toUpperCase(Locale.ROOT))).toList()));
        nwavConfiguracion.nwavCarpetaInicial().map(Path::toFile).ifPresent(nwavSelector::setInitialDirectory);
        List<File> nwavElegidos = nwavDefinicion.isNwavVariosArchivos()
                ? nwavSelector.showOpenMultipleDialog(nwavVentana())
                : java.util.Optional.ofNullable(nwavSelector.showOpenDialog(nwavVentana())).map(List::of).orElse(null);
        if (nwavElegidos != null) {
            nwavRecibir(nwavElegidos);
        }
    }

    @FXML
    private void nwavOnUsarAbierto() {
        nwavVisor.nwavDocumentoActual().ifPresentOrElse(nwavDocumento -> {
            if (nwavDocumento.nwavHayCambiosSinGuardar()) {
                nwavNotificador.nwavAdvertencia("Se usará la versión guardada de «" + nwavDocumento.getNwavNombre()
                        + "», sin los cambios pendientes.");
            }
            nwavRecibirRutas(List.of(nwavDocumento.getNwavRuta()));
        }, () -> nwavNotificador.nwavInfo("No hay ningún PDF abierto en el editor."));
    }

    @Override
    public boolean nwavAcepta(File nwavArchivo) {
        return nwavDefinicion != null && (nwavArchivo.isDirectory()
                || nwavArchivo.isFile() && nwavDefinicion.nwavAceptaArchivo(nwavArchivo.getName()));
    }

    @Override
    public void nwavRecibir(List<File> nwavRecibidos) {
        List<File> nwavArchivos = new java.util.ArrayList<>();
        for (File nwavRecibido : nwavRecibidos) {
            if (nwavRecibido.isDirectory()) {
                File[] nwavHijos = nwavRecibido.listFiles(nwavHijo -> nwavHijo.isFile() && !nwavHijo.isHidden()
                        && nwavDefinicion.nwavAceptaArchivo(nwavHijo.getName()));
                if (nwavHijos != null) {
                    java.util.Arrays.stream(nwavHijos)
                            .sorted(java.util.Comparator.comparing(File::getName, NwavHerramientaController::nwavCompararNatural))
                            .forEach(nwavArchivos::add);
                }
            } else {
                nwavArchivos.add(nwavRecibido);
            }
        }
        if (nwavArchivos.isEmpty() && !nwavRecibidos.isEmpty()) {
            nwavNotificador.nwavAdvertencia("No se encontraron archivos compatibles con esta herramienta.");
            return;
        }
        List<File> nwavValidos = nwavArchivos.stream().filter(this::nwavAcepta).toList();
        if (nwavValidos.size() < nwavArchivos.size()) {
            nwavNotificador.nwavAdvertencia("Se ignoraron " + (nwavArchivos.size() - nwavValidos.size())
                    + " archivos con un formato que esta herramienta no admite.");
        }
        nwavRecibirRutas(nwavValidos.stream().map(File::toPath).toList());
    }

    static int nwavCompararNatural(String nwavA, String nwavB) {
        java.util.regex.Matcher nwavTrozosA = java.util.regex.Pattern.compile("\\d+|\\D+").matcher(nwavA.toLowerCase(Locale.ROOT));
        java.util.regex.Matcher nwavTrozosB = java.util.regex.Pattern.compile("\\d+|\\D+").matcher(nwavB.toLowerCase(Locale.ROOT));
        while (nwavTrozosA.find() && nwavTrozosB.find()) {
            String nwavParteA = nwavTrozosA.group();
            String nwavParteB = nwavTrozosB.group();
            int nwavComparacion;
            if (Character.isDigit(nwavParteA.charAt(0)) && Character.isDigit(nwavParteB.charAt(0))) {
                nwavComparacion = new java.math.BigInteger(nwavParteA).compareTo(new java.math.BigInteger(nwavParteB));
            } else {
                nwavComparacion = nwavParteA.compareTo(nwavParteB);
            }
            if (nwavComparacion != 0) {
                return nwavComparacion;
            }
        }
        return Integer.compare(nwavA.length(), nwavB.length());
    }

    @FXML
    private void nwavOnAnadirCarpeta() {
        javafx.stage.DirectoryChooser nwavSelector = new javafx.stage.DirectoryChooser();
        nwavSelector.setTitle("Elegir una carpeta (se añaden sus archivos compatibles)");
        File nwavCarpetaElegida = nwavSelector.showDialog(nwavListaArchivos.getScene().getWindow());
        if (nwavCarpetaElegida != null) {
            nwavRecibir(List.of(nwavCarpetaElegida));
        }
    }

    private void nwavRecibirRutas(List<Path> nwavRutas) {
        if (nwavRutas == null || nwavRutas.isEmpty()) {
            return;
        }
        if (!nwavDefinicion.isNwavVariosArchivos()) {
            nwavListaArchivos.getItems().setAll(nwavRutas.get(0).toAbsolutePath().normalize());
            return;
        }
        for (Path nwavRuta : nwavRutas) {
            Path nwavNormal = nwavRuta.toAbsolutePath().normalize();
            if (!nwavListaArchivos.getItems().contains(nwavNormal) || nwavDefinicion.isNwavOrdenImporta()) {
                nwavListaArchivos.getItems().add(nwavNormal);
            }
        }
    }

    @FXML
    private void nwavOnQuitar() {
        int nwavIndice = nwavListaArchivos.getSelectionModel().getSelectedIndex();
        if (nwavIndice >= 0) {
            nwavListaArchivos.getItems().remove(nwavIndice);
        }
    }

    @FXML
    private void nwavOnVaciar() {
        nwavListaArchivos.getItems().clear();
    }

    @FXML
    private void nwavOnSubir() {
        nwavMover(-1);
    }

    @FXML
    private void nwavOnBajar() {
        nwavMover(1);
    }

    private void nwavMover(int nwavDesplazamiento) {
        int nwavDesde = nwavListaArchivos.getSelectionModel().getSelectedIndex();
        int nwavHasta = nwavDesde + nwavDesplazamiento;
        if (nwavDesde < 0 || nwavHasta < 0 || nwavHasta >= nwavListaArchivos.getItems().size()) {
            return;
        }
        Path nwavRuta = nwavListaArchivos.getItems().remove(nwavDesde);
        nwavListaArchivos.getItems().add(nwavHasta, nwavRuta);
        nwavListaArchivos.getSelectionModel().clearAndSelect(nwavHasta);
    }

    private void nwavActualizarBotones() {
        int nwavElegido = nwavListaArchivos.getSelectionModel().getSelectedIndex();
        int nwavCantidad = nwavListaArchivos.getItems().size();
        nwavBtnQuitar.setDisable(nwavElegido < 0);
        nwavBtnSubir.setDisable(nwavElegido <= 0);
        nwavBtnBajar.setDisable(nwavElegido < 0 || nwavElegido >= nwavCantidad - 1);
        boolean nwavDisponible = nwavDefinicion != null && nwavDefinicion.nwavEstado().nwavDisponible();
        nwavBtnProcesar.setDisable(!nwavDisponible || nwavDefinicion == null
                || nwavCantidad < nwavDefinicion.getNwavMinimoArchivos());
        nwavBtnProcesar.setText(nwavCantidad > 1 && nwavDefinicion != null && !nwavDefinicion.isNwavOrdenImporta()
                ? "Procesar " + nwavCantidad + " archivos" : "Procesar");
    }

    @FXML
    private void nwavOnElegirCarpeta() {
        DirectoryChooser nwavSelector = new DirectoryChooser();
        nwavSelector.setTitle("Carpeta de salida");
        File nwavElegida = nwavSelector.showDialog(nwavVentana());
        if (nwavElegida != null) {
            nwavCarpetaSalida = nwavElegida.toPath();
            nwavRdbOtraCarpeta.setSelected(true);
        } else if (nwavCarpetaSalida == null) {
            nwavRdbMismaCarpeta.setSelected(true);
        }
        nwavActualizarCarpeta();
    }

    private void nwavActualizarCarpeta() {
        nwavLblCarpeta.setText(nwavRdbOtraCarpeta.isSelected() && nwavCarpetaSalida != null
                ? nwavCarpetaSalida.toString() : "Junto a cada archivo original");
    }

    private Path nwavCarpetaElegida() {
        return nwavRdbOtraCarpeta.isSelected() ? nwavCarpetaSalida : null;
    }

    @FXML
    private void nwavOnProcesar() {
        nwavProcesar(List.copyOf(nwavListaArchivos.getItems()), false);
    }

    private void nwavProcesar(List<Path> nwavArchivos, boolean nwavReintento) {
        if (nwavArchivos.isEmpty() && nwavDefinicion.getNwavMinimoArchivos() > 0) {
            nwavNotificador.nwavAdvertencia("Añade al menos un archivo.");
            return;
        }
        List<Path> nwavFaltantes = nwavArchivos.stream().filter(nwavRuta -> !Files.isRegularFile(nwavRuta)).toList();
        if (!nwavFaltantes.isEmpty()) {
            nwavNotificador.nwavError("«" + nwavFaltantes.get(0).getFileName() + "» ya no existe.");
            return;
        }
        Map<String, String> nwavOpciones = nwavOpciones();
        String nwavId = nwavDefinicion.getNwavId();
        Map<String, String> nwavGuardables = nwavFormulario.nwavValoresGuardables();
        nwavTareaFondo.nwavGuardarEnFondo(() -> nwavOpcionesGuardadas.nwavGuardar(nwavId, nwavGuardables));
        NwavDefinicionHerramienta nwavActual = nwavDefinicion;
        Path nwavCarpeta = nwavCarpetaElegida();
        Map<Path, String> nwavClaves = Map.copyOf(nwavContrasenas);
        nwavBtnProcesar.setDisable(true);
        nwavTareaFondo.nwavEjecutarCancelable(nwavActual.getNwavNombre() + "…", (nwavAvance, nwavCancelacion) ->
                        nwavEjecutor.nwavEjecutar(nwavActual.getNwavTrabajo(), nwavArchivos, nwavOpciones, nwavCarpeta,
                                nwavClaves, nwavAvance, nwavCancelacion),
                nwavResultados -> {
                    nwavActualizarBotones();
                    nwavMostrarResultados(nwavResultados, nwavReintento);
                },
                nwavError -> {
                    nwavActualizarBotones();
                    if (!(nwavError instanceof com.nwav.editorpdf.exception.NwavCanceladoException)) {
                        nwavNotificador.nwavError(com.nwav.editorpdf.exception.NwavTraductorErrores.nwavMensaje(nwavError));
                    }
                });
    }

    private void nwavMostrarResultados(List<NwavResultadoArchivo> nwavResultados, boolean nwavAgregar) {
        if (!nwavAgregar) {
            nwavListaResultados.getChildren().clear();
        }
        long nwavCorrectos = nwavResultados.stream().filter(NwavResultadoArchivo::nwavExito).count();
        for (NwavResultadoArchivo nwavResultado : nwavResultados) {
            nwavListaResultados.getChildren().add(nwavFilaResultado(nwavResultado));
        }
        nwavTarjetaResultados.setVisible(true);
        nwavTarjetaResultados.setManaged(true);
        if (nwavCorrectos == nwavResultados.size()) {
            nwavNotificador.nwavExito(nwavResultados.size() == 1 ? "Listo." : "Listo: " + nwavCorrectos + " archivos procesados.");
        } else {
            nwavNotificador.nwavAdvertencia((nwavResultados.size() - nwavCorrectos) + " archivo(s) no se pudieron procesar.");
        }
    }

    private Node nwavFilaResultado(NwavResultadoArchivo nwavResultado) {
        FontIcon nwavEstado = new FontIcon(nwavResultado.nwavExito()
                ? (nwavResultado.nwavGenerados().isEmpty() ? Feather.ALERT_CIRCLE : Feather.CHECK_CIRCLE) : Feather.X_CIRCLE);
        nwavEstado.getStyleClass().add(nwavResultado.nwavExito()
                ? (nwavResultado.nwavGenerados().isEmpty() ? "nwav-resultado-aviso" : "nwav-resultado-ok") : "nwav-resultado-error");
        String nwavNombre = nwavResultado.nwavGenerados().isEmpty()
                ? (nwavResultado.nwavOrigen() == null ? nwavDefinicion.getNwavNombre()
                : String.valueOf(nwavResultado.nwavOrigen().getFileName()))
                : nwavResultado.nwavGenerados().stream().map(nwavRuta -> String.valueOf(nwavRuta.getFileName()))
                .reduce((nwavA, nwavB) -> nwavA + ", " + nwavB).orElse("");
        Label nwavTitulo = new Label(nwavNombre);
        nwavTitulo.getStyleClass().add("nwav-reciente-nombre");
        Label nwavResumen = new Label(nwavResultado.nwavResumen());
        nwavResumen.getStyleClass().add("nwav-texto-tenue");
        nwavResumen.setWrapText(true);
        VBox nwavTextos = new VBox(2, nwavTitulo, nwavResumen);
        nwavTextos.setMinWidth(0);
        HBox.setHgrow(nwavTextos, Priority.ALWAYS);
        HBox nwavFila = new HBox(12, nwavEstado, nwavTextos);
        nwavFila.setAlignment(Pos.CENTER_LEFT);
        nwavFila.getStyleClass().add("nwav-fila-resultado");

        if (!nwavResultado.nwavGenerados().isEmpty()) {
            Path nwavPrimero = nwavResultado.nwavGenerados().get(0);
            Button nwavAbrir = nwavBoton("Abrir", Feather.EXTERNAL_LINK, () -> {
                if (nwavPrimero.toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                    nwavMain.nwavAbrirArchivo(nwavPrimero);
                } else {
                    nwavEscritorio.nwavAbrir(nwavPrimero);
                }
            });
            Button nwavCarpeta = nwavBoton("Carpeta", Feather.FOLDER, () -> nwavEscritorio.nwavMostrarCarpeta(nwavPrimero));
            nwavFila.getChildren().addAll(nwavAbrir, nwavCarpeta);
            if (nwavDefinicion.getNwavSiguiente() != null) {
                List<Path> nwavPdfs = nwavResultado.nwavGenerados().stream()
                        .filter(nwavRuta -> nwavRuta.toString().toLowerCase(Locale.ROOT).endsWith(".pdf")).toList();
                nwavCatalogo.nwavBuscar(nwavDefinicion.getNwavSiguiente()).ifPresent(nwavOtra -> {
                    if (!nwavPdfs.isEmpty()) {
                        nwavFila.getChildren().add(nwavBoton(nwavDefinicion.getNwavTextoSiguiente(), nwavOtra.getNwavIcono(),
                                () -> nwavMain.nwavAbrirHerramienta(nwavOtra, nwavPdfs)));
                    }
                });
            }
        } else if (nwavResultado.nwavError() instanceof NwavContrasenaRequeridaException
                || nwavResultado.nwavError() instanceof NwavContrasenaIncorrectaException) {
            Path nwavOrigen = nwavResultado.nwavOrigen();
            nwavFila.getChildren().add(nwavBoton("Escribir contraseña", Feather.KEY, () ->
                    nwavNotificador.nwavPedirContrasena(String.valueOf(nwavOrigen.getFileName()),
                            nwavResultado.nwavError() instanceof NwavContrasenaIncorrectaException, nwavClave -> {
                                nwavContrasenas.put(nwavOrigen, nwavClave);
                                nwavListaResultados.getChildren().remove(nwavFila);
                                nwavProcesar(List.of(nwavOrigen), true);
                            }, null)));
        }
        return nwavFila;
    }

    private static Button nwavBoton(String nwavTexto, Feather nwavIcono, Runnable nwavAccion) {
        Button nwavBoton = new Button(nwavTexto, new FontIcon(nwavIcono));
        nwavBoton.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SMALL);
        nwavBoton.setOnAction(nwavEvento -> nwavAccion.run());
        nwavBoton.setMinWidth(Button.USE_PREF_SIZE);
        return nwavBoton;
    }

    @FXML
    private void nwavOnVolver() {
        nwavMain.nwavIrA(NwavMainController.NWAV_VISTA_INICIO);
    }

    @Override
    public List<Path> nwavArchivos() {
        return new ArrayList<>(nwavListaArchivos.getItems());
    }

    @Override
    public Map<String, String> nwavOpciones() {
        return nwavFormulario == null ? Map.of() : nwavFormulario.nwavValores();
    }

    @Override
    public String nwavContrasena(Path nwavArchivo) {
        return nwavContrasenas.get(nwavArchivo);
    }

    @Override
    public Window nwavVentana() {
        return nwavListaArchivos.getScene() == null ? null : nwavListaArchivos.getScene().getWindow();
    }

    private static final class NwavCeldaArchivo extends ListCell<Path> {
        private final Label nwavNombre = new Label();
        private final Label nwavDetalle = new Label();
        private final VBox nwavContenido = new VBox(1, nwavNombre, nwavDetalle);

        private NwavCeldaArchivo() {
            nwavNombre.getStyleClass().add("nwav-reciente-nombre");
            nwavDetalle.getStyleClass().add("nwav-reciente-ruta");
        }

        @Override
        protected void updateItem(Path nwavRuta, boolean nwavVacia) {
            super.updateItem(nwavRuta, nwavVacia);
            if (nwavVacia || nwavRuta == null) {
                setGraphic(null);
                return;
            }
            nwavNombre.setText((getIndex() + 1) + ". " + nwavRuta.getFileName());
            String nwavTamano;
            try {
                nwavTamano = com.nwav.editorpdf.service.NwavCompresorService.nwavTamano(Files.size(nwavRuta));
            } catch (java.io.IOException nwavError) {
                nwavTamano = "no disponible";
            }
            nwavDetalle.setText(nwavTamano + " · " + nwavRuta.getParent());
            setGraphic(nwavContenido);
        }
    }
}
