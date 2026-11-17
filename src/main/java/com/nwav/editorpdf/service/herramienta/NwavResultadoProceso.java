package com.nwav.editorpdf.service.herramienta;

import java.nio.file.Path;
import java.util.List;

/**
 * Resultado de procesar uno o varios archivos.
 *
 * @param nwavGenerados archivos creados (puede estar vacio, p. ej. si comprimir no mejoro)
 * @param nwavResumen   texto corto para el usuario
 * @param nwavDetalle   texto para el historial
 */
public record NwavResultadoProceso(List<Path> nwavGenerados, String nwavResumen, String nwavDetalle) {

    public static NwavResultadoProceso nwavDe(Path nwavGenerado, String nwavResumen) {
        return new NwavResultadoProceso(List.of(nwavGenerado), nwavResumen, nwavResumen);
    }
}
