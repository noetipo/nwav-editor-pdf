package com.nwav.editorpdf.model;

/**
 * Familias de letra para los cuadros de texto. Son las fuentes estandar de PDF: no se
 * incrustan, todos los lectores las tienen y cubren el espanol (tildes, ñ, ¿, ¡, €).
 */
public enum NwavFuente {
    NWAV_HELVETICA("Helvetica (sin serifa)"),
    NWAV_TIMES("Times (con serifa)"),
    NWAV_COURIER("Courier (monoespaciada)");

    private final String nwavDescripcion;

    NwavFuente(String nwavDescripcion) {
        this.nwavDescripcion = nwavDescripcion;
    }

    public String getNwavDescripcion() {
        return nwavDescripcion;
    }

    @Override
    public String toString() {
        return nwavDescripcion;
    }
}
