package com.nwav.editorpdf.model;

/**
 * Opciones de compresion. Solo se tocan las imagenes (texto y vectores quedan intactos).
 *
 * @param nwavDpi           resolucion maxima de las imagenes
 * @param nwavCalidad       calidad JPEG (0.1 - 0.95)
 * @param nwavGrises        convertir imagenes y colores a escala de grises
 * @param nwavQuitarMetadatos quitar metadatos (Info/XMP) y miniaturas incrustadas
 */
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
