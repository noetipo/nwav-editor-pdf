package com.nwav.editorpdf.service.herramienta;

import java.nio.file.Path;
import java.util.List;

public record NwavResultadoProceso(List<Path> nwavGenerados, String nwavResumen, String nwavDetalle) {
    public static NwavResultadoProceso nwavDe(Path nwavGenerado, String nwavResumen) {
        return new NwavResultadoProceso(List.of(nwavGenerado), nwavResumen, nwavResumen);
    }
}
