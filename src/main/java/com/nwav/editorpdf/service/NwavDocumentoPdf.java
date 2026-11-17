package com.nwav.editorpdf.service;

import com.nwav.editorpdf.service.comando.NwavGestorComandos;
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
 * Documento abierto en el editor. Envuelve el {@link PDDocument} junto con su renderizador,
 * sus caches y su historial de cambios (deshacer/rehacer); al cerrarlo se libera todo.
 * Se usa con try-with-resources o se cierra explicitamente al cambiar de archivo.
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
    private final String nwavContrasena;
    private final PDFRenderer nwavRenderizador;
    private final boolean nwavCifrado;
    private final NwavCacheRender nwavCachePaginas = new NwavCacheRender(NWAV_CACHE_PAGINAS_BYTES);
    private final NwavCacheRender nwavCacheMiniaturas = new NwavCacheRender(NWAV_CACHE_MINIATURAS_BYTES);
    private final NwavGestorComandos nwavGestor = new NwavGestorComandos();
    /** PDF de los que se importaron paginas: deben seguir abiertos hasta guardar o cerrar. */
    private final List<PDDocument> nwavAuxiliares = new ArrayList<>();
    private volatile List<NwavTamanoPagina> nwavTamanos;
    private volatile boolean nwavCerrado;

    NwavDocumentoPdf(Path nwavRuta, PDDocument nwavDocumento, String nwavContrasena) {
        this.nwavRuta = nwavRuta;
        this.nwavDocumento = nwavDocumento;
        this.nwavContrasena = nwavContrasena;
        this.nwavRenderizador = new PDFRenderer(nwavDocumento);
        this.nwavCifrado = nwavDocumento.isEncrypted();
        nwavActualizarEstructura();
    }

    static NwavTamanoPagina nwavCalcularTamano(PDPage nwavPagina) {
        PDRectangle nwavCaja = nwavPagina.getCropBox();
        int nwavRotacion = nwavRotacionNormalizada(nwavPagina);
        boolean nwavGirada = nwavRotacion == 90 || nwavRotacion == 270;
        return nwavGirada
                ? new NwavTamanoPagina(nwavCaja.getHeight(), nwavCaja.getWidth())
                : new NwavTamanoPagina(nwavCaja.getWidth(), nwavCaja.getHeight());
    }

    static int nwavRotacionNormalizada(PDPage nwavPagina) {
        return ((nwavPagina.getRotation() % 360) + 360) % 360;
    }

    /** Recalcula los tamanos tras un cambio de paginas. Llamar dentro de {@code synchronized (this)}. */
    void nwavActualizarEstructura() {
        List<NwavTamanoPagina> nwavLista = new ArrayList<>(nwavDocumento.getNumberOfPages());
        for (PDPage nwavPagina : nwavDocumento.getPages()) {
            nwavLista.add(nwavCalcularTamano(nwavPagina));
        }
        nwavTamanos = Collections.unmodifiableList(nwavLista);
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

    /** Historial de cambios en memoria (deshacer/rehacer) de este documento. */
    public NwavGestorComandos getNwavGestor() {
        return nwavGestor;
    }

    public boolean nwavHayCambiosSinGuardar() {
        return nwavGestor.nwavHayCambiosSinGuardar();
    }

    /** Solo para los servicios del paquete, siempre dentro de {@code synchronized (this)}. */
    PDDocument nwavPdDocument() {
        return nwavDocumento;
    }

    PDFRenderer nwavRenderizador() {
        return nwavRenderizador;
    }

    String nwavContrasena() {
        return nwavContrasena;
    }

    NwavCacheRender nwavCachePaginas() {
        return nwavCachePaginas;
    }

    NwavCacheRender nwavCacheMiniaturas() {
        return nwavCacheMiniaturas;
    }

    void nwavRetenerAuxiliar(PDDocument nwavAuxiliar) {
        nwavAuxiliares.add(nwavAuxiliar);
    }

    void nwavVerificarAbierto() {
        if (nwavCerrado) {
            throw new com.nwav.editorpdf.exception.NwavEditorException("El documento ya está cerrado.");
        }
    }

    /** Libera la memoria de las caches sin cerrar el documento. */
    public void nwavLiberarCaches() {
        nwavCachePaginas.nwavLimpiar();
        nwavCacheMiniaturas.nwavLimpiar();
    }

    @Override
    public void close() {
        // Espera a que termine cualquier renderizado o edicion en curso antes de cerrar.
        synchronized (this) {
            if (nwavCerrado) {
                return;
            }
            nwavCerrado = true;
            nwavLiberarCaches();
            nwavGestor.nwavLimpiar();
            nwavCerrarSilencioso(nwavDocumento);
            nwavAuxiliares.forEach(NwavDocumentoPdf::nwavCerrarSilencioso);
            nwavAuxiliares.clear();
        }
    }

    private static void nwavCerrarSilencioso(PDDocument nwavDoc) {
        try {
            nwavDoc.close();
        } catch (IOException nwavError) {
            nwavLog.warn("No se pudo cerrar un documento limpiamente", nwavError);
        }
    }
}
