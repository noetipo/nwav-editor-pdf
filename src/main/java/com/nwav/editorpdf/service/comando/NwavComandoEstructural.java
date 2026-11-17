package com.nwav.editorpdf.service.comando;

import com.nwav.editorpdf.service.NwavDocumentoPdf;
import com.nwav.editorpdf.service.NwavOrdenPaginas;
import com.nwav.editorpdf.service.NwavPaginaService;

/**
 * Comando que cambia el orden o el conjunto de paginas. La primera ejecucion aplica la
 * operacion y guarda una foto del orden antes y despues; deshacer y rehacer solo
 * restauran la foto correspondiente, sin repetir la operacion.
 */
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

    /** Aplica la operacion y devuelve la pagina a destacar. */
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
