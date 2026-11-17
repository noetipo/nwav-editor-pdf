package com.nwav.editorpdf.model;

import java.util.Locale;

/** Color RGB (0-255) independiente de JavaFX y de PDFBox. */
public record NwavColorRgb(int nwavRojo, int nwavVerde, int nwavAzul) {

    public static final NwavColorRgb NWAV_NEGRO = new NwavColorRgb(0, 0, 0);
    public static final NwavColorRgb NWAV_BLANCO = new NwavColorRgb(255, 255, 255);
    public static final NwavColorRgb NWAV_AMARILLO = new NwavColorRgb(255, 221, 0);
    public static final NwavColorRgb NWAV_ROJO = new NwavColorRgb(220, 38, 38);

    public NwavColorRgb {
        nwavRojo = Math.max(0, Math.min(255, nwavRojo));
        nwavVerde = Math.max(0, Math.min(255, nwavVerde));
        nwavAzul = Math.max(0, Math.min(255, nwavAzul));
    }

    /** Componentes entre 0 y 1, como los usa PDF. */
    public float[] nwavComponentes() {
        return new float[]{nwavRojo / 255f, nwavVerde / 255f, nwavAzul / 255f};
    }

    public String nwavHex() {
        return String.format(Locale.ROOT, "#%02X%02X%02X", nwavRojo, nwavVerde, nwavAzul);
    }
}
