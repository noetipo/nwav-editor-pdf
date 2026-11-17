package com.nwav.editorpdf.service;

import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cache LRU de paginas renderizadas, limitada por memoria (bytes de pixeles), no por
 * cantidad de entradas. Es segura entre hilos.
 */
public class NwavCacheRender {

    private record NwavClave(int nwavPagina, int nwavEscalaMilesimas) {
    }

    private final long nwavLimiteBytes;
    private final LinkedHashMap<NwavClave, BufferedImage> nwavEntradas = new LinkedHashMap<>(64, 0.75f, true);
    private long nwavBytesUsados;

    public NwavCacheRender(long nwavLimiteBytes) {
        this.nwavLimiteBytes = nwavLimiteBytes;
    }

    public synchronized BufferedImage nwavObtener(int nwavPagina, float nwavEscala) {
        return nwavEntradas.get(nwavClave(nwavPagina, nwavEscala));
    }

    public synchronized void nwavGuardar(int nwavPagina, float nwavEscala, BufferedImage nwavImagen) {
        long nwavTamano = nwavBytes(nwavImagen);
        if (nwavTamano > nwavLimiteBytes) {
            return; // Mas grande que toda la cache: no merece la pena guardarla.
        }
        BufferedImage nwavAnterior = nwavEntradas.put(nwavClave(nwavPagina, nwavEscala), nwavImagen);
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

    private static NwavClave nwavClave(int nwavPagina, float nwavEscala) {
        return new NwavClave(nwavPagina, Math.round(nwavEscala * 1000f));
    }

    private static long nwavBytes(BufferedImage nwavImagen) {
        return (long) nwavImagen.getWidth() * nwavImagen.getHeight() * 4L;
    }
}
