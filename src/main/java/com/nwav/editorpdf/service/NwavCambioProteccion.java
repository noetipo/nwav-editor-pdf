package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavOpcionesProteccion;

/**
 * Cambio de proteccion pendiente, que se aplica al guardar: proteger con
 * {@code nwavOpciones}, o quitar la proteccion si {@code nwavOpciones} es null.
 */
public record NwavCambioProteccion(NwavOpcionesProteccion nwavOpciones) {

    public static final NwavCambioProteccion NWAV_QUITAR = new NwavCambioProteccion(null);

    public boolean nwavQuitar() {
        return nwavOpciones == null;
    }

    public String nwavDescripcion() {
        return nwavQuitar() ? "Se quitará la protección al guardar" : "Se protegerá con contraseña al guardar";
    }
}
