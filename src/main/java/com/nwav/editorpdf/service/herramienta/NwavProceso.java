package com.nwav.editorpdf.service.herramienta;

@FunctionalInterface
public interface NwavProceso {
    NwavResultadoProceso nwavEjecutar(NwavEntradaProceso nwavEntrada) throws Exception;
}
