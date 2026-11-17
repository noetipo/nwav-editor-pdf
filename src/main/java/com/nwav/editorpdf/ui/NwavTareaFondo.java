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

@Component
@RequiredArgsConstructor
@Slf4j
public class NwavTareaFondo {
    @FunctionalInterface
    public interface NwavTrabajoConProgreso<NwavR> {
        NwavR nwavEjecutar(NwavAvance nwavAvance) throws Exception;
    }

    @FunctionalInterface
    public interface NwavTrabajoCancelable<NwavR> {
        NwavR nwavEjecutar(NwavAvance nwavAvance, com.nwav.editorpdf.service.NwavCancelacion nwavCancelacion)
                throws Exception;
    }

    private final NwavNotificador nwavNotificador;
    private final javafx.beans.property.ObjectProperty<Task<?>> nwavCancelable =
            new javafx.beans.property.SimpleObjectProperty<>();

    private final ExecutorService nwavEjecutorGeneral = Executors.newCachedThreadPool(nwavFabrica("nwav-tarea"));
    private final ExecutorService nwavEjecutorPaginas = Executors.newSingleThreadExecutor(nwavFabrica("nwav-render"));
    private final ExecutorService nwavEjecutorMiniaturas = Executors.newSingleThreadExecutor(nwavFabrica("nwav-miniatura"));
    private final ExecutorService nwavEjecutorEdicion = Executors.newSingleThreadExecutor(nwavFabrica("nwav-edicion"));

    private final IntegerProperty nwavTareasActivas = new SimpleIntegerProperty(0);
    private Region nwavZonaProgreso;
    private ProgressBar nwavBarra;
    private Label nwavEtiqueta;

    public void nwavRegistrar(Region nwavZonaProgreso, ProgressBar nwavBarra, Label nwavEtiqueta) {
        this.nwavZonaProgreso = nwavZonaProgreso;
        this.nwavBarra = nwavBarra;
        this.nwavEtiqueta = nwavEtiqueta;
        nwavZonaProgreso.visibleProperty().bind(nwavTareasActivas.greaterThan(0));
        nwavZonaProgreso.managedProperty().bind(nwavZonaProgreso.visibleProperty());
    }

    public <NwavR> Task<NwavR> nwavEjecutar(String nwavMensaje, Callable<NwavR> nwavTrabajo,
                                            Consumer<NwavR> nwavAlTerminar, Consumer<Throwable> nwavAlFallar) {
        return nwavEjecutarConProgreso(nwavMensaje, nwavAvance -> nwavTrabajo.call(), nwavAlTerminar, nwavAlFallar);
    }

    public <NwavR> Task<NwavR> nwavEjecutarConProgreso(String nwavMensaje, NwavTrabajoConProgreso<NwavR> nwavTrabajo,
                                                       Consumer<NwavR> nwavAlTerminar,
                                                       Consumer<Throwable> nwavAlFallar) {
        return nwavEnviarConProgreso(nwavEjecutorGeneral, nwavMensaje, nwavTrabajo, nwavAlTerminar, nwavAlFallar);
    }

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

    private void nwavAvisarCancelacion(Consumer<Throwable> nwavAlFallar) {
        nwavNotificador.nwavInfo("Operación cancelada.");
        if (nwavAlFallar != null) {
            nwavAlFallar.accept(new com.nwav.editorpdf.exception.NwavCanceladoException());
        }
    }

    public javafx.beans.property.ReadOnlyObjectProperty<Task<?>> nwavCancelableProperty() {
        return nwavCancelable;
    }

    public void nwavCancelar() {
        Task<?> nwavTarea = nwavCancelable.get();
        if (nwavTarea != null) {
            nwavTarea.cancel(false);
        }
    }

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

    public <NwavR> void nwavRenderizarPagina(Callable<NwavR> nwavTrabajo, Consumer<NwavR> nwavAlTerminar,
                                             Consumer<Throwable> nwavAlFallar) {
        nwavEnviarSilencioso(nwavEjecutorPaginas, nwavTrabajo, nwavAlTerminar, nwavAlFallar);
    }

    public <NwavR> void nwavRenderizarMiniatura(Callable<NwavR> nwavTrabajo, Consumer<NwavR> nwavAlTerminar,
                                                Consumer<Throwable> nwavAlFallar) {
        nwavEnviarSilencioso(nwavEjecutorMiniaturas, nwavTrabajo, nwavAlTerminar, nwavAlFallar);
    }

    public <NwavR> void nwavEjecutarSilencioso(Callable<NwavR> nwavTrabajo, Consumer<NwavR> nwavAlTerminar) {
        nwavEnviarSilencioso(nwavEjecutorGeneral, nwavTrabajo, nwavAlTerminar, null);
    }

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
