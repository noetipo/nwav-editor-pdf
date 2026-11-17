package com.nwav.editorpdf.ui;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Punto unico para la retroalimentacion visual de la aplicacion: notificaciones
 * flotantes, dialogos modales y panel lateral. El {@code NwavMainController}
 * registra aqui las capas de la ventana principal. (Heredado de la plantilla.)
 */
@Component
public class NwavNotificador {

    private static final Duration NWAV_DURACION_TOAST = Duration.seconds(4);
    private static final String NWAV_PROPIEDAD_CERRANDO = "nwav-cerrando";

    private ModalPane nwavModalPanel;
    private ModalPane nwavModalDialogo;
    private VBox nwavZonaToasts;

    public void nwavRegistrar(ModalPane nwavModalPanel, ModalPane nwavModalDialogo, VBox nwavZonaToasts) {
        this.nwavModalPanel = nwavModalPanel;
        this.nwavModalDialogo = nwavModalDialogo;
        this.nwavZonaToasts = nwavZonaToasts;
    }

    // ---------------------------------------------------------------- toasts

    public void nwavExito(String nwavMensaje) {
        nwavToast(nwavMensaje, Feather.CHECK_CIRCLE, Styles.SUCCESS);
    }

    public void nwavAdvertencia(String nwavMensaje) {
        nwavToast(nwavMensaje, Feather.ALERT_TRIANGLE, Styles.WARNING);
    }

    public void nwavError(String nwavMensaje) {
        nwavToast(nwavMensaje, Feather.X_CIRCLE, Styles.DANGER);
    }

    public void nwavInfo(String nwavMensaje) {
        nwavToast(nwavMensaje, Feather.INFO, Styles.ACCENT);
    }

    private void nwavToast(String nwavMensaje, Ikon nwavIcono, String nwavEstilo) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> nwavToast(nwavMensaje, nwavIcono, nwavEstilo));
            return;
        }
        if (nwavZonaToasts == null) {
            return;
        }
        Notification nwavAviso = new Notification(nwavMensaje, new FontIcon(nwavIcono));
        nwavAviso.getStyleClass().addAll(nwavEstilo, Styles.ELEVATED_2, "nwav-toast");
        nwavAviso.setPrefWidth(380);
        nwavAviso.setMaxWidth(380);
        nwavAviso.setOnClose(nwavEvento -> nwavCerrarToast(nwavAviso));

        nwavZonaToasts.getChildren().add(nwavAviso);
        Animations.fadeInUp(nwavAviso, Duration.millis(280)).play();

        PauseTransition nwavEspera = new PauseTransition(NWAV_DURACION_TOAST);
        nwavEspera.setOnFinished(nwavEvento -> nwavCerrarToast(nwavAviso));
        nwavEspera.play();
    }

    private void nwavCerrarToast(Notification nwavAviso) {
        if (!nwavZonaToasts.getChildren().contains(nwavAviso)
                || nwavAviso.getProperties().containsKey(NWAV_PROPIEDAD_CERRANDO)) {
            return;
        }
        nwavAviso.getProperties().put(NWAV_PROPIEDAD_CERRANDO, true);
        Timeline nwavSalida = Animations.fadeOutRight(nwavAviso, Duration.millis(250));
        nwavSalida.setOnFinished(nwavEvento -> nwavZonaToasts.getChildren().remove(nwavAviso));
        nwavSalida.play();
    }

    // ---------------------------------------------------------------- dialogos

    /** Dialogo de confirmacion de una accion destructiva; ejecuta la accion solo si se acepta. */
    public void nwavConfirmar(String nwavTitulo, String nwavMensaje, String nwavTextoAccion, Runnable nwavAlConfirmar) {
        Button nwavCancelar = nwavBotonSecundario("Cancelar", null);
        Button nwavAceptar = new Button(nwavTextoAccion);
        nwavAceptar.getStyleClass().add(Styles.DANGER);
        nwavAceptar.setOnAction(nwavEvento -> {
            nwavModalDialogo.hide();
            nwavAlConfirmar.run();
        });
        nwavMostrarDialogo(Feather.ALERT_TRIANGLE, "nwav-peligro", nwavTitulo, nwavMensaje, null,
                nwavCancelar, nwavAceptar);
        nwavAceptar.requestFocus();
    }

    /** Opcion de un dialogo de eleccion. La que tiene {@code nwavPorDefecto} responde a Intro. */
    public record NwavOpcion(String nwavTexto, String nwavEstilo, Runnable nwavAccion, boolean nwavPorDefecto) {

        public static NwavOpcion nwavCancelar() {
            return new NwavOpcion("Cancelar", Styles.BUTTON_OUTLINED, null, false);
        }
    }

    /**
     * Dialogo con varias acciones (p. ej. "Cancelar / Descartar / Guardar como…").
     * La primera opcion sin accion actua como cancelar (tecla Esc).
     */
    public void nwavElegir(String nwavTitulo, String nwavMensaje, boolean nwavAdvertencia, NwavOpcion... nwavOpciones) {
        Button[] nwavBotones = new Button[nwavOpciones.length];
        Button nwavFoco = null;
        for (int nwavIndice = 0; nwavIndice < nwavOpciones.length; nwavIndice++) {
            NwavOpcion nwavOpcion = nwavOpciones[nwavIndice];
            Button nwavBoton = new Button(nwavOpcion.nwavTexto());
            for (String nwavClase : nwavOpcion.nwavEstilo().split(",")) {
                nwavBoton.getStyleClass().add(nwavClase.trim());
            }
            nwavBoton.setCancelButton(nwavOpcion.nwavAccion() == null);
            nwavBoton.setDefaultButton(nwavOpcion.nwavPorDefecto());
            nwavBoton.setOnAction(nwavEvento -> {
                nwavModalDialogo.hide();
                if (nwavOpcion.nwavAccion() != null) {
                    nwavOpcion.nwavAccion().run();
                }
            });
            if (nwavOpcion.nwavPorDefecto()) {
                nwavFoco = nwavBoton;
            }
            nwavBotones[nwavIndice] = nwavBoton;
        }
        nwavMostrarDialogo(nwavAdvertencia ? Feather.ALERT_TRIANGLE : Feather.SAVE,
                nwavAdvertencia ? "nwav-aviso" : "nwav-acento", nwavTitulo, nwavMensaje, null, nwavBotones);
        if (nwavFoco != null) {
            nwavFoco.requestFocus();
        }
    }

    /**
     * Pide la contrasena de un PDF protegido.
     *
     * @param nwavReintento muestra el aviso de contrasena incorrecta.
     */
    public void nwavPedirContrasena(String nwavArchivo, boolean nwavReintento,
                                    Consumer<String> nwavAlAceptar, Runnable nwavAlCancelar) {
        PasswordField nwavCampo = new PasswordField();
        nwavCampo.setPromptText("Contraseña");

        VBox nwavExtra = new VBox(6, nwavCampo);
        if (nwavReintento) {
            Label nwavAviso = new Label("La contraseña no es correcta. Inténtalo de nuevo.");
            nwavAviso.getStyleClass().add("nwav-campo-error");
            nwavExtra.getChildren().add(nwavAviso);
            nwavCampo.pseudoClassStateChanged(Styles.STATE_DANGER, true);
        }

        Button nwavCancelar = nwavBotonSecundario("Cancelar", nwavAlCancelar);
        Button nwavAbrir = new Button("Abrir");
        nwavAbrir.getStyleClass().add(Styles.ACCENT);
        nwavAbrir.setDefaultButton(true);
        nwavAbrir.disableProperty().bind(nwavCampo.textProperty().isEmpty());
        nwavAbrir.setOnAction(nwavEvento -> {
            String nwavTexto = nwavCampo.getText();
            nwavModalDialogo.hide();
            nwavAlAceptar.accept(nwavTexto);
        });

        nwavMostrarDialogo(Feather.LOCK, "nwav-acento", "Documento protegido",
                "«" + nwavArchivo + "» requiere una contraseña para abrirse.", nwavExtra,
                nwavCancelar, nwavAbrir);
        Platform.runLater(nwavCampo::requestFocus);
    }

    private Button nwavBotonSecundario(String nwavTexto, Runnable nwavAccion) {
        Button nwavBoton = new Button(nwavTexto);
        nwavBoton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        nwavBoton.setCancelButton(true);
        nwavBoton.setOnAction(nwavEvento -> {
            nwavModalDialogo.hide();
            if (nwavAccion != null) {
                nwavAccion.run();
            }
        });
        return nwavBoton;
    }

    private void nwavMostrarDialogo(Ikon nwavIconoDialogo, String nwavClaseIcono, String nwavTitulo,
                                    String nwavMensaje, Node nwavContenidoExtra, Button... nwavBotones) {
        FontIcon nwavIcono = new FontIcon(nwavIconoDialogo);
        StackPane nwavCirculo = new StackPane(nwavIcono);
        nwavCirculo.getStyleClass().addAll("nwav-dialogo-icono", nwavClaseIcono);

        Label nwavLblTitulo = new Label(nwavTitulo);
        nwavLblTitulo.getStyleClass().add("nwav-dialogo-titulo");
        Label nwavLblMensaje = new Label(nwavMensaje);
        nwavLblMensaje.getStyleClass().add("nwav-dialogo-mensaje");
        nwavLblMensaje.setWrapText(true);

        Region nwavEspacio = new Region();
        HBox.setHgrow(nwavEspacio, Priority.ALWAYS);
        HBox nwavAcciones = new HBox(10, nwavEspacio);
        nwavAcciones.getChildren().addAll(nwavBotones);
        nwavAcciones.setAlignment(Pos.CENTER_RIGHT);
        nwavAcciones.getStyleClass().add("nwav-dialogo-acciones");

        VBox nwavDialogo = new VBox(14, nwavCirculo, nwavLblTitulo, nwavLblMensaje);
        if (nwavContenidoExtra != null) {
            nwavDialogo.getChildren().add(nwavContenidoExtra);
        }
        nwavDialogo.getChildren().add(nwavAcciones);
        nwavDialogo.getStyleClass().add("nwav-dialogo");
        nwavDialogo.setMaxSize(440, Region.USE_PREF_SIZE);

        nwavModalDialogo.usePredefinedTransitionFactories(null);
        nwavModalDialogo.show(nwavDialogo);
    }

    // ------------------------------------------------------------ panel lateral

    /** Desliza un panel desde la derecha. */
    public void nwavAbrirPanel(Node nwavContenido) {
        // Se renuevan las transiciones: el ModalPane las cachea para el primer nodo mostrado.
        nwavModalPanel.usePredefinedTransitionFactories(Side.RIGHT,
                Duration.millis(320), Duration.millis(220));
        nwavModalPanel.show(nwavContenido);
    }

    public void nwavCerrarPanel() {
        nwavModalPanel.hide();
    }
}
