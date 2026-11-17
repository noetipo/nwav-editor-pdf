package com.nwav.editorpdf.service.comando;

import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.NwavDocumentoPdf;
import com.nwav.editorpdf.service.NwavPaginaService;

import java.nio.file.Path;
import java.util.List;

public class NwavComandoInsertarImagenes extends NwavComandoEstructural {

    private final List<Path> nwavImagenes;
    private final int nwavPosicion;

    public NwavComandoInsertarImagenes(NwavPaginaService nwavServicio, NwavDocumentoPdf nwavDocumento,
                                       List<Path> nwavImagenes, int nwavPosicion) {
        super(nwavServicio, nwavDocumento);
        this.nwavImagenes = List.copyOf(nwavImagenes);
        this.nwavPosicion = nwavPosicion;
    }

    @Override
    protected int nwavAplicar() {
        return nwavServicio.nwavInsertarImagenes(nwavDocumento, nwavImagenes, nwavPosicion);
    }

    public List<Path> getNwavImagenes() {
        return nwavImagenes;
    }

    @Override
    public NwavTipoOperacion nwavTipo() {
        return NwavTipoOperacion.NWAV_INSERTAR_IMAGENES;
    }

    @Override
    public String nwavDescripcion() {
        return "Insertar " + (nwavImagenes.size() == 1 ? "1 imagen" : nwavImagenes.size() + " imágenes")
                + " en la posición " + (nwavPaginaDestacada() + 1);
    }
}
