package com.nwav.editorpdf.service.comando;

import com.nwav.editorpdf.service.NwavDocumentoPdf;
import com.nwav.editorpdf.service.NwavOrdenPaginas;
import com.nwav.editorpdf.service.NwavPaginaService;

public abstract class NwavComandoEstructural implements NwavComando {
    protected final NwavPaginaService nwavServicio;
    protected final NwavDocumentoPdf nwavDocumento;
    private NwavOrdenPaginas nwavAntes;
    private NwavOrdenPaginas nwavDespues;
    private int nwavDestacada = -1;

    protected NwavComandoEstructural(NwavPaginaService nwavServicio, NwavDocumentoPdf nwavDocumento) {
        this.nwavServicio = nwavServicio;
        this.nwavDocumento = nwavDocumento;
    }

    protected abstract int nwavAplicar();

    @Override
    public final void nwavEjecutar() {
        synchronized (nwavDocumento) {
            if (nwavDespues == null) {
                nwavAntes = nwavServicio.nwavCapturarOrden(nwavDocumento);
                nwavDestacada = nwavAplicar();
                nwavDespues = nwavServicio.nwavCapturarOrden(nwavDocumento);
            } else {
                nwavServicio.nwavRestaurarOrden(nwavDocumento, nwavDespues);
            }
        }
    }

    @Override
    public final void nwavDeshacer() {
        nwavServicio.nwavRestaurarOrden(nwavDocumento, nwavAntes);
    }

    @Override
    public int nwavPaginaDestacada() {
        return nwavDestacada;
    }

    @Override
    public boolean nwavTuvoEfecto() {
        return nwavAntes != null && !nwavAntes.equals(nwavDespues);
    }

    protected static String nwavPaginas(int nwavCantidad) {
        return nwavCantidad == 1 ? "1 página" : nwavCantidad + " páginas";
    }
}
