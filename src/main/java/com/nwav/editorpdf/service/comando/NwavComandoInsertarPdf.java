package com.nwav.editorpdf.service.comando;

import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.NwavDocumentoPdf;
import com.nwav.editorpdf.service.NwavPaginaService;

import java.nio.file.Path;

public class NwavComandoInsertarPdf extends NwavComandoEstructural {

    private final Path nwavOrigen;
    private final String nwavContrasena;
    private final String nwavRangos;
    private final int nwavPosicion;
    private int nwavInsertadas;

    public NwavComandoInsertarPdf(NwavPaginaService nwavServicio, NwavDocumentoPdf nwavDocumento, Path nwavOrigen,
                                  String nwavContrasena, String nwavRangos, int nwavPosicion) {
        super(nwavServicio, nwavDocumento);
        this.nwavOrigen = nwavOrigen;
        this.nwavContrasena = nwavContrasena;
        this.nwavRangos = nwavRangos;
        this.nwavPosicion = nwavPosicion;
    }

    @Override
    protected int nwavAplicar() {
        int nwavAntes = nwavDocumento.getNwavNumeroPaginas();
        int nwavDonde = nwavServicio.nwavInsertarPdf(nwavDocumento, nwavOrigen, nwavContrasena, nwavRangos, nwavPosicion);
        nwavInsertadas = nwavDocumento.getNwavNumeroPaginas() - nwavAntes;
        return nwavDonde;
    }

    public Path getNwavOrigen() {
        return nwavOrigen;
    }

    @Override
    public NwavTipoOperacion nwavTipo() {
        return NwavTipoOperacion.NWAV_INSERTAR_PDF;
    }

    @Override
    public String nwavDescripcion() {
        return "Insertar " + nwavPaginas(nwavInsertadas) + " de «" + nwavOrigen.getFileName()
                + "» en la posición " + (nwavPaginaDestacada() + 1);
    }
}
