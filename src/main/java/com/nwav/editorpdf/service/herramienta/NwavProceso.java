package com.nwav.editorpdf.service.herramienta;

/** La parte de una herramienta que hace el trabajo (sin JavaFX). */
@FunctionalInterface
public interface NwavProceso {
    NwavResultadoProceso nwavEjecutar(NwavEntradaProceso nwavEntrada) throws Exception;
}
