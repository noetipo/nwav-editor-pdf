package com.nwav.editorpdf.service.comando;

import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.NwavContenidoService;
import com.nwav.editorpdf.service.NwavDocumentoPdf;
import com.nwav.editorpdf.service.NwavInstantaneaPagina;

public class NwavComandoContenido implements NwavComando {
    private final NwavContenidoService nwavServicio;
    private final NwavDocumentoPdf nwavDocumento;
    private final int nwavPagina;
    private final NwavTipoOperacion nwavTipo;
    private final String nwavDescripcion;
    private final Runnable nwavAccion;
    private NwavInstantaneaPagina nwavAntes;
    private NwavInstantaneaPagina nwavDespues;

    public NwavComandoContenido(NwavContenidoService nwavServicio, NwavDocumentoPdf nwavDocumento, int nwavPagina,
                                NwavTipoOperacion nwavTipo, String nwavDescripcion, Runnable nwavAccion) {
        this.nwavServicio = nwavServicio;
        this.nwavDocumento = nwavDocumento;
        this.nwavPagina = nwavPagina;
        this.nwavTipo = nwavTipo;
        this.nwavDescripcion = nwavDescripcion;
        this.nwavAccion = nwavAccion;
    }

    @Override
    public void nwavEjecutar() {
        synchronized (nwavDocumento) {
            if (nwavDespues != null) {
                nwavServicio.nwavRestaurar(nwavDocumento, nwavDespues);
                return;
            }
            nwavAntes = nwavServicio.nwavCapturar(nwavDocumento, nwavPagina);
            try {
                nwavAccion.run();
            } catch (RuntimeException nwavError) {
                nwavServicio.nwavRestaurar(nwavDocumento, nwavAntes);
                throw nwavError;
            }
            nwavDespues = nwavServicio.nwavCapturar(nwavDocumento, nwavPagina);
        }
    }

    @Override
    public void nwavDeshacer() {
        nwavServicio.nwavRestaurar(nwavDocumento, nwavAntes);
    }

    @Override
    public NwavTipoOperacion nwavTipo() {
        return nwavTipo;
    }

    @Override
    public String nwavDescripcion() {
        return nwavDescripcion + " en la página " + (nwavPagina + 1);
    }

    @Override
    public int nwavPaginaDestacada() {
        return nwavPagina;
    }

    @Override
    public boolean nwavCambiaEstructura() {
        return false;
    }
}
