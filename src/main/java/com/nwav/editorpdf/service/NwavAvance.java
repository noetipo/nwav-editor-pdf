package com.nwav.editorpdf.service;

/** Informe de avance de una operacion larga (independiente de JavaFX). */
@FunctionalInterface
public interface NwavAvance {

    NwavAvance NWAV_NINGUNO = (nwavHecho, nwavTotal, nwavMensaje) -> { };

    void nwavInformar(double nwavHecho, double nwavTotal, String nwavMensaje);
}
