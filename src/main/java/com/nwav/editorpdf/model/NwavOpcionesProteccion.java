package com.nwav.editorpdf.model;

/**
 * Proteccion a aplicar al guardar. {@code nwavPropietario} vacio significa "igual que la de
 * apertura" (y entonces los permisos no restringen a quien conoce la contrasena).
 */
public record NwavOpcionesProteccion(String nwavUsuario, String nwavPropietario, boolean nwavImprimir,
                                     boolean nwavCopiar, boolean nwavModificar, boolean nwavAnotar,
                                     boolean nwavRellenarFormularios) {

    public boolean nwavTienePropietario() {
        return nwavPropietario != null && !nwavPropietario.isEmpty();
    }

    @Override
    public String toString() {
        // Nunca mostrar contrasenas en registros.
        return "NwavOpcionesProteccion[permisos restringidos=" + nwavTienePropietario() + "]";
    }
}
