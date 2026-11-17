package com.nwav.editorpdf.service.comando;

import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.NwavContenidoService;
import com.nwav.editorpdf.service.NwavDocumentoPdf;
import com.nwav.editorpdf.service.NwavInstantaneaPagina;

import java.util.ArrayList;
import java.util.List;

/**
 * Como {@link NwavComandoContenido}, pero para una operacion que cambia el contenido de
 * varias paginas a la vez (marca de agua, numeracion…): guarda una foto de cada una.
 */
public class NwavComandoVariasPaginas implements NwavComando {

    private final NwavContenidoService nwavServicio;
    private final NwavDocumentoPdf nwavDocumento;
    private final List<Integer> nwavPaginas;
    private final NwavTipoOperacion nwavTipo;
    private final String nwavDescripcion;
    private final Runnable nwavAccion;
    private List<NwavInstantaneaPagina> nwavAntes;
    private List<NwavInstantaneaPagina> nwavDespues;

    public NwavComandoVariasPaginas(NwavContenidoService nwavServicio, NwavDocumentoPdf nwavDocumento,
                                    List<Integer> nwavPaginas, NwavTipoOperacion nwavTipo, String nwavDescripcion,
                                    Runnable nwavAccion) {
        this.nwavServicio = nwavServicio;
        this.nwavDocumento = nwavDocumento;
        this.nwavPaginas = List.copyOf(nwavPaginas);
        this.nwavTipo = nwavTipo;
        this.nwavDescripcion = nwavDescripcion;
        this.nwavAccion = nwavAccion;
    }

    @Override
    public void nwavEjecutar() {
        synchronized (nwavDocumento) {
            if (nwavDespues != null) {
                nwavRestaurar(nwavDespues);
                return;
            }
            nwavAntes = nwavCapturar();
            try {
                nwavAccion.run();
            } catch (RuntimeException nwavError) {
                nwavRestaurar(nwavAntes);
                throw nwavError;
            }
            nwavDespues = nwavCapturar();
        }
    }

    @Override
    public void nwavDeshacer() {
        synchronized (nwavDocumento) {
            nwavRestaurar(nwavAntes);
        }
    }

    private List<NwavInstantaneaPagina> nwavCapturar() {
        List<NwavInstantaneaPagina> nwavFotos = new ArrayList<>();
        for (int nwavPagina : nwavPaginas) {
            nwavFotos.add(nwavServicio.nwavCapturar(nwavDocumento, nwavPagina));
        }
        return nwavFotos;
    }

    private void nwavRestaurar(List<NwavInstantaneaPagina> nwavFotos) {
        nwavFotos.forEach(nwavFoto -> nwavServicio.nwavRestaurar(nwavDocumento, nwavFoto));
    }

    @Override
    public NwavTipoOperacion nwavTipo() {
        return nwavTipo;
    }

    @Override
    public String nwavDescripcion() {
        return nwavDescripcion + (nwavPaginas.size() == 1 ? " (1 página)" : " (" + nwavPaginas.size() + " páginas)");
    }

    @Override
    public boolean nwavCambiaEstructura() {
        return false;
    }
}
