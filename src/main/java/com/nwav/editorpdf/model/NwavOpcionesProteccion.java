package com.nwav.editorpdf.model;

public record NwavOpcionesProteccion(String nwavUsuario, String nwavPropietario, boolean nwavImprimir,
                                     boolean nwavCopiar, boolean nwavModificar, boolean nwavAnotar,
                                     boolean nwavRellenarFormularios) {
    public boolean nwavTienePropietario() {
        return nwavPropietario != null && !nwavPropietario.isEmpty();
    }

    @Override
    public String toString() {
        return "NwavOpcionesProteccion[permisos restringidos=" + nwavTienePropietario() + "]";
    }
}
