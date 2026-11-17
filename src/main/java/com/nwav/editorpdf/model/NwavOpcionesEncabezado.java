package com.nwav.editorpdf.model;

/**
 * Encabezado, pie y numeracion. Los textos admiten {n} (numero de pagina), {total},
 * {archivo} y {fecha}. Un texto vacio no se dibuja.
 *
 * @param nwavNumeroInicial numero que lleva la primera pagina del documento
 * @param nwavMargen        distancia al borde, en puntos
 * @param nwavPaginas       rango de paginas ("2-"); vacio = todas
 */
public record NwavOpcionesEncabezado(String nwavEncabezado, NwavAlineacion nwavAlineacionEncabezado,
                                     String nwavPie, NwavAlineacion nwavAlineacionPie, NwavFuente nwavFuente,
                                     double nwavTamano, NwavColorRgb nwavColor, double nwavMargen,
                                     int nwavNumeroInicial, String nwavPaginas) {

    public NwavOpcionesEncabezado {
        nwavEncabezado = nwavEncabezado == null ? "" : nwavEncabezado.strip();
        nwavPie = nwavPie == null ? "" : nwavPie.strip();
        if (nwavAlineacionEncabezado == null) {
            nwavAlineacionEncabezado = NwavAlineacion.NWAV_CENTRO;
        }
        if (nwavAlineacionPie == null) {
            nwavAlineacionPie = NwavAlineacion.NWAV_CENTRO;
        }
        if (nwavFuente == null) {
            nwavFuente = NwavFuente.NWAV_HELVETICA;
        }
        if (nwavColor == null) {
            nwavColor = NwavColorRgb.NWAV_NEGRO;
        }
        nwavTamano = Math.max(5, Math.min(72, nwavTamano));
        nwavMargen = Math.max(0, Math.min(200, nwavMargen));
    }
}
