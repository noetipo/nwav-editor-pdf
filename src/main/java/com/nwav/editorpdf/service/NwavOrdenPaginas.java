package com.nwav.editorpdf.service;

import org.apache.pdfbox.pdmodel.PDPage;

import java.util.List;

public final class NwavOrdenPaginas {
    private final List<PDPage> nwavPaginas;

    NwavOrdenPaginas(List<PDPage> nwavPaginas) {
        this.nwavPaginas = List.copyOf(nwavPaginas);
    }

    List<PDPage> nwavPaginas() {
        return nwavPaginas;
    }

    public int nwavCantidad() {
        return nwavPaginas.size();
    }

    @Override
    public boolean equals(Object nwavOtro) {
        return nwavOtro instanceof NwavOrdenPaginas nwavOrden && nwavOrden.nwavPaginas.equals(nwavPaginas);
    }

    @Override
    public int hashCode() {
        return nwavPaginas.hashCode();
    }
}
