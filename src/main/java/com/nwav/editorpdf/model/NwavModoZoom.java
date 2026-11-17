package com.nwav.editorpdf.model;

/** Como se calcula el zoom del visor. */
public enum NwavModoZoom {
    NWAV_AJUSTAR_ANCHO("Ajustar al ancho"),
    NWAV_AJUSTAR_PAGINA("Ajustar a la página"),
    NWAV_PORCENTAJE("Porcentaje");

    private final String nwavDescripcion;

    NwavModoZoom(String nwavDescripcion) {
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
