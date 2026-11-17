package com.nwav.editorpdf.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Documento abierto en el visor. Envuelve el {@link PDDocument} junto con su renderizador
 * y sus caches; al cerrarlo se libera todo. Se usa con try-with-resources o se cierra
 * explicitamente al cambiar de archivo.
 * <p>
 * PDFBox no es seguro entre hilos: todo acceso al documento debe sincronizarse sobre
 * esta instancia (lo hacen los servicios).
 */
@Slf4j
public class NwavDocumentoPdf implements AutoCloseable {

    /** Tamano visible de una pagina, en puntos PDF, con la rotacion ya aplicada. */
    public record NwavTamanoPagina(float nwavAncho, float nwavAlto) {
    }

    public static final long NWAV_CACHE_PAGINAS_BYTES = 256L * 1024 * 1024;
    public static final long NWAV_CACHE_MINIATURAS_BYTES = 48L * 1024 * 1024;

    private final Path nwavRuta;
    private final PDDocument nwavDocumento;
    private final PDFRenderer nwavRenderizador;
    private final List<NwavTamanoPagina> nwavTamanos;
    private final boolean nwavCifrado;
    private final NwavCacheRender nwavCachePaginas = new NwavCacheRender(NWAV_CACHE_PAGINAS_BYTES);
    private final NwavCacheRender nwavCacheMiniaturas = new NwavCacheRender(NWAV_CACHE_MINIATURAS_BYTES);
    private volatile boolean nwavCerrado;

    NwavDocumentoPdf(Path nwavRuta, PDDocument nwavDocumento) {
        this.nwavRuta = nwavRuta;
        this.nwavDocumento = nwavDocumento;
        this.nwavRenderizador = new PDFRenderer(nwavDocumento);
        this.nwavCifrado = nwavDocumento.isEncrypted();
        List<NwavTamanoPagina> nwavLista = new ArrayList<>(nwavDocumento.getNumberOfPages());
        for (PDPage nwavPagina : nwavDocumento.getPages()) {
            nwavLista.add(nwavCalcularTamano(nwavPagina));
        }
        this.nwavTamanos = Collections.unmodifiableList(nwavLista);
    }

    static NwavTamanoPagina nwavCalcularTamano(PDPage nwavPagina) {
        PDRectangle nwavCaja = nwavPagina.getCropBox();
        int nwavRotacion = ((nwavPagina.getRotation() % 360) + 360) % 360;
        boolean nwavGirada = nwavRotacion == 90 || nwavRotacion == 270;
        return nwavGirada
                ? new NwavTamanoPagina(nwavCaja.getHeight(), nwavCaja.getWidth())
                : new NwavTamanoPagina(nwavCaja.getWidth(), nwavCaja.getHeight());
    }

    public Path getNwavRuta() {
        return nwavRuta;
    }

    public String getNwavNombre() {
        return nwavRuta.getFileName().toString();
    }

    public int getNwavNumeroPaginas() {
        return nwavTamanos.size();
    }

    public List<NwavTamanoPagina> getNwavTamanos() {
        return nwavTamanos;
    }

    public NwavTamanoPagina nwavTamano(int nwavIndice) {
        return nwavTamanos.get(nwavIndice);
    }

    public boolean isNwavCifrado() {
        return nwavCifrado;
    }

    public boolean isNwavCerrado() {
        return nwavCerrado;
    }

    /** Solo para los servicios del paquete, siempre dentro de {@code synchronized (this)}. */
    PDDocument nwavPdDocument() {
        return nwavDocumento;
    }

    PDFRenderer nwavRenderizador() {
        return nwavRenderizador;
    }

    NwavCacheRender nwavCachePaginas() {
        return nwavCachePaginas;
    }

    NwavCacheRender nwavCacheMiniaturas() {
        return nwavCacheMiniaturas;
    }

    /** Libera la memoria de las caches sin cerrar el documento. */
    public void nwavLiberarCaches() {
        nwavCachePaginas.nwavLimpiar();
        nwavCacheMiniaturas.nwavLimpiar();
    }

    @Override
    public void close() {
        // Espera a que termine cualquier renderizado en curso antes de cerrar.
        synchronized (this) {
            if (nwavCerrado) {
                return;
            }
            nwavCerrado = true;
            nwavLiberarCaches();
            try {
                nwavDocumento.close();
            } catch (IOException nwavError) {
                nwavLog.warn("No se pudo cerrar {} limpiamente", nwavRuta, nwavError);
            }
        }
    }
}
