package com.nwav.editorpdf.ui;

import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.service.NwavAvance;
import jakarta.annotation.PreDestroy;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Ejecuta trabajo pesado fuera del hilo de JavaFX con {@link Task}, muestra el
 * indicador de progreso de la barra de estado y traduce los errores a mensajes
 * en espanol. Generaliza el patron que la plantilla usaba en su Exportador.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NwavTareaFondo {

    /** Trabajo que puede informar su avance. */
    @FunctionalInterface
    public interface NwavTrabajoConProgreso<NwavR> {
        NwavR nwavEjecutar(NwavAvance nwavAvance) throws Exception;
    }

    /** Trabajo que informa su avance y consulta si se pidio cancelar. */
    @FunctionalInterface
    public interface NwavTrabajoCancelable<NwavR> {
        NwavR nwavEjecutar(NwavAvance nwavAvance, com.nwav.editorpdf.service.NwavCancelacion nwavCancelacion)
                throws Exception;
    }

    private final NwavNotificador nwavNotificador;
    /** Tarea que se puede cancelar desde la barra de estado (solo una a la vez). */
    private final javafx.beans.property.ObjectProperty<Task<?>> nwavCancelable =
            new javafx.beans.property.SimpleObjectProperty<>();

    private final ExecutorService nwavEjecutorGeneral = Executors.newCachedThreadPool(nwavFabrica("nwav-tarea"));
    private final ExecutorService nwavEjecutorPaginas = Executors.newSingleThreadExecutor(nwavFabrica("nwav-render"));
    private final ExecutorService nwavEjecutorMiniaturas = Executors.newSingleThreadExecutor(nwavFabrica("nwav-miniatura"));
    /** Las ediciones se aplican de una en una y en el orden en que se pidieron. */
    private final ExecutorService nwavEjecutorEdicion = Executors.newSingleThreadExecutor(nwavFabrica("nwav-edicion"));

    private final IntegerProperty nwavTareasActivas = new SimpleIntegerProperty(0);
    private Region nwavZonaProgreso;
    private ProgressBar nwavBarra;
    private Label nwavEtiqueta;

    /** La ventana principal registra aqui su barra de estado. */
    public void nwavRegistrar(Region nwavZonaProgreso, ProgressBar nwavBarra, Label nwavEtiqueta) {
        this.nwavZonaProgreso = nwavZonaProgreso;
        this.nwavBarra = nwavBarra;
        this.nwavEtiqueta = nwavEtiqueta;
        nwavZonaProgreso.visibleProperty().bind(nwavTareasActivas.greaterThan(0));
        nwavZonaProgreso.managedProperty().bind(nwavZonaProgreso.visibleProperty());
    }

    /** Ejecuta con indicador de progreso indeterminado. */
    public <NwavR> Task<NwavR> nwavEjecutar(String nwavMensaje, Callable<NwavR> nwavTrabajo,
                                            Consumer<NwavR> nwavAlTerminar, Consumer<Throwable> nwavAlFallar) {
        return nwavEjecutarConProgreso(nwavMensaje, nwavAvance -> nwavTrabajo.call(), nwavAlTerminar, nwavAlFallar);
    }

    /** Ejecuta mostrando el avance real informado por el trabajo. */
    public <NwavR> Task<NwavR> nwavEjecutarConProgreso(String nwavMensaje, NwavTrabajoConProgreso<NwavR> nwavTrabajo,
                                                       Consumer<NwavR> nwavAlTerminar,
                                                       Consumer<Throwable> nwavAlFallar) {
        return nwavEnviarConProgreso(nwavEjecutorGeneral, nwavMensaje, nwavTrabajo, nwavAlTerminar, nwavAlFallar);
    }

    /**
     * Tarea larga que el usuario puede cancelar con el boton de la barra de estado. Al
     * cancelar, el trabajo lo detecta en su siguiente comprobacion y su resultado se descarta.
     */
    public <NwavR> Task<NwavR> nwavEjecutarCancelable(String nwavMensaje, NwavTrabajoCancelable<NwavR> nwavTrabajo,
                                                      Consumer<NwavR> nwavAlTerminar, Consumer<Throwable> nwavAlFallar) {
        Task<NwavR> nwavTarea = new Task<>() {
            @Override
            protected NwavR call() throws Exception {
                updateMessage(nwavMensaje);
                return nwavTrabajo.nwavEjecutar((nwavHecho, nwavTotal, nwavTexto) -> {
                    updateProgress(nwavHecho, nwavTotal);
                    if (nwavTexto != null) {
                        updateMessage(nwavTexto);
                    }
                }, this::isCancelled);
            }
        };
        nwavMostrarProgreso(nwavTarea);
        nwavEnHiloFx(() -> nwavCancelable.set(nwavTarea));
        Runnable nwavSoltar = () -> {
            nwavOcultarProgreso();
            if (nwavCancelable.get() == nwavTarea) {
                nwavCancelable.set(null);
            }
        };
        nwavTarea.setOnSucceeded(nwavEvento -> {
            nwavSoltar.run();
            if (nwavAlTerminar != null) {
                nwavAlTerminar.accept(nwavTarea.getValue());
            }
        });
        nwavTarea.setOnFailed(nwavEvento -> {
            nwavSoltar.run();
            if (nwavTarea.getException() instanceof com.nwav.editorpdf.exception.NwavCanceladoException) {
                nwavAvisarCancelacion(nwavAlFallar);
                return;
            }
            nwavManejarError(nwavTarea.getException(), nwavAlFallar);
        });
        nwavTarea.setOnCancelled(nwavEvento -> {
            nwavSoltar.run();
            nwavAvisarCancelacion(nwavAlFallar);
        });
        nwavEjecutorGeneral.execute(nwavTarea);
        return nwavTarea;
    }

    /** La cancelacion se informa como aviso (no como error) y llega tambien a quien lanzo la tarea. */
    private void nwavAvisarCancelacion(Consumer<Throwable> nwavAlFallar) {
        nwavNotificador.nwavInfo("Operación cancelada.");
        if (nwavAlFallar != null) {
            nwavAlFallar.accept(new com.nwav.editorpdf.exception.NwavCanceladoException());
        }
    }

    public javafx.beans.property.ReadOnlyObjectProperty<Task<?>> nwavCancelableProperty() {
        return nwavCancelable;
    }

    /** Pide cancelar la tarea cancelable en curso (si hay). */
    public void nwavCancelar() {
        Task<?> nwavTarea = nwavCancelable.get();
        if (nwavTarea != null) {
            nwavTarea.cancel(false);
        }
    }

    /** Edicion del documento abierto: en un hilo propio, en orden y con indicador de progreso. */
    public <NwavR> Task<NwavR> nwavEjecutarEdicion(String nwavMensaje, Callable<NwavR> nwavTrabajo,
                                                   Consumer<NwavR> nwavAlTerminar, Consumer<Throwable> nwavAlFallar) {
        return nwavEnviarConProgreso(nwavEjecutorEdicion, nwavMensaje, nwavAvance -> nwavTrabajo.call(),
                nwavAlTerminar, nwavAlFallar);
    }

    private <NwavR> Task<NwavR> nwavEnviarConProgreso(ExecutorService nwavEjecutor, String nwavMensaje,
                                                      NwavTrabajoConProgreso<NwavR> nwavTrabajo,
                                                      Consumer<NwavR> nwavAlTerminar,
                                                      Consumer<Throwable> nwavAlFallar) {
        Task<NwavR> nwavTarea = new Task<>() {
            @Override
            protected NwavR call() throws Exception {
                updateMessage(nwavMensaje);
                return nwavTrabajo.nwavEjecutar((nwavHecho, nwavTotal, nwavTexto) -> {
                    updateProgress(nwavHecho, nwavTotal);
                    if (nwavTexto != null) {
                        updateMessage(nwavTexto);
                    }
                });
            }
        };
        nwavMostrarProgreso(nwavTarea);
        nwavTarea.setOnSucceeded(nwavEvento -> {
            nwavOcultarProgreso();
            if (nwavAlTerminar != null) {
                nwavAlTerminar.accept(nwavTarea.getValue());
            }
        });
        nwavTarea.setOnFailed(nwavEvento -> {
            nwavOcultarProgreso();
            nwavManejarError(nwavTarea.getException(), nwavAlFallar);
        });
        nwavTarea.setOnCancelled(nwavEvento -> nwavOcultarProgreso());
        nwavEjecutor.execute(nwavTarea);
        return nwavTarea;
    }

    /** Renderizado de paginas: un solo hilo y sin indicador global (cada pagina muestra el suyo). */
    public <NwavR> void nwavRenderizarPagina(Callable<NwavR> nwavTrabajo, Consumer<NwavR> nwavAlTerminar,
                                             Consumer<Throwable> nwavAlFallar) {
        nwavEnviarSilencioso(nwavEjecutorPaginas, nwavTrabajo, nwavAlTerminar, nwavAlFallar);
    }

    public <NwavR> void nwavRenderizarMiniatura(Callable<NwavR> nwavTrabajo, Consumer<NwavR> nwavAlTerminar,
                                                Consumer<Throwable> nwavAlFallar) {
        nwavEnviarSilencioso(nwavEjecutorMiniaturas, nwavTrabajo, nwavAlTerminar, nwavAlFallar);
    }

    /** Consulta o escritura rapida en segundo plano, sin indicador de progreso. */
    public <NwavR> void nwavEjecutarSilencioso(Callable<NwavR> nwavTrabajo, Consumer<NwavR> nwavAlTerminar) {
        nwavEnviarSilencioso(nwavEjecutorGeneral, nwavTrabajo, nwavAlTerminar, null);
    }

    /** Escritura rapida en segundo plano (p. ej. guardar preferencias) sin indicador. */
    public void nwavGuardarEnFondo(Runnable nwavTrabajo) {
        nwavEjecutarSilencioso(() -> {
            nwavTrabajo.run();
            return null;
        }, null);
    }

    private <NwavR> void nwavEnviarSilencioso(ExecutorService nwavEjecutor, Callable<NwavR> nwavTrabajo,
                                              Consumer<NwavR> nwavAlTerminar, Consumer<Throwable> nwavAlFallar) {
        Task<NwavR> nwavTarea = new Task<>() {
            @Override
            protected NwavR call() throws Exception {
                return nwavTrabajo.call();
            }
        };
        nwavTarea.setOnSucceeded(nwavEvento -> {
            if (nwavAlTerminar != null) {
                nwavAlTerminar.accept(nwavTarea.getValue());
            }
        });
        nwavTarea.setOnFailed(nwavEvento -> nwavManejarError(nwavTarea.getException(), nwavAlFallar));
        nwavEjecutor.execute(nwavTarea);
    }

    private void nwavManejarError(Throwable nwavError, Consumer<Throwable> nwavAlFallar) {
        if (nwavAlFallar != null) {
            nwavAlFallar.accept(nwavError);
            return;
        }
        nwavLog.error("Error en tarea de fondo", nwavError);
        nwavNotificador.nwavError(NwavTraductorErrores.nwavMensaje(nwavError));
    }

    private void nwavMostrarProgreso(Task<?> nwavTarea) {
        nwavEnHiloFx(() -> {
            nwavTareasActivas.set(nwavTareasActivas.get() + 1);
            if (nwavBarra != null) {
                nwavBarra.progressProperty().bind(nwavTarea.progressProperty());
                nwavEtiqueta.textProperty().bind(nwavTarea.messageProperty());
            }
        });
    }

    private void nwavOcultarProgreso() {
        nwavTareasActivas.set(Math.max(0, nwavTareasActivas.get() - 1));
        if (nwavTareasActivas.get() == 0 && nwavBarra != null) {
            nwavBarra.progressProperty().unbind();
            nwavEtiqueta.textProperty().unbind();
        }
    }

    private static void nwavEnHiloFx(Runnable nwavAccion) {
        if (Platform.isFxApplicationThread()) {
            nwavAccion.run();
        } else {
            Platform.runLater(nwavAccion);
        }
    }

    private static ThreadFactory nwavFabrica(String nwavPrefijo) {
        AtomicInteger nwavContador = new AtomicInteger();
        return nwavTarea -> {
            Thread nwavHilo = new Thread(nwavTarea, nwavPrefijo + "-" + nwavContador.incrementAndGet());
            nwavHilo.setDaemon(true);
            return nwavHilo;
        };
    }

    @PreDestroy
    public void nwavApagar() {
        nwavEjecutorGeneral.shutdownNow();
        nwavEjecutorPaginas.shutdownNow();
        nwavEjecutorMiniaturas.shutdownNow();
        nwavEjecutorEdicion.shutdownNow();
    }
}
