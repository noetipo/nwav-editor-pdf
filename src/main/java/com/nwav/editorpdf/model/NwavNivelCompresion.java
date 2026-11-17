package com.nwav.editorpdf.model;

public enum NwavNivelCompresion {
    NWAV_BAJA("Baja compresión (máxima calidad)", 220, 0.85f),
    NWAV_RECOMENDADA("Recomendada", 150, 0.72f),
    NWAV_EXTREMA("Extrema (menor tamaño)", 96, 0.5f),
    NWAV_PERSONALIZADA("Personalizada", 150, 0.75f);

    private final String nwavDescripcion;
    private final int nwavDpi;
    private final float nwavCalidad;

    NwavNivelCompresion(String nwavDescripcion, int nwavDpi, float nwavCalidad) {
        this.nwavDescripcion = nwavDescripcion;
        this.nwavDpi = nwavDpi;
        this.nwavCalidad = nwavCalidad;
    }

    public String getNwavDescripcion() {
        return nwavDescripcion;
    }

    public int getNwavDpi() {
        return nwavDpi;
    }

    public float getNwavCalidad() {
        return nwavCalidad;
    }
}
