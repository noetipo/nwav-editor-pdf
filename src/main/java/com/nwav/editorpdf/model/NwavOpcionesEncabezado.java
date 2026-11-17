package com.nwav.editorpdf.model;

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
