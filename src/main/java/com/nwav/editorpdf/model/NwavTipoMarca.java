package com.nwav.editorpdf.model;

/** Marcas sobre texto (anotaciones de marcado). */
public enum NwavTipoMarca {
    NWAV_RESALTADO("Resaltado"),
    NWAV_SUBRAYADO("Subrayado"),
    NWAV_TACHADO("Tachado");

    private final String nwavDescripcion;

    NwavTipoMarca(String nwavDescripcion) {
        this.nwavDescripcion = nwavDescripcion;
    }

    public String getNwavDescripcion() {
        return nwavDescripcion;
    }
}
