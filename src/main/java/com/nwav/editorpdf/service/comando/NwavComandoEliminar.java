package com.nwav.editorpdf.service.comando;

import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.NwavDocumentoPdf;
import com.nwav.editorpdf.service.NwavPaginaService;
import com.nwav.editorpdf.service.NwavRangosPaginas;

import java.util.Collections;
import java.util.List;

public class NwavComandoEliminar extends NwavComandoEstructural {

    private final List<Integer> nwavIndices;

    public NwavComandoEliminar(NwavPaginaService nwavServicio, NwavDocumentoPdf nwavDocumento,
                               List<Integer> nwavIndices) {
        super(nwavServicio, nwavDocumento);
        this.nwavIndices = List.copyOf(nwavIndices);
    }

    @Override
    protected int nwavAplicar() {
        nwavServicio.nwavEliminar(nwavDocumento, nwavIndices);
        return Math.min(Collections.min(nwavIndices), nwavDocumento.getNwavNumeroPaginas() - 1);
    }

    @Override
    public NwavTipoOperacion nwavTipo() {
        return NwavTipoOperacion.NWAV_ELIMINAR;
    }

    @Override
    public String nwavDescripcion() {
        return "Eliminar " + nwavPaginas(nwavIndices.size()) + " (" + NwavRangosPaginas.nwavDescribir(nwavIndices) + ")";
    }
}
