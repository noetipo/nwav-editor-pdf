package com.nwav.editorpdf.service.herramienta;

import java.nio.file.Path;
import java.util.List;

public record NwavResultadoArchivo(Path nwavOrigen, List<Path> nwavGenerados, String nwavResumen, Throwable nwavError) {
    public boolean nwavExito() {
        return nwavError == null;
    }
}
