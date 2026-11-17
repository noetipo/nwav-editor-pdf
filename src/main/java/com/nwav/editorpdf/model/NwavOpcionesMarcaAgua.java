package com.nwav.editorpdf.model;

/**
 * Marca de agua a aplicar. Para {@link NwavTipoMarcaAgua#NWAV_IMAGEN}, {@code nwavImagen}
 * contiene la imagen (PNG/JPG) y {@code nwavEscala} es la fraccion del ancho de la pagina.
 *
 * @param nwavOpacidad entre 0.05 y 1
 * @param nwavRotacion grados en sentido antihorario
 * @param nwavPaginas  rango de paginas ("1-3, 5"); vacio = todas
 */
public record NwavOpcionesMarcaAgua(NwavTipoMarcaAgua nwavTipo, String nwavTexto, byte[] nwavImagen,
                                    NwavFuente nwavFuente, double nwavTamano, NwavColorRgb nwavColor,
                                    double nwavOpacidad, double nwavRotacion, double nwavEscala, String nwavPaginas) {

    public NwavOpcionesMarcaAgua {
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
