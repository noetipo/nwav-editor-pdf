package com.nwav.editorpdf.model;

public record NwavOpcionesMarcaAgua(NwavTipoMarcaAgua nwavTipo, String nwavTexto, byte[] nwavImagen,
                                    NwavFuente nwavFuente, double nwavTamano, NwavColorRgb nwavColor,
                                    double nwavOpacidad, double nwavRotacion, double nwavEscala, String nwavPaginas,
                                    NwavPosicionMarca nwavPosicion) {
    public NwavOpcionesMarcaAgua(NwavTipoMarcaAgua nwavTipo, String nwavTexto, byte[] nwavImagen, NwavFuente nwavFuente,
                                 double nwavTamano, NwavColorRgb nwavColor, double nwavOpacidad, double nwavRotacion,
                                 double nwavEscala, String nwavPaginas) {
        this(nwavTipo, nwavTexto, nwavImagen, nwavFuente, nwavTamano, nwavColor, nwavOpacidad, nwavRotacion, nwavEscala,
                nwavPaginas, NwavPosicionMarca.NWAV_CENTRO);
    }

    public NwavOpcionesMarcaAgua {
        if (nwavPosicion == null) {
            nwavPosicion = NwavPosicionMarca.NWAV_CENTRO;
        }
        if (nwavTipo == null) {
            nwavTipo = NwavTipoMarcaAgua.NWAV_TEXTO;
        }
        if (nwavFuente == null) {
            nwavFuente = NwavFuente.NWAV_HELVETICA;
        }
        if (nwavColor == null) {
            nwavColor = new NwavColorRgb(150, 150, 150);
        }
        nwavTamano = Math.max(8, Math.min(300, nwavTamano));
        nwavOpacidad = Math.max(0.05, Math.min(1, nwavOpacidad));
        nwavEscala = Math.max(0.05, Math.min(1, nwavEscala));
    }
}
