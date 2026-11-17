package com.nwav.editorpdf.service;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.service.NwavDisposicionPdf.NwavLinea;
import com.nwav.editorpdf.service.NwavDisposicionPdf.NwavPagina;
import com.nwav.editorpdf.service.NwavDisposicionPdf.NwavPalabra;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Compara dos PDF: diferencias de texto palabra a palabra (en todo el documento, para que
 * un parrafo que cambia de pagina no cuente como cambio) y superposicion visual por pixeles.
 */
@Service
@RequiredArgsConstructor
public class NwavComparacionService {

    private final NwavPdfDocumentoService nwavDocumentos;

    /** Palabras de una pagina marcadas como quitadas (en el original) o anadidas (en el nuevo). */
    public record NwavMarcasPagina(List<NwavRectangulo> nwavQuitadas, List<NwavRectangulo> nwavAnadidas) {
    }

    /**
     * Sesion abierta de comparacion: mantiene ambos documentos para dibujar sus paginas.
     * Hay que cerrarla al terminar.
     */
    public static final class NwavSesion implements AutoCloseable {
        private final PDDocument nwavA;
        private final PDDocument nwavB;
        private final PDFRenderer nwavRenderA;
        private final PDFRenderer nwavRenderB;
        private final List<NwavMarcasPagina> nwavMarcasA;
        private final List<NwavMarcasPagina> nwavMarcasB;
        private final int nwavQuitadas;
        private final int nwavAnadidas;

        private NwavSesion(PDDocument nwavA, PDDocument nwavB, List<NwavMarcasPagina> nwavMarcasA,
                           List<NwavMarcasPagina> nwavMarcasB, int nwavQuitadas, int nwavAnadidas) {
            this.nwavA = nwavA;
            this.nwavB = nwavB;
            this.nwavRenderA = new PDFRenderer(nwavA);
            this.nwavRenderB = new PDFRenderer(nwavB);
            this.nwavMarcasA = nwavMarcasA;
            this.nwavMarcasB = nwavMarcasB;
            this.nwavQuitadas = nwavQuitadas;
            this.nwavAnadidas = nwavAnadidas;
        }

        public int nwavPaginasA() {
            return nwavA.getNumberOfPages();
        }

        public int nwavPaginasB() {
            return nwavB.getNumberOfPages();
        }

        public int nwavPaginas() {
            return Math.max(nwavPaginasA(), nwavPaginasB());
        }

        public int nwavQuitadas() {
            return nwavQuitadas;
        }

        public int nwavAnadidas() {
            return nwavAnadidas;
        }

        public boolean nwavIguales() {
            return nwavQuitadas == 0 && nwavAnadidas == 0;
        }

        /** Marcas de la pagina en el original (vacias si no existe). */
        public NwavMarcasPagina nwavMarcasA(int nwavPagina) {
            return nwavPagina < nwavMarcasA.size() ? nwavMarcasA.get(nwavPagina) : new NwavMarcasPagina(List.of(), List.of());
        }

        public NwavMarcasPagina nwavMarcasB(int nwavPagina) {
            return nwavPagina < nwavMarcasB.size() ? nwavMarcasB.get(nwavPagina) : new NwavMarcasPagina(List.of(), List.of());
        }

        /** Pagina del original o del nuevo a la escala dada; null si ese documento no tiene esa pagina. */
        public synchronized BufferedImage nwavRenderizar(boolean nwavOriginal, int nwavPagina, float nwavEscala)
                throws IOException {
            PDDocument nwavDoc = nwavOriginal ? nwavA : nwavB;
            if (nwavPagina >= nwavDoc.getNumberOfPages()) {
                return null;
            }
            return (nwavOriginal ? nwavRenderA : nwavRenderB).renderImage(nwavPagina, nwavEscala, ImageType.RGB);
        }

        /**
         * Superposicion: el original atenuado; en rojo lo que solo esta en el original y en
         * verde lo que solo esta en el nuevo. Devuelve la imagen y el % de pixeles distintos.
         */
        public synchronized NwavSuperposicion nwavSuperponer(int nwavPagina, float nwavEscala) throws IOException {
            BufferedImage nwavImagenA = nwavRenderizar(true, nwavPagina, nwavEscala);
            BufferedImage nwavImagenB = nwavRenderizar(false, nwavPagina, nwavEscala);
            return NwavComparacionService.nwavSuperponer(nwavImagenA, nwavImagenB);
        }

        @Override
        public void close() throws IOException {
            try {
                nwavA.close();
            } finally {
                nwavB.close();
            }
        }
    }

    public record NwavSuperposicion(BufferedImage nwavImagen, double nwavPorcentajeDistinto) {
    }

    /** Abre ambos documentos y calcula las diferencias de texto. */
    public NwavSesion nwavComparar(Path nwavOriginal, String nwavClaveA, Path nwavNuevo, String nwavClaveB,
                                   NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) {
        PDDocument nwavA = null;
        PDDocument nwavB = null;
        try {
            nwavAvance.nwavInformar(0, 3, "Leyendo «" + nwavOriginal.getFileName() + "»…");
            nwavA = nwavDocumentos.nwavCargarDocumento(nwavOriginal, nwavClaveA);
            nwavAvance.nwavInformar(1, 3, "Leyendo «" + nwavNuevo.getFileName() + "»…");
            nwavB = nwavDocumentos.nwavCargarDocumento(nwavNuevo, nwavClaveB);
            List<NwavPagina> nwavPaginasA = NwavDisposicionPdf.nwavAnalizar(nwavA,
                    IntStream.range(0, nwavA.getNumberOfPages()).boxed().toList(), nwavCancelacion);
            List<NwavPagina> nwavPaginasB = NwavDisposicionPdf.nwavAnalizar(nwavB,
                    IntStream.range(0, nwavB.getNumberOfPages()).boxed().toList(), nwavCancelacion);
            nwavAvance.nwavInformar(2, 3, "Buscando diferencias…");
            List<NwavPalabraUbicada> nwavPalabrasA = nwavPalabras(nwavPaginasA);
            List<NwavPalabraUbicada> nwavPalabrasB = nwavPalabras(nwavPaginasB);
            List<List<NwavRectangulo>> nwavQuitadasPorPagina = nwavVacias(nwavPaginasA.size());
            List<List<NwavRectangulo>> nwavAnadidasPorPagina = nwavVacias(nwavPaginasB.size());
            int nwavQuitadas = 0;
            int nwavAnadidas = 0;
            List<AbstractDelta<String>> nwavCambios = DiffUtils.diff(
                    nwavPalabrasA.stream().map(NwavPalabraUbicada::nwavTexto).toList(),
                    nwavPalabrasB.stream().map(NwavPalabraUbicada::nwavTexto).toList()).getDeltas();
            for (AbstractDelta<String> nwavCambio : nwavCambios) {
                nwavCancelacion.nwavVerificar();
                if (nwavCambio.getType() == DeltaType.EQUAL) {
                    continue;
                }
                int nwavInicioA = nwavCambio.getSource().getPosition();
                for (int nwavIndice = 0; nwavIndice < nwavCambio.getSource().size(); nwavIndice++) {
                    NwavPalabraUbicada nwavPalabra = nwavPalabrasA.get(nwavInicioA + nwavIndice);
                    nwavQuitadasPorPagina.get(nwavPalabra.nwavPagina()).add(nwavPalabra.nwavZona());
                    nwavQuitadas++;
                }
                int nwavInicioB = nwavCambio.getTarget().getPosition();
                for (int nwavIndice = 0; nwavIndice < nwavCambio.getTarget().size(); nwavIndice++) {
                    NwavPalabraUbicada nwavPalabra = nwavPalabrasB.get(nwavInicioB + nwavIndice);
                    nwavAnadidasPorPagina.get(nwavPalabra.nwavPagina()).add(nwavPalabra.nwavZona());
                    nwavAnadidas++;
                }
            }
            List<NwavMarcasPagina> nwavMarcasA = nwavQuitadasPorPagina.stream()
                    .map(nwavQ -> new NwavMarcasPagina(List.copyOf(nwavQ), List.of())).toList();
            List<NwavMarcasPagina> nwavMarcasB = nwavAnadidasPorPagina.stream()
                    .map(nwavN -> new NwavMarcasPagina(List.of(), List.copyOf(nwavN))).toList();
            nwavAvance.nwavInformar(3, 3, "Listo");
            return new NwavSesion(nwavA, nwavB, nwavMarcasA, nwavMarcasB, nwavQuitadas, nwavAnadidas);
        } catch (IOException | RuntimeException nwavError) {
            nwavCerrar(nwavA);
            nwavCerrar(nwavB);
            if (nwavError instanceof IOException nwavIo) {
                throw NwavTraductorErrores.nwavTraducir(nwavIo, String.valueOf(nwavOriginal.getFileName()));
            }
            throw (RuntimeException) nwavError;
        }
    }

    record NwavPalabraUbicada(String nwavTexto, int nwavPagina, NwavRectangulo nwavZona) {
    }

    private static List<NwavPalabraUbicada> nwavPalabras(List<NwavPagina> nwavPaginas) {
        List<NwavPalabraUbicada> nwavPalabras = new ArrayList<>();
        for (int nwavIndice = 0; nwavIndice < nwavPaginas.size(); nwavIndice++) {
            for (NwavLinea nwavLinea : nwavPaginas.get(nwavIndice).nwavLineas()) {
                for (NwavPalabra nwavPalabra : nwavLinea.nwavPalabras()) {
                    double nwavAlto = Math.max(nwavPalabra.nwavAlto(), nwavPalabra.nwavTamano() * 0.75);
                    nwavPalabras.add(new NwavPalabraUbicada(nwavPalabra.nwavTexto(), nwavIndice, new NwavRectangulo(
                            nwavPalabra.nwavX() - 1, nwavPalabra.nwavY() - nwavAlto - 1,
                            nwavPalabra.nwavXFin() - nwavPalabra.nwavX() + 2, nwavAlto + nwavPalabra.nwavTamano() * 0.3 + 2)));
                }
            }
        }
        return nwavPalabras;
    }

    private static List<List<NwavRectangulo>> nwavVacias(int nwavCantidad) {
        List<List<NwavRectangulo>> nwavListas = new ArrayList<>();
        for (int nwavIndice = 0; nwavIndice < nwavCantidad; nwavIndice++) {
            nwavListas.add(new ArrayList<>());
        }
        return nwavListas;
    }

    static NwavSuperposicion nwavSuperponer(BufferedImage nwavImagenA, BufferedImage nwavImagenB) {
        if (nwavImagenA == null && nwavImagenB == null) {
            return new NwavSuperposicion(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), 0);
        }
        int nwavAncho = Math.max(nwavImagenA == null ? 0 : nwavImagenA.getWidth(), nwavImagenB == null ? 0 : nwavImagenB.getWidth());
        int nwavAlto = Math.max(nwavImagenA == null ? 0 : nwavImagenA.getHeight(), nwavImagenB == null ? 0 : nwavImagenB.getHeight());
        BufferedImage nwavA = nwavLienzo(nwavImagenA, nwavAncho, nwavAlto);
        BufferedImage nwavB = nwavLienzo(nwavImagenB, nwavAncho, nwavAlto);
        BufferedImage nwavResultado = new BufferedImage(nwavAncho, nwavAlto, BufferedImage.TYPE_INT_RGB);
        long nwavDistintos = 0;
        for (int nwavY = 0; nwavY < nwavAlto; nwavY++) {
            for (int nwavX = 0; nwavX < nwavAncho; nwavX++) {
                int nwavLumA = nwavLuminancia(nwavA.getRGB(nwavX, nwavY));
                int nwavLumB = nwavLuminancia(nwavB.getRGB(nwavX, nwavY));
                int nwavColor;
                if (Math.abs(nwavLumA - nwavLumB) > 48) {
                    nwavDistintos++;
                    // mas oscuro en el original = solo esta en el original (rojo); al reves, verde
                    nwavColor = nwavLumA < nwavLumB ? 0xE11D48 : 0x16A34A;
                } else {
                    int nwavAtenuado = 255 - (255 - nwavLumA) / 3; // original en gris claro
                    nwavColor = (nwavAtenuado << 16) | (nwavAtenuado << 8) | nwavAtenuado;
                }
                nwavResultado.setRGB(nwavX, nwavY, nwavColor);
            }
        }
        return new NwavSuperposicion(nwavResultado, 100.0 * nwavDistintos / Math.max(1L, (long) nwavAncho * nwavAlto));
    }

    private static BufferedImage nwavLienzo(BufferedImage nwavImagen, int nwavAncho, int nwavAlto) {
        BufferedImage nwavLienzo = new BufferedImage(nwavAncho, nwavAlto, BufferedImage.TYPE_INT_RGB);
        Graphics2D nwavG = nwavLienzo.createGraphics();
        nwavG.setColor(java.awt.Color.WHITE);
        nwavG.fillRect(0, 0, nwavAncho, nwavAlto);
        if (nwavImagen != null) {
            nwavG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            nwavG.drawImage(nwavImagen, 0, 0, null);
        }
        nwavG.dispose();
        return nwavLienzo;
    }

    private static int nwavLuminancia(int nwavRgb) {
        int nwavR = (nwavRgb >> 16) & 0xFF;
        int nwavV = (nwavRgb >> 8) & 0xFF;
        int nwavAz = nwavRgb & 0xFF;
        return (nwavR * 299 + nwavV * 587 + nwavAz * 114) / 1000;
    }

    private static void nwavCerrar(PDDocument nwavPd) {
        if (nwavPd != null) {
            try {
                nwavPd.close();
            } catch (IOException nwavError) {
                // ya se informa el error principal
            }
        }
    }
}
