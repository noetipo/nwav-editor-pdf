package com.nwav.editorpdf.model;

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
