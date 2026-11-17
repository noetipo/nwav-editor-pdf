package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavCanceladoException;

/** Consulta si el usuario pidio cancelar. Los procesos largos la revisan a menudo. */
@FunctionalInterface
public interface NwavCancelacion {

    NwavCancelacion NWAV_NUNCA = () -> false;

    boolean nwavCancelado();

    /** Lanza {@link NwavCanceladoException} si se pidio cancelar. */
    default void nwavVerificar() {
        if (nwavCancelado() || Thread.currentThread().isInterrupted()) {
            throw new NwavCanceladoException();
        }
    }
}
