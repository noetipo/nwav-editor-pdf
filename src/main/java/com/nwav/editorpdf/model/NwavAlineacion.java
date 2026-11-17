package com.nwav.editorpdf.model;

public enum NwavAlineacion {
    NWAV_IZQUIERDA("Izquierda"),
    NWAV_CENTRO("Centro"),
    NWAV_DERECHA("Derecha");

    private final String nwavDescripcion;

    NwavAlineacion(String nwavDescripcion) {
        this.nwavDescripcion = nwavDescripcion;
    }

    @Override
    public String toString() {
        return nwavDescripcion;
    }
}
