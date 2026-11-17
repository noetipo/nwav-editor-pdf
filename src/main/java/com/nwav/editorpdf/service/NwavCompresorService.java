package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.model.NwavOpcionesCompresion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Compresion de PDF. Solo toca las imagenes: reduce las que superan la resolucion
 * objetivo, las recomprime en JPEG (conservando la transparencia) y une las duplicadas.
 * No toca las imagenes en blanco y negro de 1 bit ni el texto o los vectores. Ademas
 * comprime los flujos, guarda con object streams y, si se pide, quita metadatos y
 * miniaturas. Si el resultado no es mas pequeno, no se guarda.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NwavCompresorService {

    /** Imagenes mas pequenas que esto no compensan recomprimirlas. */
    private static final int NWAV_PIXELES_MINIMOS = 32 * 32;
    /** La nueva imagen debe ahorrar al menos un 5 % para reemplazar a la original. */
    private static final double NWAV_AHORRO_MINIMO = 0.95;

    private final NwavPdfDocumentoService nwavDocumentos;

    /** Resultado de comprimir un archivo. {@code nwavDestino} es null si no se guardo. */
    public record NwavResultadoCompresion(long nwavAntes, long nwavDespues, Path nwavDestino, int nwavImagenesReducidas,
                                          int nwavImagenesDuplicadas) {

        public boolean nwavGuardado() {
            return nwavDestino != null;
        }

        public double nwavReduccion() {
            return nwavAntes <= 0 ? 0 : 100.0 * (nwavAntes - nwavDespues) / nwavAntes;
        }

        public String nwavResumen() {
            return nwavGuardado()
                    ? String.format(Locale.ROOT, "%s → %s (−%.0f %%)", nwavTamano(nwavAntes), nwavTamano(nwavDespues),
                    nwavReduccion())
                    : "No se pudo reducir: el resultado (" + nwavTamano(nwavDespues) + ") no es más pequeño que el original ("
                    + nwavTamano(nwavAntes) + "). No se guardó.";
        }
    }

    /** Comprime {@code nwavEntrada} en {@code nwavDestino}; si no mejora, no escribe nada. */
    public NwavResultadoCompresion nwavComprimir(Path nwavEntrada, String nwavContrasena, NwavOpcionesCompresion nwavOpciones,
                                                 Path nwavDestino, NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) {
        Path nwavFinal = nwavDestino.toAbsolutePath().normalize();
        if (nwavFinal.equals(nwavEntrada.toAbsolutePath().normalize())) {
            throw new NwavEditorException("La compresión nunca sobrescribe el archivo original.");
        }
        long nwavAntes = nwavTamanoDe(nwavEntrada);
        Path nwavTemporal = null;
        try {
            nwavTemporal = Files.createTempFile("nwav-compresion-", ".pdf");
            int[] nwavContadores = nwavComprimirA(nwavEntrada, nwavContrasena, nwavOpciones, nwavTemporal, nwavAvance,
                    nwavCancelacion);
            long nwavDespues = Files.size(nwavTemporal);
            if (nwavDespues >= nwavAntes) {
                return new NwavResultadoCompresion(nwavAntes, nwavDespues, null, nwavContadores[0], nwavContadores[1]);
            }
            Path nwavOrigen = nwavTemporal;
            NwavEscrituraSegura.nwavEscribir(nwavFinal, nwavTemporalFinal -> Files.copy(nwavOrigen, nwavTemporalFinal,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING));
            return new NwavResultadoCompresion(nwavAntes, nwavDespues, nwavFinal, nwavContadores[0], nwavContadores[1]);
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        } finally {
            nwavBorrar(nwavTemporal);
        }
    }

    /**
     * Comprime a un archivo dado (sin comprobar si mejora). Lo usa la vista previa.
     *
     * @return {imagenes reducidas o recomprimidas, imagenes duplicadas unidas}
     */
    public int[] nwavComprimirA(Path nwavEntrada, String nwavContrasena, NwavOpcionesCompresion nwavOpciones, Path nwavSalida,
                                NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) throws IOException {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            int[] nwavContadores = nwavComprimirDocumento(nwavPd, nwavOpciones, nwavAvance, nwavCancelacion);
            nwavCancelacion.nwavVerificar();
            nwavAvance.nwavInformar(0.95, 1, "Guardando…");
            NwavEscrituraSegura.nwavMantenerProteccion(nwavPd, nwavContrasena);
            nwavPd.save(nwavSalida.toFile(), org.apache.pdfbox.pdfwriter.compress.CompressParameters.DEFAULT_COMPRESSION);
            return nwavContadores;
        }
    }

    /** Antes y despues de una pagina, para comparar la calidad. */
    public record NwavVistaPrevia(BufferedImage nwavAntes, BufferedImage nwavDespues, long nwavTamanoAntes,
                                  long nwavTamanoDespues, int nwavPaginas) {
    }

    /**
     * Comprime a un temporal (que se borra) y renderiza la misma pagina del original y del
     * resultado a la escala indicada.
     */
    public NwavVistaPrevia nwavVistaPrevia(Path nwavEntrada, String nwavContrasena, NwavOpcionesCompresion nwavOpciones,
                                           int nwavPagina, float nwavEscala, NwavAvance nwavAvance,
                                           NwavCancelacion nwavCancelacion) {
        Path nwavTemporal = null;
        try {
            nwavTemporal = Files.createTempFile("nwav-vista-previa-", ".pdf");
            nwavComprimirA(nwavEntrada, nwavContrasena, nwavOpciones, nwavTemporal, nwavAvance, nwavCancelacion);
            nwavCancelacion.nwavVerificar();
            BufferedImage nwavAntes;
            BufferedImage nwavDespues;
            int nwavPaginas;
            try (PDDocument nwavOriginal = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena);
                 PDDocument nwavComprimido = nwavDocumentos.nwavCargarDocumento(nwavTemporal, nwavContrasena)) {
                nwavPaginas = nwavOriginal.getNumberOfPages();
                int nwavIndice = Math.max(0, Math.min(nwavPaginas - 1, nwavPagina));
                nwavAntes = new org.apache.pdfbox.rendering.PDFRenderer(nwavOriginal).renderImage(nwavIndice, nwavEscala);
                nwavDespues = new org.apache.pdfbox.rendering.PDFRenderer(nwavComprimido).renderImage(nwavIndice, nwavEscala);
            }
            return new NwavVistaPrevia(nwavAntes, nwavDespues, Files.size(nwavEntrada), Files.size(nwavTemporal),
                    nwavPaginas);
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        } finally {
            nwavBorrar(nwavTemporal);
        }
    }

    // =============================================================== documento

    int[] nwavComprimirDocumento(PDDocument nwavPd, NwavOpcionesCompresion nwavOpciones, NwavAvance nwavAvance,
                                 NwavCancelacion nwavCancelacion) throws IOException {
        int nwavTotal = nwavPd.getNumberOfPages();
        // 1. Tamano con el que se dibuja cada imagen, para conocer su resolucion real.
        NwavMedidorImagenes nwavMedidor = new NwavMedidorImagenes();
        int nwavIndice = 0;
        for (PDPage nwavPagina : nwavPd.getPages()) {
            nwavCancelacion.nwavVerificar();
            nwavAvance.nwavInformar(nwavIndice * 0.2 / Math.max(1, nwavTotal), 1, "Analizando la página " + (nwavIndice + 1) + "…");
            try {
                nwavMedidor.processPage(nwavPagina);
            } catch (IOException | RuntimeException nwavError) {
                nwavLog.debug("No se pudo analizar la pagina {}", nwavIndice + 1, nwavError);
            }
            nwavIndice++;
        }

        // 2. Imagenes: reducir, recomprimir y unir duplicadas (una sola vez por imagen).
        Map<COSBase, PDImageXObject> nwavProcesadas = new HashMap<>();
        Map<String, PDImageXObject> nwavPorHuella = new HashMap<>();
        Set<COSBase> nwavFormularios = new HashSet<>();
        int[] nwavContadores = new int[2];
        nwavIndice = 0;
        for (PDPage nwavPagina : nwavPd.getPages()) {
            nwavCancelacion.nwavVerificar();
            nwavAvance.nwavInformar(0.2 + nwavIndice * 0.7 / Math.max(1, nwavTotal), 1,
                    "Optimizando imágenes de la página " + (nwavIndice + 1) + " de " + nwavTotal + "…");
            nwavRecorrer(nwavPd, nwavPagina.getResources(), nwavFormularios, nwavMedidor, nwavProcesadas, nwavPorHuella,
                    nwavOpciones, nwavContadores, nwavCancelacion);
            nwavPagina.getCOSObject().removeItem(COSName.getPDFName("Thumb"));
            nwavPagina.getCOSObject().removeItem(COSName.PIECE_INFO);
            if (nwavOpciones.nwavGrises()) {
                nwavGrisesEnContenido(nwavPd, nwavPagina);
            }
            nwavComprimirContenido(nwavPagina);
            nwavIndice++;
        }

        // 3. Metadatos.
        if (nwavOpciones.nwavQuitarMetadatos()) {
            nwavPd.getDocumentCatalog().getCOSObject().removeItem(COSName.METADATA);
            nwavPd.getDocumentCatalog().getCOSObject().removeItem(COSName.PIECE_INFO);
            PDDocumentInformation nwavInfo = new PDDocumentInformation();
            nwavInfo.setProducer("NWAV Editor PDF");
            nwavPd.setDocumentInformation(nwavInfo);
        }
        return nwavContadores;
    }

    private void nwavRecorrer(PDDocument nwavPd, PDResources nwavRecursos, Set<COSBase> nwavFormularios,
                              NwavMedidorImagenes nwavMedidor, Map<COSBase, PDImageXObject> nwavProcesadas,
                              Map<String, PDImageXObject> nwavPorHuella, NwavOpcionesCompresion nwavOpciones,
                              int[] nwavContadores, NwavCancelacion nwavCancelacion) throws IOException {
        if (nwavRecursos == null) {
            return;
        }
        for (COSName nwavNombre : nwavRecursos.getXObjectNames()) {
            nwavCancelacion.nwavVerificar();
            PDXObject nwavObjeto;
            try {
                nwavObjeto = nwavRecursos.getXObject(nwavNombre);
            } catch (IOException nwavError) {
                continue;
            }
            if (nwavObjeto instanceof PDFormXObject nwavForma) {
                if (nwavFormularios.add(nwavForma.getCOSObject())) {
                    nwavRecorrer(nwavPd, nwavForma.getResources(), nwavFormularios, nwavMedidor, nwavProcesadas,
                            nwavPorHuella, nwavOpciones, nwavContadores, nwavCancelacion);
                    if (nwavOpciones.nwavGrises()) {
                        nwavGrisesEnFlujo(nwavForma.getCOSObject());
                    }
                }
                continue;
            }
            if (!(nwavObjeto instanceof PDImageXObject nwavImagen)) {
                continue;
            }
            COSStream nwavFlujo = nwavImagen.getCOSObject();
            PDImageXObject nwavYaHecha = nwavProcesadas.get(nwavFlujo);
            if (nwavYaHecha == null) {
                String nwavHuella = nwavHuella(nwavFlujo);
                PDImageXObject nwavGemela = nwavHuella == null ? null : nwavPorHuella.get(nwavHuella);
                if (nwavGemela != null) {
                    nwavYaHecha = nwavGemela;
                    nwavContadores[1]++;
                } else {
                    nwavYaHecha = nwavOptimizar(nwavPd, nwavImagen, nwavMedidor.nwavTamano(nwavFlujo), nwavOpciones);
                    if (nwavYaHecha != nwavImagen) {
                        nwavContadores[0]++;
                    }
                    if (nwavHuella != null) {
                        nwavPorHuella.put(nwavHuella, nwavYaHecha);
                    }
                }
                nwavProcesadas.put(nwavFlujo, nwavYaHecha);
            }
            if (nwavYaHecha != nwavImagen) {
                nwavRecursos.put(nwavNombre, nwavYaHecha);
            }
        }
    }

    // ================================================================= imagenes

    /** Devuelve una imagen optimizada o la misma si no conviene cambiarla. */
    PDImageXObject nwavOptimizar(PDDocument nwavPd, PDImageXObject nwavImagen, float[] nwavTamanoDibujado,
                                 NwavOpcionesCompresion nwavOpciones) {
        try {
            COSStream nwavFlujo = nwavImagen.getCOSObject();
            if (nwavImagen.isStencil() || nwavImagen.getBitsPerComponent() == 1
                    || nwavFlujo.containsKey(COSName.MASK) || nwavFlujo.containsKey(COSName.DECODE)) {
                return nwavImagen; // 1 bit, mascaras de color o Decode: no se tocan
            }
            List<COSName> nwavFiltros = nwavImagen.getStream().getFilters();
            if (nwavFiltros.contains(COSName.CCITTFAX_DECODE) || nwavFiltros.contains(COSName.JBIG2_DECODE)) {
                return nwavImagen;
            }
            int nwavAncho = nwavImagen.getWidth();
            int nwavAlto = nwavImagen.getHeight();
            if ((long) nwavAncho * nwavAlto < NWAV_PIXELES_MINIMOS) {
                return nwavImagen;
            }
            double nwavEscala = 1.0;
            if (nwavTamanoDibujado != null && nwavTamanoDibujado[0] > 0 && nwavTamanoDibujado[1] > 0) {
                double nwavDpiX = nwavAncho / (nwavTamanoDibujado[0] / 72.0);
                double nwavDpiY = nwavAlto / (nwavTamanoDibujado[1] / 72.0);
                double nwavDpi = Math.min(nwavDpiX, nwavDpiY);
                if (nwavDpi > nwavOpciones.nwavDpi() * 1.15) {
                    nwavEscala = nwavOpciones.nwavDpi() / nwavDpi;
                }
            }
            boolean nwavEsJpeg = nwavFiltros.contains(COSName.DCT_DECODE);
            if (nwavEscala == 1.0 && nwavEsJpeg && !nwavOpciones.nwavGrises() && nwavOpciones.nwavCalidad() >= 0.8f) {
                return nwavImagen; // ya es JPEG y no hay nada que ganar
            }
            BufferedImage nwavOriginal = nwavImagen.getImage();
            if (nwavOriginal == null) {
                return nwavImagen;
            }
            int nwavNuevoAncho = Math.max(1, (int) Math.round(nwavAncho * nwavEscala));
            int nwavNuevoAlto = Math.max(1, (int) Math.round(nwavAlto * nwavEscala));
            boolean nwavConAlfa = nwavOriginal.getColorModel().hasAlpha();
            BufferedImage nwavPreparada = nwavEscalar(nwavOriginal, nwavNuevoAncho, nwavNuevoAlto, nwavConAlfa);
            if (nwavOpciones.nwavGrises()) {
                nwavPreparada = nwavAGrises(nwavPreparada, nwavConAlfa);
            }
            // JPEGFactory guarda el canal alfa como SMask: se conserva la transparencia.
            PDImageXObject nwavNueva = JPEGFactory.createFromImage(nwavPd, nwavPreparada, nwavOpciones.nwavCalidad());
            long nwavLargoNuevo = nwavLargo(nwavNueva.getCOSObject())
                    + (nwavNueva.getCOSObject().getCOSStream(COSName.SMASK) == null ? 0
                    : nwavLargo(nwavNueva.getCOSObject().getCOSStream(COSName.SMASK)));
            long nwavLargoViejo = nwavLargo(nwavFlujo) + (nwavFlujo.getCOSStream(COSName.SMASK) == null ? 0
                    : nwavLargo(nwavFlujo.getCOSStream(COSName.SMASK)));
            if (nwavLargoNuevo >= nwavLargoViejo * NWAV_AHORRO_MINIMO && !nwavOpciones.nwavGrises()) {
                return nwavImagen;
            }
            if (nwavFlujo.getItem(COSName.INTERPOLATE) != null) {
                nwavNueva.getCOSObject().setItem(COSName.INTERPOLATE, nwavFlujo.getItem(COSName.INTERPOLATE));
            }
            return nwavNueva;
        } catch (IOException | RuntimeException nwavError) {
            // Imagen que PDFBox no sabe leer (p. ej. JPEG 2000 sin plugin): se deja como esta.
            nwavLog.debug("Imagen no optimizable: {}", nwavError.toString());
            return nwavImagen;
        }
    }

    /** Reduce por pasos (cada uno a la mitad como mucho) para mantener la nitidez. */
    static BufferedImage nwavEscalar(BufferedImage nwavOrigen, int nwavAncho, int nwavAlto, boolean nwavConAlfa) {
        int nwavTipo = nwavConAlfa ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage nwavActual = nwavOrigen;
        int nwavW = nwavOrigen.getWidth();
        int nwavH = nwavOrigen.getHeight();
        do {
            nwavW = Math.max(nwavAncho, nwavW / 2 >= nwavAncho ? nwavW / 2 : nwavAncho);
            nwavH = Math.max(nwavAlto, nwavH / 2 >= nwavAlto ? nwavH / 2 : nwavAlto);
            BufferedImage nwavPaso = new BufferedImage(nwavW, nwavH, nwavTipo);
            Graphics2D nwavLienzo = nwavPaso.createGraphics();
            nwavLienzo.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            nwavLienzo.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (!nwavConAlfa) {
                nwavLienzo.setColor(java.awt.Color.WHITE);
                nwavLienzo.fillRect(0, 0, nwavW, nwavH);
            }
            nwavLienzo.drawImage(nwavActual, 0, 0, nwavW, nwavH, null);
            nwavLienzo.dispose();
            nwavActual = nwavPaso;
        } while (nwavW > nwavAncho || nwavH > nwavAlto);
        return nwavActual;
    }

    static BufferedImage nwavAGrises(BufferedImage nwavOrigen, boolean nwavConAlfa) {
        if (!nwavConAlfa) {
            BufferedImage nwavGris = new BufferedImage(nwavOrigen.getWidth(), nwavOrigen.getHeight(),
                    BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D nwavLienzo = nwavGris.createGraphics();
            nwavLienzo.drawImage(nwavOrigen, 0, 0, null);
            nwavLienzo.dispose();
            return nwavGris;
        }
        BufferedImage nwavGris = new BufferedImage(nwavOrigen.getWidth(), nwavOrigen.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int nwavY = 0; nwavY < nwavOrigen.getHeight(); nwavY++) {
            for (int nwavX = 0; nwavX < nwavOrigen.getWidth(); nwavX++) {
                int nwavPixel = nwavOrigen.getRGB(nwavX, nwavY);
                int nwavLuz = (int) Math.round(0.299 * ((nwavPixel >> 16) & 0xFF) + 0.587 * ((nwavPixel >> 8) & 0xFF)
                        + 0.114 * (nwavPixel & 0xFF));
                nwavGris.setRGB(nwavX, nwavY, (nwavPixel & 0xFF000000) | (nwavLuz << 16) | (nwavLuz << 8) | nwavLuz);
            }
        }
        return nwavGris;
    }

    // ============================================================ contenido

    /** Comprime con Flate los flujos de contenido que esten sin comprimir. */
    private static void nwavComprimirContenido(PDPage nwavPagina) {
        try {
            java.util.Iterator<PDStream> nwavFlujos = nwavContenidos(nwavPagina);
            while (nwavFlujos.hasNext()) {
                PDStream nwavFlujo = nwavFlujos.next();
                if (nwavFlujo.getFilters().isEmpty()) {
                    byte[] nwavDatos;
                    try (InputStream nwavEntrada = nwavFlujo.createInputStream()) {
                        nwavDatos = nwavEntrada.readAllBytes();
                    }
                    try (OutputStream nwavSalida = nwavFlujo.getCOSObject().createOutputStream(COSName.FLATE_DECODE)) {
                        nwavSalida.write(nwavDatos);
                    }
                }
            }
        } catch (IOException nwavError) {
            nwavLog.debug("No se pudo comprimir el contenido", nwavError);
        }
    }

    private static java.util.Iterator<PDStream> nwavContenidos(PDPage nwavPagina) {
        COSBase nwavContenido = nwavPagina.getCOSObject().getDictionaryObject(COSName.CONTENTS);
        List<PDStream> nwavLista = new ArrayList<>();
        if (nwavContenido instanceof COSStream nwavFlujo) {
            nwavLista.add(new PDStream(nwavFlujo));
        } else if (nwavContenido instanceof COSArray nwavArreglo) {
            for (int nwavIndice = 0; nwavIndice < nwavArreglo.size(); nwavIndice++) {
                if (nwavArreglo.getObject(nwavIndice) instanceof COSStream nwavFlujo) {
                    nwavLista.add(new PDStream(nwavFlujo));
                }
            }
        }
        return nwavLista.iterator();
    }

    /**
     * Escala de grises en texto y vectores: convierte los colores RGB y CMYK directos
     * (rg, RG, k, K) en grises (g, G). Los espacios de color con nombre se dejan igual.
     */
    private static void nwavGrisesEnContenido(PDDocument nwavPd, PDPage nwavPagina) {
        try {
            List<Object> nwavTokens = nwavConvertirTokens(new PDFStreamParser(nwavPagina).parse());
            PDStream nwavNuevo = new PDStream(nwavPd);
            try (OutputStream nwavSalida = nwavNuevo.createOutputStream(COSName.FLATE_DECODE)) {
                new ContentStreamWriter(nwavSalida).writeTokens(nwavTokens);
            }
            nwavPagina.setContents(nwavNuevo);
        } catch (IOException | RuntimeException nwavError) {
            nwavLog.debug("No se pudo pasar a grises el contenido", nwavError);
        }
    }

    private static void nwavGrisesEnFlujo(COSStream nwavFlujo) {
        try {
            List<Object> nwavTokens;
            try (InputStream nwavEntrada = nwavFlujo.createInputStream()) {
                nwavTokens = nwavConvertirTokens(new PDFStreamParser(nwavEntrada.readAllBytes()).parse());
            }
            try (OutputStream nwavSalida = nwavFlujo.createOutputStream(COSName.FLATE_DECODE)) {
                new ContentStreamWriter(nwavSalida).writeTokens(nwavTokens);
            }
        } catch (IOException | RuntimeException nwavError) {
            nwavLog.debug("No se pudo pasar a grises un formulario", nwavError);
        }
    }

    static List<Object> nwavConvertirTokens(List<Object> nwavTokens) {
        List<Object> nwavSalida = new ArrayList<>(nwavTokens.size());
        List<Object> nwavOperandos = new ArrayList<>();
        for (Object nwavToken : nwavTokens) {
            if (!(nwavToken instanceof Operator nwavOperador)) {
                nwavOperandos.add(nwavToken);
                continue;
            }
            String nwavNombre = nwavOperador.getName();
            boolean nwavRgb = ("rg".equals(nwavNombre) || "RG".equals(nwavNombre)) && nwavOperandos.size() == 3;
            boolean nwavCmyk = ("k".equals(nwavNombre) || "K".equals(nwavNombre)) && nwavOperandos.size() == 4;
            if ((nwavRgb || nwavCmyk) && nwavOperandos.stream().allMatch(nwavValor -> nwavValor instanceof COSNumber)) {
                float[] nwavV = new float[nwavOperandos.size()];
                for (int nwavIndice = 0; nwavIndice < nwavV.length; nwavIndice++) {
                    nwavV[nwavIndice] = ((COSNumber) nwavOperandos.get(nwavIndice)).floatValue();
                }
                float nwavGris = nwavRgb
                        ? 0.299f * nwavV[0] + 0.587f * nwavV[1] + 0.114f * nwavV[2]
                        : 1f - Math.min(1f, 0.3f * nwavV[0] + 0.59f * nwavV[1] + 0.11f * nwavV[2] + nwavV[3]);
                nwavSalida.add(new COSFloat(Math.max(0f, Math.min(1f, nwavGris))));
                nwavSalida.add(Operator.getOperator(Character.isUpperCase(nwavNombre.charAt(0)) ? "G" : "g"));
            } else {
                nwavSalida.addAll(nwavOperandos);
                nwavSalida.add(nwavOperador);
            }
            nwavOperandos.clear();
        }
        nwavSalida.addAll(nwavOperandos);
        return nwavSalida;
    }

    // ============================================================= utilidades

    private static long nwavLargo(COSStream nwavFlujo) {
        long nwavLargo = nwavFlujo.getLength();
        return nwavLargo > 0 ? nwavLargo : nwavFlujo.getLong(COSName.LENGTH, 0);
    }

    /** Huella de los datos codificados de una imagen (y su mascara), para unir duplicadas. */
    private static String nwavHuella(COSStream nwavFlujo) {
        try (InputStream nwavDatos = nwavFlujo.createRawInputStream()) {
            MessageDigest nwavResumen = MessageDigest.getInstance("SHA-256");
            nwavResumen.update(nwavDatos.readAllBytes());
            COSDictionary nwavDiccionario = nwavFlujo;
            nwavResumen.update((nwavDiccionario.getInt(COSName.WIDTH) + "x" + nwavDiccionario.getInt(COSName.HEIGHT))
                    .getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            COSStream nwavMascara = nwavFlujo.getCOSStream(COSName.SMASK);
            if (nwavMascara != null) {
                try (InputStream nwavDatosMascara = nwavMascara.createRawInputStream()) {
                    nwavResumen.update(nwavDatosMascara.readAllBytes());
                }
            }
            return HexFormat.of().formatHex(nwavResumen.digest());
        } catch (IOException | NoSuchAlgorithmException nwavError) {
            return null;
        }
    }

    private static long nwavTamanoDe(Path nwavRuta) {
        try {
            return Files.size(nwavRuta);
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavRuta.getFileName()));
        }
    }

    private static void nwavBorrar(Path nwavRuta) {
        if (nwavRuta != null) {
            try {
                Files.deleteIfExists(nwavRuta);
            } catch (IOException nwavIgnorado) {
                // temporal del sistema
            }
        }
    }

    public static String nwavTamano(long nwavBytes) {
        if (nwavBytes < 1024) {
            return nwavBytes + " B";
        }
        if (nwavBytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.0f KB", nwavBytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", nwavBytes / 1024.0 / 1024.0);
    }
}
