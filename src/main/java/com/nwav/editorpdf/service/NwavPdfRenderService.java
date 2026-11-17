package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Dibuja paginas como imagenes. Es sincrono y no depende de JavaFX: la interfaz lo
 * invoca desde tareas en segundo plano. Las imagenes se guardan en las caches del
 * propio documento, que se liberan al cerrarlo.
 */
@Service
public class NwavPdfRenderService {

    /** Limite de pixeles por imagen para no agotar la memoria con zooms extremos. */
    public static final long NWAV_MAXIMO_PIXELES = 36_000_000L;
    public static final float NWAV_ESCALA_MINIMA = 0.02f;

    /**
     * Renderiza una pagina.
     *
     * @param nwavEscala pixeles por punto PDF (1 = 72 ppp).
     */
    public BufferedImage nwavRenderizarPagina(NwavDocumentoPdf nwavDocumento, int nwavIndice, float nwavEscala) {
        return nwavRenderizar(nwavDocumento, nwavIndice, nwavEscala, nwavDocumento.nwavCachePaginas());
    }

    /** Renderiza una miniatura con el ancho indicado en pixeles. */
    public BufferedImage nwavRenderizarMiniatura(NwavDocumentoPdf nwavDocumento, int nwavIndice, int nwavAnchoPx) {
        float nwavAncho = nwavDocumento.nwavTamano(nwavIndice).nwavAncho();
        float nwavEscala = nwavAnchoPx / Math.max(1f, nwavAncho);
        return nwavRenderizar(nwavDocumento, nwavIndice, nwavEscala, nwavDocumento.nwavCacheMiniaturas());
    }

    /** Escala efectiva tras aplicar los limites de memoria. */
    public float nwavEscalaSegura(NwavDocumentoPdf nwavDocumento, int nwavIndice, float nwavEscala) {
        NwavDocumentoPdf.NwavTamanoPagina nwavTamano = nwavDocumento.nwavTamano(nwavIndice);
        double nwavPixeles = (double) nwavTamano.nwavAncho() * nwavTamano.nwavAlto() * nwavEscala * nwavEscala;
        float nwavResultado = nwavEscala;
        if (nwavPixeles > NWAV_MAXIMO_PIXELES) {
            nwavResultado = (float) (nwavEscala * Math.sqrt(NWAV_MAXIMO_PIXELES / nwavPixeles));
        }
        return Math.max(NWAV_ESCALA_MINIMA, nwavResultado);
    }

    private BufferedImage nwavRenderizar(NwavDocumentoPdf nwavDocumento, int nwavIndice, float nwavEscala,
                                         NwavCacheRender nwavCache) {
        if (nwavIndice < 0 || nwavIndice >= nwavDocumento.getNwavNumeroPaginas()) {
            throw new IllegalArgumentException("Página fuera de rango: " + (nwavIndice + 1));
        }
        float nwavSegura = nwavEscalaSegura(nwavDocumento, nwavIndice, nwavEscala);
        synchronized (nwavDocumento) {
            nwavDocumento.nwavVerificarAbierto();
            PDPage nwavPagina = nwavDocumento.nwavPdDocument().getPage(nwavIndice);
            Object nwavIdentidad = nwavPagina.getCOSObject();
            int nwavRotacion = NwavDocumentoPdf.nwavRotacionNormalizada(nwavPagina);
            BufferedImage nwavGuardada = nwavCache.nwavObtener(nwavIdentidad, nwavRotacion, nwavSegura);
            if (nwavGuardada != null) {
                return nwavGuardada;
            }
            try {
                BufferedImage nwavImagen = nwavDocumento.nwavRenderizador()
                        .renderImage(nwavIndice, nwavSegura, ImageType.RGB);
                nwavCache.nwavGuardar(nwavIdentidad, nwavRotacion, nwavSegura, nwavImagen);
                return nwavImagen;
            } catch (IOException nwavError) {
                throw new NwavEditorException("No se pudo dibujar la página " + (nwavIndice + 1)
                        + " de «" + nwavDocumento.getNwavNombre() + "». El archivo podría estar dañado.", nwavError);
            }
        }
    }
}
