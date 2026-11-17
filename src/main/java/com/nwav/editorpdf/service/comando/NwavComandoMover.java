package com.nwav.editorpdf.service.comando;

import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.NwavDocumentoPdf;
import com.nwav.editorpdf.service.NwavPaginaService;
import com.nwav.editorpdf.service.NwavRangosPaginas;

import java.util.List;

public class NwavComandoMover extends NwavComandoEstructural {
    private final List<Integer> nwavIndices;
    private final int nwavDestino;

    public NwavComandoMover(NwavPaginaService nwavServicio, NwavDocumentoPdf nwavDocumento,
                            List<Integer> nwavIndices, int nwavDestino) {
        super(nwavServicio, nwavDocumento);
        this.nwavIndices = List.copyOf(nwavIndices);
        this.nwavDestino = nwavDestino;
    }

    @Override
    protected int nwavAplicar() {
        return nwavServicio.nwavMover(nwavDocumento, nwavIndices, nwavDestino);
    }

    @Override
    public NwavTipoOperacion nwavTipo() {
        return NwavTipoOperacion.NWAV_REORDENAR;
    }

    @Override
    public String nwavDescripcion() {
        return "Mover " + (nwavIndices.size() == 1 ? "página " : "páginas ")
                + NwavRangosPaginas.nwavDescribir(nwavIndices) + " a la posición " + (nwavPaginaDestacada() + 1);
    }
}
