package com.nwav.editorpdf.service;

import org.apache.pdfbox.pdmodel.PDPage;

import java.util.List;

/**
 * Foto del orden de paginas de un documento. Es opaca fuera del paquete: los comandos la
 * guardan para deshacer y rehacer sin conocer los objetos de PDFBox. Las paginas quitadas
 * siguen referenciadas aqui, por eso se pueden volver a insertar tal cual.
 */
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

    /** Mismo orden y mismas paginas (PDPage compara por su diccionario). */
    @Override
    public boolean equals(Object nwavOtro) {
        return nwavOtro instanceof NwavOrdenPaginas nwavOrden && nwavOrden.nwavPaginas.equals(nwavPaginas);
    }

    @Override
    public int hashCode() {
        return nwavPaginas.hashCode();
    }
}
