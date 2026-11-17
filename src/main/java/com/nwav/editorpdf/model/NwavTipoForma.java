package com.nwav.editorpdf.model;

/** Formas basicas (anotaciones). */
public enum NwavTipoForma {
    NWAV_RECTANGULO("Rectángulo"),
    NWAV_ELIPSE("Elipse"),
    NWAV_LINEA("Línea"),
    NWAV_FLECHA("Flecha");

    private final String nwavDescripcion;

    NwavTipoForma(String nwavDescripcion) {
        this.nwavDescripcion = nwavDescripcion;
    }

    public String getNwavDescripcion() {
        return nwavDescripcion;
    }
}
