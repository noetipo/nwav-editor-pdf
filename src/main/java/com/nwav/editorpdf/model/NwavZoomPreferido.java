package com.nwav.editorpdf.model;

public record NwavZoomPreferido(NwavModoZoom nwavModo, double nwavFactor) {
    public static final double NWAV_FACTOR_MINIMO = 0.10;
    public static final double NWAV_FACTOR_MAXIMO = 8.00;
    public static final NwavZoomPreferido NWAV_POR_DEFECTO =
            new NwavZoomPreferido(NwavModoZoom.NWAV_AJUSTAR_ANCHO, 1.0);

    public NwavZoomPreferido {
        if (nwavModo == null) {
            nwavModo = NwavModoZoom.NWAV_AJUSTAR_ANCHO;
        }
        if (Double.isNaN(nwavFactor) || nwavFactor <= 0) {
            nwavFactor = 1.0;
        }
        nwavFactor = Math.max(NWAV_FACTOR_MINIMO, Math.min(NWAV_FACTOR_MAXIMO, nwavFactor));
    }

    public String nwavSerializar() {
        return nwavModo == NwavModoZoom.NWAV_PORCENTAJE ? nwavModo.name() + ":" + nwavFactor : nwavModo.name();
    }

    public static NwavZoomPreferido nwavDeserializar(String nwavTexto) {
        if (nwavTexto == null || nwavTexto.isBlank()) {
            return NWAV_POR_DEFECTO;
        }
        try {
            String[] nwavPartes = nwavTexto.split(":", 2);
            NwavModoZoom nwavModoLeido = NwavModoZoom.valueOf(nwavPartes[0].trim());
            double nwavFactorLeido = nwavPartes.length > 1 ? Double.parseDouble(nwavPartes[1]) : 1.0;
            return new NwavZoomPreferido(nwavModoLeido, nwavFactorLeido);
        } catch (RuntimeException nwavError) {
            return NWAV_POR_DEFECTO;
        }
    }
}
