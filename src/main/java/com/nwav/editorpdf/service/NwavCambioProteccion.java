package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavOpcionesProteccion;

public record NwavCambioProteccion(NwavOpcionesProteccion nwavOpciones) {
    public static final NwavCambioProteccion NWAV_QUITAR = new NwavCambioProteccion(null);

    public boolean nwavQuitar() {
        return nwavOpciones == null;
    }

    public String nwavDescripcion() {
        return nwavQuitar() ? "Se quitará la protección al guardar" : "Se protegerá con contraseña al guardar";
    }
}
