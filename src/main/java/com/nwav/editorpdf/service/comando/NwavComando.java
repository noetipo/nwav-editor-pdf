package com.nwav.editorpdf.service.comando;

import com.nwav.editorpdf.model.NwavTipoOperacion;

/**
 * Operacion de edicion reversible (patron Command). La ejecuta y deshace
 * {@link NwavGestorComandos}; rehacer vuelve a llamar a {@link #nwavEjecutar()}.
 */
public interface NwavComando {

    void nwavEjecutar();

    void nwavDeshacer();

    NwavTipoOperacion nwavTipo();

    /** Texto para el usuario, p. ej. "Eliminar 3 páginas". */
    String nwavDescripcion();

    /** Pagina que conviene mostrar tras ejecutar (o -1 para no moverse). */
    default int nwavPaginaDestacada() {
        return -1;
    }

    /** {@code false} si la ejecucion no cambio nada (no se apila en el historial). */
    default boolean nwavTuvoEfecto() {
        return true;
    }
}
