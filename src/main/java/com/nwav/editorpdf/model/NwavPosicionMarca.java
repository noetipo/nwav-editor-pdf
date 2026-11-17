package com.nwav.editorpdf.model;

/** Donde se coloca la marca de agua en cada pagina (o repetida en mosaico). */
public enum NwavPosicionMarca {
    NWAV_CENTRO("Centro", 0.5, 0.5),
    NWAV_ARRIBA_IZQUIERDA("Arriba a la izquierda", 0, 0),
    NWAV_ARRIBA("Arriba en el centro", 0.5, 0),
    NWAV_ARRIBA_DERECHA("Arriba a la derecha", 1, 0),
    NWAV_IZQUIERDA("Centro a la izquierda", 0, 0.5),
    NWAV_DERECHA("Centro a la derecha", 1, 0.5),
    NWAV_ABAJO_IZQUIERDA("Abajo a la izquierda", 0, 1),
    NWAV_ABAJO("Abajo en el centro", 0.5, 1),
    NWAV_ABAJO_DERECHA("Abajo a la derecha", 1, 1),
    NWAV_MOSAICO("Mosaico (repetida por toda la página)", 0.5, 0.5);

    private final String nwavDescripcion;
    private final double nwavFraccionX;
    private final double nwavFraccionY;

    NwavPosicionMarca(String nwavDescripcion, double nwavFraccionX, double nwavFraccionY) {
        this.nwavDescripcion = nwavDescripcion;
        this.nwavFraccionX = nwavFraccionX;
        this.nwavFraccionY = nwavFraccionY;
    }

    public String getNwavDescripcion() {
        return nwavDescripcion;
    }

    /** 0 = izquierda, 0.5 = centro, 1 = derecha. */
    public double getNwavFraccionX() {
        return nwavFraccionX;
    }

    /** 0 = arriba, 0.5 = centro, 1 = abajo (visual). */
    public double getNwavFraccionY() {
        return nwavFraccionY;
    }

    @Override
    public String toString() {
        return nwavDescripcion;
    }
}
