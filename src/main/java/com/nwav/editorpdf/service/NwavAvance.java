package com.nwav.editorpdf.service;

@FunctionalInterface
public interface NwavAvance {
    NwavAvance NWAV_NINGUNO = (nwavHecho, nwavTotal, nwavMensaje) -> { };

    void nwavInformar(double nwavHecho, double nwavTotal, String nwavMensaje);
}
