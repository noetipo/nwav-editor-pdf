package com.nwav.editorpdf.service.comando;

import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.NwavDocumentoPdf;
import com.nwav.editorpdf.service.NwavPaginaService;
import com.nwav.editorpdf.service.NwavRangosPaginas;

import java.util.List;

public class NwavComandoDuplicar extends NwavComandoEstructural {

    private final List<Integer> nwavIndices;

    public NwavComandoDuplicar(NwavPaginaService nwavServicio, NwavDocumentoPdf nwavDocumento,
                               List<Integer> nwavIndices) {
        super(nwavServicio, nwavDocumento);
        this.nwavIndices = List.copyOf(nwavIndices);
    }

    @Override
    protected int nwavAplicar() {
        return nwavServicio.nwavDuplicar(nwavDocumento, nwavIndices);
    }

    @Override
    public NwavTipoOperacion nwavTipo() {
        return NwavTipoOperacion.NWAV_DUPLICAR;
    }

    @Override
    public String nwavDescripcion() {
        return "Duplicar " + nwavPaginas(nwavIndices.size()) + " (" + NwavRangosPaginas.nwavDescribir(nwavIndices) + ")";
    }
}
