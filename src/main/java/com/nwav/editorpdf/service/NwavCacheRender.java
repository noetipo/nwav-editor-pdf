package com.nwav.editorpdf.service;

import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cache LRU de paginas renderizadas, limitada por memoria (bytes de pixeles), no por
 * cantidad de entradas. Es segura entre hilos.
 * <p>
 * La clave es la identidad de la pagina (su objeto PDF, no su numero), su rotacion, la
 * version de su contenido y la escala: reordenar no invalida las imagenes ya hechas, pero
 * editar el contenido de una pagina si.
 */
public class NwavCacheRender {

    /** {@code nwavPagina} se compara por identidad (los objetos COS no redefinen equals). */
    private record NwavClave(Object nwavPagina, int nwavRotacion, int nwavVersion, int nwavEscalaMilesimas) {
    }

    private final long nwavLimiteBytes;
    private final LinkedHashMap<NwavClave, BufferedImage> nwavEntradas = new LinkedHashMap<>(64, 0.75f, true);
    private long nwavBytesUsados;

    public NwavCacheRender(long nwavLimiteBytes) {
        this.nwavLimiteBytes = nwavLimiteBytes;
    }

    public synchronized BufferedImage nwavObtener(Object nwavPagina, int nwavRotacion, int nwavVersion,
                                               float nwavEscala) {
        return nwavEntradas.get(nwavClave(nwavPagina, nwavRotacion, nwavVersion, nwavEscala));
    }

    public synchronized void nwavGuardar(Object nwavPagina, int nwavRotacion, int nwavVersion, float nwavEscala,
                                         BufferedImage nwavImagen) {
        long nwavTamano = nwavBytes(nwavImagen);
        if (nwavTamano > nwavLimiteBytes) {
            return; // Mas grande que toda la cache: no merece la pena guardarla.
        }
        BufferedImage nwavAnterior = nwavEntradas.put(nwavClave(nwavPagina, nwavRotacion, nwavVersion, nwavEscala), nwavImagen);
        if (nwavAnterior != null) {
            nwavBytesUsados -= nwavBytes(nwavAnterior);
        }
        nwavBytesUsados += nwavTamano;
        Iterator<Map.Entry<NwavClave, BufferedImage>> nwavIterador = nwavEntradas.entrySet().iterator();
        while (nwavBytesUsados > nwavLimiteBytes && nwavIterador.hasNext()) {
            Map.Entry<NwavClave, BufferedImage> nwavMasAntigua = nwavIterador.next();
            nwavBytesUsados -= nwavBytes(nwavMasAntigua.getValue());
            nwavIterador.remove();
        }
    }

    public synchronized void nwavLimpiar() {
        nwavEntradas.clear();
        nwavBytesUsados = 0;
    }

    public synchronized int nwavCantidad() {
        return nwavEntradas.size();
    }

    public synchronized long nwavBytesUsados() {
        return nwavBytesUsados;
    }

    private static NwavClave nwavClave(Object nwavPagina, int nwavRotacion, int nwavVersion, float nwavEscala) {
        return new NwavClave(nwavPagina, nwavRotacion, nwavVersion, Math.round(nwavEscala * 1000f));
    }

    private static long nwavBytes(BufferedImage nwavImagen) {
        return (long) nwavImagen.getWidth() * nwavImagen.getHeight() * 4L;
    }
}
