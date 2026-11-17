package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavCanceladoException;
import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.service.herramienta.NwavDependenciasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import net.sourceforge.tess4j.util.ImageHelper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OCR con Tesseract (Tess4J). La imagen de cada pagina queda visible y el texto reconocido
 * se agrega como capa invisible, alineada con cada palabra, de modo que el PDF se puede
 * seleccionar y buscar. Las paginas que ya tienen texto se saltan (salvo que se fuerce).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NwavOcrService {

    public static final int NWAV_DPI_OCR = 300;
    /** Una pagina con menos caracteres que estos se considera sin texto. */
    public static final int NWAV_CARACTERES_MINIMOS = 25;
    private static final double NWAV_UMBRAL_INCLINACION = 0.3;
    private static final List<String> NWAV_FUENTES_UNICODE = List.of(
            "/System/Library/Fonts/Supplemental/Arial Unicode.ttf", "/Library/Fonts/Arial Unicode.ttf",
            "C:\\Windows\\Fonts\\arialuni.ttf", "C:\\Windows\\Fonts\\arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", "/usr/share/fonts/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf");

    private final NwavDependenciasService nwavDependencias;
    private final NwavPdfDocumentoService nwavDocumentos;

    /** Opciones del OCR. */
    public record NwavOpcionesOcr(List<String> nwavIdiomas, boolean nwavForzar, boolean nwavEnderezar,
                                  boolean nwavGrises, boolean nwavContraste, boolean nwavSalidaPdf,
                                  boolean nwavSalidaTexto) {
    }

    /** Resultado del OCR. */
    public record NwavResultadoOcr(Path nwavPdf, Path nwavTexto, int nwavPaginasReconocidas, int nwavPaginasConTexto,
                                   int nwavPalabras, String nwavIdiomas) {

        public String nwavResumen() {
            return nwavPaginasReconocidas + (nwavPaginasReconocidas == 1 ? " página reconocida" : " páginas reconocidas")
                    + (nwavPaginasConTexto > 0 ? " · " + nwavPaginasConTexto + " ya tenían texto" : "")
                    + " · " + nwavPalabras + " palabras · idioma: " + nwavIdiomas;
        }
    }

    /** Palabra reconocida en coordenadas de la imagen renderizada. */
    record NwavPalabra(String nwavTexto, Rectangle nwavCaja) {
    }

    /** Resultado de una pagina. */
    record NwavPaginaReconocida(int nwavIndice, int nwavAnchoPx, int nwavAltoPx, List<NwavPalabra> nwavPalabras) {
    }

    public static String nwavNombreIdioma(String nwavCodigo) {
        return switch (nwavCodigo) {
            case "spa" -> "Español";
            case "eng" -> "Inglés";
            case "fra" -> "Francés";
            case "deu" -> "Alemán";
            case "por" -> "Portugués";
            case "ita" -> "Italiano";
            case "cat" -> "Catalán";
            case "glg" -> "Gallego";
            case "eus" -> "Euskera";
            case "nld" -> "Neerlandés";
            case "rus" -> "Ruso";
            case "chi_sim" -> "Chino simplificado";
            case "jpn" -> "Japonés";
            case "ara" -> "Árabe";
            default -> nwavCodigo;
        };
    }

    public NwavResultadoOcr nwavAplicar(Path nwavEntrada, String nwavContrasena, NwavOpcionesOcr nwavOpciones,
                                        Path nwavDestinoPdf, Path nwavDestinoTexto, NwavAvance nwavAvance,
                                        NwavCancelacion nwavCancelacion) {
        NwavDependenciasService.NwavEstado nwavEstado = nwavDependencias.nwavTesseract();
        if (!nwavEstado.nwavDisponible()) {
            throw new NwavEditorException(nwavEstado.nwavDetalle() + " " + nwavEstado.nwavInstrucciones());
        }
        if (nwavOpciones.nwavIdiomas() == null || nwavOpciones.nwavIdiomas().isEmpty()) {
            throw new NwavEditorException("Elige al menos un idioma.");
        }
        if (!nwavOpciones.nwavSalidaPdf() && !nwavOpciones.nwavSalidaTexto()) {
            throw new NwavEditorException("Elige al menos una salida: PDF buscable o texto.");
        }
        String nwavIdiomas = String.join("+", nwavOpciones.nwavIdiomas());
        Path nwavDatos;
        try {
            nwavDatos = nwavDependencias.nwavPrepararIdiomas(nwavOpciones.nwavIdiomas());
        } catch (IOException nwavError) {
            throw new NwavEditorException("No se pudieron preparar los datos de idioma del OCR.", nwavError);
        }

        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            int nwavTotal = nwavPd.getNumberOfPages();
            String[] nwavTextoExistente = new String[nwavTotal];
            List<Integer> nwavPendientes = new ArrayList<>();
            for (int nwavIndice = 0; nwavIndice < nwavTotal; nwavIndice++) {
                nwavCancelacion.nwavVerificar();
                nwavTextoExistente[nwavIndice] = nwavTextoDePagina(nwavPd, nwavIndice);
                boolean nwavTieneTexto = nwavTextoExistente[nwavIndice].replaceAll("\\s+", "").length()
                        >= NWAV_CARACTERES_MINIMOS;
                if (!nwavTieneTexto || nwavOpciones.nwavForzar()) {
                    nwavPendientes.add(nwavIndice);
                }
            }

            List<NwavPaginaReconocida> nwavReconocidas = nwavReconocer(nwavPd, nwavPendientes, nwavDatos, nwavIdiomas,
                    nwavOpciones, nwavAvance, nwavCancelacion);
            nwavCancelacion.nwavVerificar();

            int nwavPalabras = nwavReconocidas.stream().mapToInt(nwavPagina -> nwavPagina.nwavPalabras().size()).sum();
            Path nwavPdfGenerado = null;
            Path nwavTextoGenerado = null;
            if (nwavOpciones.nwavSalidaPdf()) {
                nwavAvance.nwavInformar(0.95, 1, "Agregando la capa de texto…");
                PDFont nwavFuente = nwavFuenteParaTexto(nwavPd, nwavReconocidas);
                for (NwavPaginaReconocida nwavPagina : nwavReconocidas) {
                    nwavAgregarCapaDeTexto(nwavPd, nwavPagina, nwavFuente);
                }
                NwavEscrituraSegura.nwavMantenerProteccion(nwavPd, nwavContrasena);
                nwavPdfGenerado = NwavEscrituraSegura.nwavGuardarPdf(nwavPd, nwavDestinoPdf);
            }
            if (nwavOpciones.nwavSalidaTexto()) {
                String nwavTexto = nwavTextoCompleto(nwavTotal, nwavTextoExistente, nwavReconocidas);
                nwavTextoGenerado = NwavEscrituraSegura.nwavEscribir(nwavDestinoTexto,
                        nwavTemporal -> Files.writeString(nwavTemporal, nwavTexto, StandardCharsets.UTF_8));
            }
            nwavAvance.nwavInformar(1, 1, "Listo");
            return new NwavResultadoOcr(nwavPdfGenerado, nwavTextoGenerado, nwavReconocidas.size(),
                    nwavTotal - nwavPendientes.size(), nwavPalabras,
                    nwavOpciones.nwavIdiomas().stream().map(NwavOcrService::nwavNombreIdioma).reduce((nwavA, nwavB) -> nwavA + " + " + nwavB)
                            .orElse(nwavIdiomas));
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    // ============================================================ reconocimiento

    /**
     * Renderiza las paginas (de una en una: PDFBox no es seguro entre hilos) y las reconoce
     * en paralelo, una instancia de Tesseract por hilo.
     */
    private List<NwavPaginaReconocida> nwavReconocer(PDDocument nwavPd, List<Integer> nwavPaginas, Path nwavDatos,
                                                     String nwavIdiomas, NwavOpcionesOcr nwavOpciones,
                                                     NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) {
        if (nwavPaginas.isEmpty()) {
            return List.of();
        }
        int nwavHilos = Math.max(1, Math.min(nwavPaginas.size(), Runtime.getRuntime().availableProcessors() - 1));
        ExecutorService nwavEjecutor = Executors.newFixedThreadPool(nwavHilos, nwavTarea -> {
            Thread nwavHilo = new Thread(nwavTarea, "nwav-ocr");
            nwavHilo.setDaemon(true);
            return nwavHilo;
        });
        ThreadLocal<Tesseract> nwavMotores = ThreadLocal.withInitial(() -> {
            Tesseract nwavMotor = new Tesseract();
            nwavMotor.setDatapath(nwavDatos.toString());
            nwavMotor.setLanguage(nwavIdiomas);
            nwavMotor.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);
            nwavMotor.setPageSegMode(ITessAPI.TessPageSegMode.PSM_AUTO);
            nwavMotor.setVariable("user_defined_dpi", String.valueOf(NWAV_DPI_OCR));
            return nwavMotor;
        });
        PDFRenderer nwavRenderizador = new PDFRenderer(nwavPd);
        AtomicInteger nwavHechas = new AtomicInteger();
        List<Future<NwavPaginaReconocida>> nwavFuturos = new ArrayList<>();
        try {
            for (int nwavIndice : nwavPaginas) {
                nwavCancelacion.nwavVerificar();
                nwavAvance.nwavInformar(nwavHechas.get(), nwavPaginas.size(),
                        "Preparando la página " + (nwavIndice + 1) + "…");
                BufferedImage nwavImagen;
                synchronized (nwavPd) {
                    nwavImagen = nwavRenderizador.renderImageWithDPI(nwavIndice, NWAV_DPI_OCR, ImageType.RGB);
                }
                nwavFuturos.add(nwavEjecutor.submit(() -> {
                    nwavCancelacion.nwavVerificar();
                    NwavPaginaReconocida nwavResultado = nwavReconocerPagina(nwavMotores.get(), nwavIndice, nwavImagen,
                            nwavOpciones);
                    int nwavCuenta = nwavHechas.incrementAndGet();
                    nwavAvance.nwavInformar(nwavCuenta, nwavPaginas.size(),
                            "Página " + (nwavIndice + 1) + " reconocida (" + nwavCuenta + " de " + nwavPaginas.size() + ")");
                    return nwavResultado;
                }));
            }
            List<NwavPaginaReconocida> nwavResultados = new ArrayList<>();
            for (Future<NwavPaginaReconocida> nwavFuturo : nwavFuturos) {
                nwavResultados.add(nwavFuturo.get());
            }
            nwavResultados.sort(Comparator.comparingInt(NwavPaginaReconocida::nwavIndice));
            return nwavResultados;
        } catch (IOException nwavError) {
            throw new NwavEditorException("No se pudo dibujar una página para el OCR.", nwavError);
        } catch (InterruptedException nwavError) {
            Thread.currentThread().interrupt();
            throw new NwavCanceladoException();
        } catch (ExecutionException nwavError) {
            Throwable nwavCausa = nwavError.getCause();
            if (nwavCausa instanceof RuntimeException nwavEnTiempo) {
                throw nwavEnTiempo;
            }
            throw new NwavEditorException("Falló el reconocimiento de texto: " + nwavCausa.getMessage(), nwavCausa);
        } finally {
            nwavFuturos.forEach(nwavFuturo -> nwavFuturo.cancel(true));
            nwavEjecutor.shutdownNow();
        }
    }

    static NwavPaginaReconocida nwavReconocerPagina(Tesseract nwavMotor, int nwavIndice, BufferedImage nwavOriginal,
                                                    NwavOpcionesOcr nwavOpciones) {
        BufferedImage nwavImagen = nwavOriginal;
        if (nwavOpciones.nwavGrises() || nwavOpciones.nwavContraste()) {
            nwavImagen = ImageHelper.convertImageToGrayscale(nwavImagen);
        }
        if (nwavOpciones.nwavContraste()) {
            nwavImagen = nwavEstirarContraste(nwavImagen);
        }
        double nwavAngulo = 0;
        if (nwavOpciones.nwavEnderezar()) {
            nwavAngulo = new com.recognition.software.jdeskew.ImageDeskew(nwavImagen).getSkewAngle();
            if (Math.abs(nwavAngulo) > NWAV_UMBRAL_INCLINACION) {
                nwavImagen = ImageHelper.rotateImage(nwavImagen, -nwavAngulo);
            } else {
                nwavAngulo = 0;
            }
        }
        List<Word> nwavPalabras = nwavMotor.getWords(List.of(nwavImagen), ITessAPI.TessPageIteratorLevel.RIL_WORD);
        List<NwavPalabra> nwavResultado = new ArrayList<>();
        double nwavCentroX = nwavImagen.getWidth() / 2.0;
        double nwavCentroY = nwavImagen.getHeight() / 2.0;
        double nwavRadianes = Math.toRadians(nwavAngulo);
        for (Word nwavPalabra : nwavPalabras) {
            String nwavTexto = nwavPalabra.getText() == null ? "" : nwavPalabra.getText().strip();
            if (nwavTexto.isEmpty() || nwavPalabra.getBoundingBox() == null) {
                continue;
            }
            Rectangle nwavCaja = new Rectangle(nwavPalabra.getBoundingBox());
            if (nwavAngulo != 0) {
                // La imagen se giro para reconocerla: el centro de la palabra vuelve a su sitio.
                double nwavDx = nwavCaja.getCenterX() - nwavCentroX;
                double nwavDy = nwavCaja.getCenterY() - nwavCentroY;
                double nwavX = nwavCentroX + nwavDx * Math.cos(nwavRadianes) - nwavDy * Math.sin(nwavRadianes);
                double nwavY = nwavCentroY + nwavDx * Math.sin(nwavRadianes) + nwavDy * Math.cos(nwavRadianes);
                nwavCaja.setLocation((int) Math.round(nwavX - nwavCaja.width / 2.0),
                        (int) Math.round(nwavY - nwavCaja.height / 2.0));
            }
            nwavResultado.add(new NwavPalabra(nwavTexto, nwavCaja));
        }
        return new NwavPaginaReconocida(nwavIndice, nwavOriginal.getWidth(), nwavOriginal.getHeight(), nwavResultado);
    }

    /** Lleva los niveles de gris del 1 % al 99 % a todo el rango (mas contraste). */
    static BufferedImage nwavEstirarContraste(BufferedImage nwavGris) {
        int[] nwavHistograma = new int[256];
        int nwavAncho = nwavGris.getWidth();
        int nwavAlto = nwavGris.getHeight();
        var nwavRaster = nwavGris.getRaster();
        for (int nwavY = 0; nwavY < nwavAlto; nwavY++) {
            for (int nwavX = 0; nwavX < nwavAncho; nwavX++) {
                nwavHistograma[nwavRaster.getSample(nwavX, nwavY, 0)]++;
            }
        }
        long nwavTotal = (long) nwavAncho * nwavAlto;
        int nwavBajo = 0;
        int nwavAltoNivel = 255;
        long nwavAcumulado = 0;
        for (int nwavNivel = 0; nwavNivel < 256; nwavNivel++) {
            nwavAcumulado += nwavHistograma[nwavNivel];
            if (nwavAcumulado <= nwavTotal * 0.01) {
                nwavBajo = nwavNivel;
            }
            if (nwavAcumulado <= nwavTotal * 0.99) {
                nwavAltoNivel = nwavNivel;
            }
        }
        if (nwavAltoNivel - nwavBajo < 10) {
            return nwavGris;
        }
        BufferedImage nwavSalida = new BufferedImage(nwavAncho, nwavAlto, BufferedImage.TYPE_BYTE_GRAY);
        var nwavDestino = nwavSalida.getRaster();
        double nwavFactor = 255.0 / (nwavAltoNivel - nwavBajo);
        for (int nwavY = 0; nwavY < nwavAlto; nwavY++) {
            for (int nwavX = 0; nwavX < nwavAncho; nwavX++) {
                int nwavValor = (int) Math.round((nwavRaster.getSample(nwavX, nwavY, 0) - nwavBajo) * nwavFactor);
                nwavDestino.setSample(nwavX, nwavY, 0, Math.max(0, Math.min(255, nwavValor)));
            }
        }
        return nwavSalida;
    }

    // ============================================================= capa de texto

    /**
     * Escribe las palabras como texto invisible (modo de render 3) sobre la pagina,
     * ajustando tamano y escala horizontal para que cada palabra ocupe su caja.
     */
    static void nwavAgregarCapaDeTexto(PDDocument nwavPd, NwavPaginaReconocida nwavPagina, PDFont nwavFuente)
            throws IOException {
        if (nwavPagina.nwavPalabras().isEmpty()) {
            return;
        }
        PDPage nwavHoja = nwavPd.getPage(nwavPagina.nwavIndice());
        NwavGeometriaPagina nwavGeometria = NwavGeometriaPagina.nwavDe(nwavHoja);
        double nwavPuntosPorPixel = 72.0 / NWAV_DPI_OCR;
        float nwavAltoPagina = nwavGeometria.nwavAltoVisual();
        try (PDPageContentStream nwavFlujo = new PDPageContentStream(nwavPd, nwavHoja,
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            nwavFlujo.saveGraphicsState();
            nwavFlujo.transform(nwavGeometria.nwavMatrizVisual());
            nwavFlujo.beginText();
            nwavFlujo.setRenderingMode(RenderingMode.NEITHER);
            for (NwavPalabra nwavPalabra : nwavPagina.nwavPalabras()) {
                String nwavTexto = nwavCodificable(nwavFuente, nwavPalabra.nwavTexto());
                if (nwavTexto.isBlank()) {
                    continue;
                }
                Rectangle nwavCaja = nwavPalabra.nwavCaja();
                float nwavX = (float) (nwavCaja.x * nwavPuntosPorPixel);
                float nwavAncho = (float) (nwavCaja.width * nwavPuntosPorPixel);
                float nwavAlto = (float) (nwavCaja.height * nwavPuntosPorPixel);
                float nwavTamano = Math.max(1f, nwavAlto);
                float nwavAnchoNatural = nwavFuente.getStringWidth(nwavTexto) / 1000f * nwavTamano;
                float nwavBase = (float) (nwavAltoPagina - (nwavCaja.y + nwavCaja.height) * nwavPuntosPorPixel)
                        + nwavAlto * 0.2f;
                nwavFlujo.setFont(nwavFuente, nwavTamano);
                nwavFlujo.setHorizontalScaling(nwavAnchoNatural > 0 ? 100f * nwavAncho / nwavAnchoNatural : 100f);
                nwavFlujo.setTextMatrix(Matrix.getTranslateInstance(nwavX, nwavBase));
                nwavFlujo.showText(nwavTexto);
            }
            nwavFlujo.endText();
            nwavFlujo.restoreGraphicsState();
        }
    }

    /**
     * Helvetica (WinAnsi) cubre espanol, ingles y la mayoria de idiomas europeos. Si el texto
     * tiene otros caracteres, se incrusta una fuente Unicode del sistema si existe.
     */
    private static PDFont nwavFuenteParaTexto(PDDocument nwavPd, List<NwavPaginaReconocida> nwavPaginas) {
        PDType1Font nwavHelvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        boolean nwavTodoLatino = nwavPaginas.stream().flatMap(nwavPagina -> nwavPagina.nwavPalabras().stream())
                .allMatch(nwavPalabra -> nwavSeCodifica(nwavHelvetica, nwavPalabra.nwavTexto()));
        if (nwavTodoLatino) {
            return nwavHelvetica;
        }
        for (String nwavRuta : NWAV_FUENTES_UNICODE) {
            Path nwavArchivo = Path.of(nwavRuta);
            if (Files.isRegularFile(nwavArchivo)) {
                try {
                    return PDType0Font.load(nwavPd, nwavArchivo.toFile());
                } catch (IOException | RuntimeException nwavError) {
                    nwavLog.debug("No se pudo cargar la fuente {}", nwavRuta, nwavError);
                }
            }
        }
        return nwavHelvetica; // los caracteres sin soporte se sustituyen por "?"
    }

    private static boolean nwavSeCodifica(PDFont nwavFuente, String nwavTexto) {
        try {
            nwavFuente.encode(nwavTexto);
            return true;
        } catch (IOException | IllegalArgumentException nwavError) {
            return false;
        }
    }

    private static String nwavCodificable(PDFont nwavFuente, String nwavTexto) {
        if (nwavSeCodifica(nwavFuente, nwavTexto)) {
            return nwavTexto;
        }
        StringBuilder nwavSalida = new StringBuilder();
        nwavTexto.codePoints().forEach(nwavCodigo -> {
            String nwavCaracter = new String(Character.toChars(nwavCodigo));
            nwavSalida.append(nwavSeCodifica(nwavFuente, nwavCaracter) ? nwavCaracter : "?");
        });
        return nwavSalida.toString();
    }

    // ================================================================== texto

    private static String nwavTextoDePagina(PDDocument nwavPd, int nwavIndice) {
        try {
            PDFTextStripper nwavExtractor = new PDFTextStripper();
            nwavExtractor.setStartPage(nwavIndice + 1);
            nwavExtractor.setEndPage(nwavIndice + 1);
            return nwavExtractor.getText(nwavPd);
        } catch (IOException | RuntimeException nwavError) {
            return "";
        }
    }

    /** Texto del documento: el reconocido (reconstruyendo lineas) o el que ya tenia. */
    private static String nwavTextoCompleto(int nwavTotal, String[] nwavExistente, List<NwavPaginaReconocida> nwavPaginas) {
        Map<Integer, NwavPaginaReconocida> nwavPorIndice = new java.util.HashMap<>();
        nwavPaginas.forEach(nwavPagina -> nwavPorIndice.put(nwavPagina.nwavIndice(), nwavPagina));
        StringBuilder nwavTexto = new StringBuilder();
        for (int nwavIndice = 0; nwavIndice < nwavTotal; nwavIndice++) {
            if (nwavIndice > 0) {
                nwavTexto.append("\n\n");
            }
            nwavTexto.append("--- Página ").append(nwavIndice + 1).append(" ---\n");
            NwavPaginaReconocida nwavPagina = nwavPorIndice.get(nwavIndice);
            nwavTexto.append(nwavPagina != null ? nwavLineas(nwavPagina.nwavPalabras()) : nwavExistente[nwavIndice].strip());
        }
        return nwavTexto.append('\n').toString();
    }

    /** Agrupa las palabras en lineas segun su posicion vertical. */
    static String nwavLineas(List<NwavPalabra> nwavPalabras) {
        List<NwavPalabra> nwavOrdenadas = new ArrayList<>(nwavPalabras);
        nwavOrdenadas.sort(Comparator.comparingDouble((NwavPalabra nwavP) -> nwavP.nwavCaja().getCenterY())
                .thenComparingInt(nwavP -> nwavP.nwavCaja().x));
        List<List<NwavPalabra>> nwavLineas = new ArrayList<>();
        for (NwavPalabra nwavPalabra : nwavOrdenadas) {
            List<NwavPalabra> nwavUltima = nwavLineas.isEmpty() ? null : nwavLineas.get(nwavLineas.size() - 1);
            if (nwavUltima != null && Math.abs(nwavUltima.get(0).nwavCaja().getCenterY() - nwavPalabra.nwavCaja().getCenterY())
                    < Math.max(nwavUltima.get(0).nwavCaja().height, nwavPalabra.nwavCaja().height) * 0.6) {
                nwavUltima.add(nwavPalabra);
            } else {
                nwavLineas.add(new ArrayList<>(List.of(nwavPalabra)));
            }
        }
        StringBuilder nwavTexto = new StringBuilder();
        for (List<NwavPalabra> nwavLinea : nwavLineas) {
            nwavLinea.sort(Comparator.comparingInt(nwavP -> nwavP.nwavCaja().x));
            nwavTexto.append(String.join(" ", nwavLinea.stream().map(NwavPalabra::nwavTexto).toList())).append('\n');
        }
        return nwavTexto.toString().strip();
    }
}
