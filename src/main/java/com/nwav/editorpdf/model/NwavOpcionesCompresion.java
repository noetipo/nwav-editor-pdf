package com.nwav.editorpdf.model;

public record NwavOpcionesCompresion(int nwavDpi, float nwavCalidad, boolean nwavGrises, boolean nwavQuitarMetadatos) {
    public NwavOpcionesCompresion {
        nwavDpi = Math.max(36, Math.min(600, nwavDpi));
        nwavCalidad = Math.max(0.1f, Math.min(0.95f, nwavCalidad));
    }

    public static NwavOpcionesCompresion nwavDe(NwavNivelCompresion nwavNivel, boolean nwavGrises,
                                                boolean nwavQuitarMetadatos) {
        return new NwavOpcionesCompresion(nwavNivel.getNwavDpi(), nwavNivel.getNwavCalidad(), nwavGrises,
                nwavQuitarMetadatos || nwavNivel == NwavNivelCompresion.NWAV_EXTREMA);
    }
}
