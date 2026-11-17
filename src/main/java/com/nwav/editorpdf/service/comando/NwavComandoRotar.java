package com.nwav.editorpdf.service.comando;

import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.NwavDocumentoPdf;
import com.nwav.editorpdf.service.NwavPaginaService;
import com.nwav.editorpdf.service.NwavRangosPaginas;

import java.util.List;

public class NwavComandoRotar implements NwavComando {

    private final NwavPaginaService nwavServicio;
    private final NwavDocumentoPdf nwavDocumento;
    private final List<Integer> nwavIndices;
    private final int nwavGrados;

    public NwavComandoRotar(NwavPaginaService nwavServicio, NwavDocumentoPdf nwavDocumento,
                            List<Integer> nwavIndices, int nwavGrados) {
        this.nwavServicio = nwavServicio;
        this.nwavDocumento = nwavDocumento;
        this.nwavIndices = List.copyOf(nwavIndices);
        this.nwavGrados = nwavGrados;
    }

    @Override
    public void nwavEjecutar() {
        nwavServicio.nwavRotar(nwavDocumento, nwavIndices, nwavGrados);
    }

    @Override
    public void nwavDeshacer() {
        nwavServicio.nwavRotar(nwavDocumento, nwavIndices, -nwavGrados);
    }

    @Override
    public NwavTipoOperacion nwavTipo() {
        return NwavTipoOperacion.NWAV_ROTAR;
    }

    @Override
    public String nwavDescripcion() {
        String nwavSentido = nwavGrados == 180 || nwavGrados == -180 ? "180°"
                : nwavGrados > 0 ? "90° a la derecha" : "90° a la izquierda";
        return "Rotar " + (nwavIndices.size() == 1 ? "página " : "páginas ")
                + NwavRangosPaginas.nwavDescribir(nwavIndices) + " " + nwavSentido;
    }

    @Override
    public int nwavPaginaDestacada() {
        return nwavIndices.get(0);
    }

    @Override
    public boolean nwavTuvoEfecto() {
        return nwavGrados % 360 != 0;
    }
}
