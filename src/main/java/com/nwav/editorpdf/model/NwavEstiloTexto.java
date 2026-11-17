package com.nwav.editorpdf.model;

public record NwavEstiloTexto(NwavFuente nwavFuente, double nwavTamano, boolean nwavNegrita, boolean nwavCursiva,
                              NwavColorRgb nwavColor) {
    public static final double NWAV_TAMANO_MINIMO = 4;
    public static final double NWAV_TAMANO_MAXIMO = 200;

    public NwavEstiloTexto {
        if (nwavFuente == null) {
            nwavFuente = NwavFuente.NWAV_HELVETICA;
        }
        if (nwavColor == null) {
            nwavColor = NwavColorRgb.NWAV_NEGRO;
        }
        nwavTamano = Math.max(NWAV_TAMANO_MINIMO, Math.min(NWAV_TAMANO_MAXIMO, nwavTamano));
    }
}
