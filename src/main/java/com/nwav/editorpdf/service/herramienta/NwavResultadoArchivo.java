package com.nwav.editorpdf.service.herramienta;

import java.nio.file.Path;
import java.util.List;

/**
 * Resultado de un archivo dentro de un lote. {@code nwavOrigen} es null cuando la
 * herramienta combina todos los archivos (p. ej. unir).
 */
public record NwavResultadoArchivo(Path nwavOrigen, List<Path> nwavGenerados, String nwavResumen, Throwable nwavError) {

    public boolean nwavExito() {
        return nwavError == null;
    }
}
