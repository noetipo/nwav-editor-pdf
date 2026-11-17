package com.nwav.editorpdf.service.comando;

import com.nwav.editorpdf.model.NwavTipoOperacion;

public interface NwavComando {
    void nwavEjecutar();

    void nwavDeshacer();

    NwavTipoOperacion nwavTipo();

    String nwavDescripcion();

    default int nwavPaginaDestacada() {
        return -1;
    }

    default boolean nwavCambiaEstructura() {
        return true;
    }

    default boolean nwavTuvoEfecto() {
        return true;
    }
}
