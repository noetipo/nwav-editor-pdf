package com.nwav.editorpdf;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.nwav.editorpdf.controller.NwavMainController;
import com.nwav.editorpdf.controller.NwavVisorController;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.service.NwavConfiguracionService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

public class NwavJavaFxApplication extends Application {

    private ConfigurableApplicationContext nwavContexto;
    private Throwable nwavErrorArranque;

    @Override
    public void init() {
        if (Thread.currentThread().getContextClassLoader() == null) {
            Thread.currentThread().setContextClassLoader(NwavJavaFxApplication.class.getClassLoader());
        }
        try {
            this.nwavContexto = new SpringApplicationBuilder(NwavEditorPdfApplication.class)
                    .web(WebApplicationType.NONE)
                    .run(getParameters().getRaw().toArray(String[]::new));
        } catch (RuntimeException nwavError) {
            // Se informa en start(), cuando ya se pueden mostrar ventanas.
            nwavErrorArranque = nwavError;
        }
    }

    @Override
    public void start(Stage nwavVentana) throws Exception {
        // En macOS el hilo de JavaFX puede no tener class loader de contexto (lo necesitan
        // AtlantaFX y FXML para cargar recursos).
        if (Thread.currentThread().getContextClassLoader() == null) {
            Thread.currentThread().setContextClassLoader(NwavJavaFxApplication.class.getClassLoader());
        }
        if (nwavErrorArranque != null) {
            Alert nwavAlerta = new Alert(Alert.AlertType.ERROR);
            nwavAlerta.setTitle(NwavMainController.NWAV_TITULO_APP);
            nwavAlerta.setHeaderText("No se pudo iniciar la aplicación");
            nwavAlerta.setContentText(NwavTraductorErrores.nwavMensaje(nwavErrorArranque));
            nwavAlerta.showAndWait();
            Platform.exit();
            return;
        }

        boolean nwavOscuro = nwavContexto.getBean(NwavConfiguracionService.class).nwavTemaOscuro();
        Application.setUserAgentStylesheet(nwavOscuro
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());

        nwavContexto.getBean(com.nwav.editorpdf.ui.NwavEscritorio.class).nwavRegistrar(getHostServices());
        FXMLLoader nwavCargador = new FXMLLoader(getClass().getResource("/view/nwav-main-view.fxml"));
        // LINEA CLAVE: Spring entrega los controladores ya inyectados
        nwavCargador.setControllerFactory(nwavContexto::getBean);

        Scene nwavEscena = new Scene(nwavCargador.load());
        nwavEscena.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/nwav-app.css")).toExternalForm());

        NwavMainController nwavMain = nwavCargador.getController();
        nwavVentana.titleProperty().bind(nwavMain.nwavTituloVentana());
        nwavVentana.setScene(nwavEscena);
        // Icono de la ventana y de la barra de tareas (Windows elige el tamano adecuado).
        for (int nwavLado : new int[]{16, 32, 64, 128, 256}) {
            try (java.io.InputStream nwavPng = getClass().getResourceAsStream("/img/nwav-icono-" + nwavLado + ".png")) {
                if (nwavPng != null) {
                    nwavVentana.getIcons().add(new javafx.scene.image.Image(nwavPng));
                }
            }
        }
        // Cerrar la ventana con cambios sin guardar pregunta antes (por defecto, "Guardar como").
        nwavVentana.setOnCloseRequest(nwavEvento -> {
            if (nwavContexto.getBean(NwavVisorController.class).nwavSinGuardarProperty().get()) {
                nwavEvento.consume();
                nwavMain.nwavSalir();
            }
        });
        nwavVentana.setMinWidth(980);
        nwavVentana.setMinHeight(640);
        nwavVentana.show();

        // Permite abrir un PDF pasado como argumento (p. ej. "Abrir con").
        getParameters().getUnnamed().stream()
                .filter(nwavArgumento -> nwavArgumento.toLowerCase(Locale.ROOT).endsWith(".pdf"))
                .map(Path::of)
                .filter(Files::isRegularFile)
                .findFirst()
                .ifPresent(nwavMain::nwavAbrirArchivo);
    }

    @Override
    public void stop() {
        if (nwavContexto == null) {
            return;
        }
        try {
            nwavContexto.getBean(NwavVisorController.class).nwavCerrarAlSalir();
        } finally {
            nwavContexto.close();
            Platform.exit();
        }
    }
}
