package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavCanceladoException;

@FunctionalInterface
public interface NwavCancelacion {
    NwavCancelacion NWAV_NUNCA = () -> false;

    boolean nwavCancelado();

    default void nwavVerificar() {
        if (nwavCancelado() || Thread.currentThread().isInterrupted()) {
            throw new NwavCanceladoException();
        }
    }
}
