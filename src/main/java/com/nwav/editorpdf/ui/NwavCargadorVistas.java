package com.nwav.editorpdf.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga vistas FXML con controladores inyectados por Spring y las conserva para
 * reutilizarlas (patron de la plantilla, extraido del controlador principal).
 */
@Component
@RequiredArgsConstructor
public class NwavCargadorVistas {

    /** Nodo raiz de la vista y su controlador. */
    public record NwavVistaCargada(Parent nwavNodo, Object nwavControlador) {
    }

    private final ApplicationContext nwavContexto;
    private final Map<String, NwavVistaCargada> nwavVistas = new HashMap<>();

    /** Devuelve la vista {@code /view/{nombre}.fxml}, cargandola solo la primera vez. */
    public NwavVistaCargada nwavObtener(String nwavNombre) {
        return nwavVistas.computeIfAbsent(nwavNombre, this::nwavCargar);
    }

    @SuppressWarnings("unchecked")
    public <NwavC> NwavC nwavControlador(String nwavNombre) {
        return (NwavC) nwavObtener(nwavNombre).nwavControlador();
    }

    private NwavVistaCargada nwavCargar(String nwavNombre) {
        FXMLLoader nwavCargador = new FXMLLoader(getClass().getResource("/view/" + nwavNombre + ".fxml"));
        nwavCargador.setControllerFactory(nwavContexto::getBean);
        // Explicito: en macOS el hilo de eventos puede no tener class loader de contexto.
        nwavCargador.setClassLoader(getClass().getClassLoader());
        try {
            Parent nwavNodo = nwavCargador.load();
            return new NwavVistaCargada(nwavNodo, nwavCargador.getController());
        } catch (IOException nwavError) {
            throw new UncheckedIOException("No se pudo cargar la vista " + nwavNombre, nwavError);
        }
    }
}
