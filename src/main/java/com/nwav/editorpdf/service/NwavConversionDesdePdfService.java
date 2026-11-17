package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.service.NwavDisposicionPdf.NwavBloque;
import com.nwav.editorpdf.service.NwavDisposicionPdf.NwavEstilos;
import com.nwav.editorpdf.service.NwavDisposicionPdf.NwavLinea;
import com.nwav.editorpdf.service.NwavDisposicionPdf.NwavPagina;
import com.nwav.editorpdf.service.NwavDisposicionPdf.NwavPalabra;
import com.nwav.editorpdf.service.NwavDisposicionPdf.NwavTipoBloque;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * "Convertir desde PDF": paginas a imagen, extraer imagenes, texto/Markdown y Office
 * (Word, Excel, PowerPoint). La conversion a Office reconstruye la estructura a partir de
 * la posicion del texto: el resultado es editable, pero puede necesitar ajustes.
 */
@Service
@RequiredArgsConstructor
public class NwavConversionDesdePdfService {

    public static final String NWAV_AVISO_OFFICE = "La conversión reconstruye el documento a partir de la posición del "
            + "texto: es editable, pero el diseño puede necesitar ajustes (columnas, tablas complejas, fuentes).";

    private final NwavPdfDocumentoService nwavDocumentos;

    // ================================================================ a imagen

    /** Cada pagina a una imagen JPG o PNG. Devuelve los archivos creados. */
    public List<Path> nwavAImagenes(Path nwavEntrada, String nwavContrasena, String nwavFormato, int nwavDpi,
                                    float nwavCalidad, String nwavRangos, Path nwavCarpeta, NwavAvance nwavAvance,
                                    NwavCancelacion nwavCancelacion) {
        String nwavExtension = "png".equalsIgnoreCase(nwavFormato) ? "png" : "jpg";
        if (nwavDpi < 36 || nwavDpi > 600) {
            throw new NwavEditorException("La resolución debe estar entre 36 y 600 ppp.");
        }
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            List<Integer> nwavIndices = nwavPaginas(nwavPd, nwavRangos);
            PDFRenderer nwavRender = new PDFRenderer(nwavPd);
            int nwavCifras = Math.max(2, String.valueOf(nwavPd.getNumberOfPages()).length());
            List<Path> nwavCreados = new ArrayList<>();
            for (int nwavPaso = 0; nwavPaso < nwavIndices.size(); nwavPaso++) {
                nwavCancelacion.nwavVerificar();
                int nwavIndice = nwavIndices.get(nwavPaso);
                nwavAvance.nwavInformar(nwavPaso, nwavIndices.size(), "Página " + (nwavIndice + 1) + "…");
                BufferedImage nwavImagen = nwavRender.renderImageWithDPI(nwavIndice, nwavDpi,
                        "png".equals(nwavExtension) ? ImageType.ARGB : ImageType.RGB);
                String nwavSufijo = String.format(Locale.ROOT, "p%0" + nwavCifras + "d", nwavIndice + 1);
                Path nwavDestino = NwavEscrituraSegura.nwavRutaSalida(nwavEntrada, nwavCarpeta, nwavSufijo, nwavExtension);
                nwavCreados.add(NwavEscrituraSegura.nwavEscribir(nwavDestino, nwavTemporal ->
                        nwavGuardarImagen(nwavImagen, nwavExtension, nwavCalidad, nwavTemporal)));
            }
            return nwavCreados;
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    static void nwavGuardarImagen(BufferedImage nwavImagen, String nwavExtension, float nwavCalidad, Path nwavDestino)
            throws IOException {
        if ("png".equals(nwavExtension)) {
            ImageIO.write(nwavImagen, "png", nwavDestino.toFile());
            return;
        }
        ImageWriter nwavEscritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam nwavParametros = nwavEscritor.getDefaultWriteParam();
        nwavParametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        nwavParametros.setCompressionQuality(Math.max(0.1f, Math.min(1f, nwavCalidad)));
        try (OutputStream nwavSalida = Files.newOutputStream(nwavDestino);
             ImageOutputStream nwavFlujo = ImageIO.createImageOutputStream(nwavSalida)) {
            nwavEscritor.setOutput(nwavFlujo);
            nwavEscritor.write(null, new IIOImage(nwavImagen, null, null), nwavParametros);
        } finally {
            nwavEscritor.dispose();
        }
    }

    // ======================================================== extraer imagenes

    /**
     * Guarda las imagenes incrustadas tal como estan (los JPEG sin recomprimir). Se omiten
     * las repetidas y las menores de {@code nwavMinimoPx} pixeles de lado.
     */
    public List<Path> nwavExtraerImagenes(Path nwavEntrada, String nwavContrasena, int nwavMinimoPx, Path nwavCarpeta,
                                          NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            Set<COSStream> nwavVistas = new HashSet<>();
            Set<String> nwavHuellas = new HashSet<>(); // la misma imagen guardada como objetos distintos
            List<Path> nwavCreados = new ArrayList<>();
            int nwavTotal = nwavPd.getNumberOfPages();
            for (int nwavIndice = 0; nwavIndice < nwavTotal; nwavIndice++) {
                nwavCancelacion.nwavVerificar();
                nwavAvance.nwavInformar(nwavIndice, nwavTotal, "Buscando imágenes en la página " + (nwavIndice + 1) + "…");
                List<PDImageXObject> nwavImagenes = new ArrayList<>();
                nwavRecolectar(nwavPd.getPage(nwavIndice).getResources(), nwavImagenes, new HashSet<>());
                int nwavNumero = 0;
                for (PDImageXObject nwavImagen : nwavImagenes) {
                    if (!nwavVistas.add(nwavImagen.getCOSObject()) || nwavImagen.isStencil()
                            || nwavImagen.getWidth() < nwavMinimoPx || nwavImagen.getHeight() < nwavMinimoPx
                            || !nwavHuellas.add(nwavHuella(nwavImagen.getCOSObject()))) {
                        continue;
                    }
                    nwavNumero++;
                    String nwavSufijo = "p" + (nwavIndice + 1) + "_img" + nwavNumero;
                    nwavCreados.add(nwavGuardarIncrustada(nwavImagen, nwavEntrada, nwavCarpeta, nwavSufijo));
                }
            }
            if (nwavCreados.isEmpty()) {
                throw new NwavEditorException("El documento no tiene imágenes que extraer"
                        + (nwavMinimoPx > 1 ? " (de al menos " + nwavMinimoPx + " px)." : "."));
            }
            return nwavCreados;
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    private static String nwavHuella(COSStream nwavFlujo) throws IOException {
        try (InputStream nwavBytes = nwavFlujo.createRawInputStream()) {
            java.security.MessageDigest nwavSha = java.security.MessageDigest.getInstance("SHA-256");
            byte[] nwavBuffer = new byte[65536];
            int nwavLeidos;
            while ((nwavLeidos = nwavBytes.read(nwavBuffer)) > 0) {
                nwavSha.update(nwavBuffer, 0, nwavLeidos);
            }
            return java.util.HexFormat.of().formatHex(nwavSha.digest());
        } catch (java.security.NoSuchAlgorithmException nwavError) {
            throw new IllegalStateException(nwavError);
        }
    }

    private static void nwavRecolectar(PDResources nwavRecursos, List<PDImageXObject> nwavImagenes,
                                       Set<COSStream> nwavFormularios) throws IOException {
        if (nwavRecursos == null) {
            return;
        }
        for (COSName nwavNombre : nwavRecursos.getXObjectNames()) {
            PDXObject nwavObjeto = nwavRecursos.getXObject(nwavNombre);
            if (nwavObjeto instanceof PDImageXObject nwavImagen) {
                nwavImagenes.add(nwavImagen);
            } else if (nwavObjeto instanceof PDFormXObject nwavFormulario && nwavFormularios.add(nwavFormulario.getCOSObject())) {
                nwavRecolectar(nwavFormulario.getResources(), nwavImagenes, nwavFormularios);
            }
        }
    }

    private static Path nwavGuardarIncrustada(PDImageXObject nwavImagen, Path nwavEntrada, Path nwavCarpeta,
                                              String nwavSufijo) throws IOException {
        boolean nwavJpegPuro = "jpg".equals(nwavImagen.getSuffix()) && nwavImagen.getSoftMask() == null
                && !COSName.DEVICECMYK.equals(nwavImagen.getColorSpace().getCOSObject());
        if (nwavJpegPuro) {
            Path nwavDestino = NwavEscrituraSegura.nwavRutaSalida(nwavEntrada, nwavCarpeta, nwavSufijo, "jpg");
            return NwavEscrituraSegura.nwavEscribir(nwavDestino, nwavTemporal -> {
                try (InputStream nwavBytes = nwavImagen.getStream().createInputStream(List.of(COSName.DCT_DECODE.getName()))) {
                    Files.copy(nwavBytes, nwavTemporal, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            });
        }
        BufferedImage nwavPixeles = nwavImagen.getImage(); // aplica la mascara de transparencia
        Path nwavDestino = NwavEscrituraSegura.nwavRutaSalida(nwavEntrada, nwavCarpeta, nwavSufijo, "png");
        return NwavEscrituraSegura.nwavEscribir(nwavDestino, nwavTemporal -> ImageIO.write(nwavPixeles, "png",
                nwavTemporal.toFile()));
    }

    // ========================================================= texto/Markdown

    /** Texto plano, pagina a pagina (en orden de lectura). */
    public Path nwavATexto(Path nwavEntrada, String nwavContrasena, Path nwavDestino) {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            PDFTextStripper nwavExtractor = new PDFTextStripper();
            nwavExtractor.setSortByPosition(true);
            StringBuilder nwavTexto = new StringBuilder();
            for (int nwavPagina = 1; nwavPagina <= nwavPd.getNumberOfPages(); nwavPagina++) {
                nwavExtractor.setStartPage(nwavPagina);
                nwavExtractor.setEndPage(nwavPagina);
                if (nwavPagina > 1) {
                    nwavTexto.append("\n\f\n");
                }
                nwavTexto.append(nwavExtractor.getText(nwavPd).strip()).append('\n');
            }
            nwavComprobarTexto(nwavTexto.toString());
            return NwavEscrituraSegura.nwavEscribir(nwavDestino, nwavTemporal ->
                    Files.writeString(nwavTemporal, nwavTexto.toString(), StandardCharsets.UTF_8));
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    /** Markdown con titulos, listas, tablas y enlaces (las imagenes se omiten). */
    public Path nwavAMarkdown(Path nwavEntrada, String nwavContrasena, Path nwavDestino, NwavCancelacion nwavCancelacion) {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            List<NwavPagina> nwavPaginas = NwavDisposicionPdf.nwavAnalizar(nwavPd, nwavTodas(nwavPd), nwavCancelacion);
            String nwavMarkdown = nwavMarkdown(nwavPaginas);
            nwavComprobarTexto(nwavMarkdown);
            return NwavEscrituraSegura.nwavEscribir(nwavDestino, nwavTemporal ->
                    Files.writeString(nwavTemporal, nwavMarkdown, StandardCharsets.UTF_8));
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    /** Markdown de cada pagina (misma deteccion de estilos para todo el documento). */
    public List<String> nwavMarkdownPorPagina(Path nwavEntrada, String nwavContrasena, NwavCancelacion nwavCancelacion) {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            List<NwavPagina> nwavPaginas = NwavDisposicionPdf.nwavAnalizar(nwavPd, nwavTodas(nwavPd), nwavCancelacion);
            List<String> nwavResultado = new ArrayList<>();
            for (NwavPagina nwavPagina : nwavPaginas) {
                nwavResultado.add(nwavMarkdownConEstilos(List.of(nwavPagina), NwavDisposicionPdf.nwavEstilos(nwavPaginas)));
            }
            return nwavResultado;
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    static String nwavMarkdown(List<NwavPagina> nwavPaginas) {
        return nwavMarkdownConEstilos(nwavPaginas, NwavDisposicionPdf.nwavEstilos(nwavPaginas));
    }

    private static String nwavMarkdownConEstilos(List<NwavPagina> nwavPaginas, NwavEstilos nwavEstilos) {
        StringBuilder nwavMd = new StringBuilder();
        for (NwavPagina nwavPagina : nwavPaginas) {
            NwavTipoBloque nwavAnterior = null;
            for (NwavBloque nwavBloque : NwavDisposicionPdf.nwavBloques(nwavPagina, nwavEstilos)) {
                switch (nwavBloque.nwavTipo()) {
                    case NWAV_TITULO -> nwavMd.append("#".repeat(nwavBloque.nwavNivel())).append(' ')
                            .append(nwavEscaparMd(nwavBloque.nwavTexto())).append("\n\n");
                    case NWAV_LISTA -> {
                        if (nwavAnterior != NwavTipoBloque.NWAV_LISTA && !nwavMd.isEmpty() && !nwavMd.toString().endsWith("\n\n")) {
                            nwavMd.append('\n');
                        }
                        nwavMd.append(nwavBloque.nwavOrdenada() ? nwavBloque.nwavMarca().replace(')', '.') : "-").append(' ')
                                .append(nwavConEnlaces(nwavBloque, nwavPagina, nwavBloque.nwavTexto())).append('\n');
                    }
                    case NWAV_TABLA -> {
                        nwavFinLista(nwavMd, nwavAnterior);
                        List<List<String>> nwavFilas = nwavBloque.nwavFilas();
                        for (int nwavFila = 0; nwavFila < nwavFilas.size(); nwavFila++) {
                            nwavMd.append("| ").append(String.join(" | ", nwavFilas.get(nwavFila).stream()
                                    .map(nwavCelda -> nwavCelda.replace("|", "\\|")).toList())).append(" |\n");
                            if (nwavFila == 0) {
                                nwavMd.append("|").append(" --- |".repeat(nwavFilas.get(0).size())).append('\n');
                            }
                        }
                        nwavMd.append('\n');
                    }
                    case NWAV_PARRAFO -> {
                        nwavFinLista(nwavMd, nwavAnterior);
                        nwavMd.append(nwavConEnlaces(nwavBloque, nwavPagina, nwavEscaparMd(nwavBloque.nwavTexto())))
                                .append("\n\n");
                    }
                    default -> {
                        continue; // imagenes: no van en el Markdown
                    }
                }
                nwavAnterior = nwavBloque.nwavTipo();
            }
            nwavFinLista(nwavMd, nwavAnterior);
        }
        return nwavMd.toString().replaceAll("\n{3,}", "\n\n").strip() + "\n";
    }

    private static void nwavFinLista(StringBuilder nwavMd, NwavTipoBloque nwavAnterior) {
        if (nwavAnterior == NwavTipoBloque.NWAV_LISTA) {
            nwavMd.append('\n');
        }
    }

    private static String nwavEscaparMd(String nwavTexto) {
        return nwavTexto.replaceAll("^([#>+])", "\\\\$1");
    }

    /** Sustituye las palabras con enlace por [texto](uri). */
    private static String nwavConEnlaces(NwavBloque nwavBloque, NwavPagina nwavPagina, String nwavTexto) {
        if (nwavPagina.nwavEnlaces().isEmpty()) {
            return nwavTexto;
        }
        String nwavResultado = nwavTexto;
        for (NwavLinea nwavLinea : nwavBloque.nwavLineas()) {
            List<NwavPalabra> nwavPalabras = nwavLinea.nwavPalabras();
            int nwavIndice = 0;
            while (nwavIndice < nwavPalabras.size()) {
                String nwavUri = NwavDisposicionPdf.nwavEnlaceDe(nwavPalabras.get(nwavIndice), nwavPagina.nwavEnlaces());
                if (nwavUri == null) {
                    nwavIndice++;
                    continue;
                }
                StringBuilder nwavAncla = new StringBuilder(nwavPalabras.get(nwavIndice).nwavTexto());
                int nwavFin = nwavIndice + 1;
                while (nwavFin < nwavPalabras.size()
                        && nwavUri.equals(NwavDisposicionPdf.nwavEnlaceDe(nwavPalabras.get(nwavFin), nwavPagina.nwavEnlaces()))) {
                    nwavAncla.append(' ').append(nwavPalabras.get(nwavFin).nwavTexto());
                    nwavFin++;
                }
                String nwavPlano = nwavAncla.toString();
                if (nwavResultado.contains(nwavPlano) && !nwavResultado.contains("[" + nwavPlano + "]")) {
                    nwavResultado = nwavResultado.replaceFirst(java.util.regex.Pattern.quote(nwavPlano),
                            java.util.regex.Matcher.quoteReplacement("[" + nwavPlano + "](" + nwavUri + ")"));
                }
                nwavIndice = nwavFin;
            }
        }
        return nwavResultado;
    }

    private static void nwavComprobarTexto(String nwavTexto) {
        if (nwavTexto.isBlank()) {
            throw new NwavEditorException("El PDF no contiene texto seleccionable (¿es un escaneo?). "
                    + "Aplica primero la herramienta OCR.");
        }
    }

    // ==================================================================== Word

    public Path nwavAWord(Path nwavEntrada, String nwavContrasena, boolean nwavConImagenes, Path nwavDestino,
                          NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena);
             XWPFDocument nwavWord = new XWPFDocument()) {
            nwavAvance.nwavInformar(-1, 1, "Analizando el documento…");
            List<NwavPagina> nwavPaginas = NwavDisposicionPdf.nwavAnalizar(nwavPd, nwavTodas(nwavPd), nwavCancelacion);
            NwavEstilos nwavEstilos = NwavDisposicionPdf.nwavEstilos(nwavPaginas);
            boolean nwavHayTexto = nwavPaginas.stream().anyMatch(nwavP -> !nwavP.nwavLineas().isEmpty());
            if (!nwavHayTexto && !nwavConImagenes) {
                nwavComprobarTexto("");
            }
            for (int nwavIndice = 0; nwavIndice < nwavPaginas.size(); nwavIndice++) {
                nwavCancelacion.nwavVerificar();
                NwavPagina nwavPagina = nwavPaginas.get(nwavIndice);
                nwavAvance.nwavInformar(nwavIndice, nwavPaginas.size(), "Página " + (nwavIndice + 1) + "…");
                for (NwavBloque nwavBloque : NwavDisposicionPdf.nwavBloques(nwavPagina, nwavEstilos)) {
                    nwavBloqueWord(nwavWord, nwavBloque, nwavPagina, nwavEstilos, nwavConImagenes);
                }
                if (nwavIndice < nwavPaginas.size() - 1) {
                    nwavWord.createParagraph().createRun().addBreak(BreakType.PAGE);
                }
            }
            return NwavEscrituraSegura.nwavEscribir(nwavDestino, nwavTemporal -> {
                try (OutputStream nwavSalida = Files.newOutputStream(nwavTemporal)) {
                    nwavWord.write(nwavSalida);
                }
            });
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    private static void nwavBloqueWord(XWPFDocument nwavWord, NwavBloque nwavBloque, NwavPagina nwavPagina,
                                       NwavEstilos nwavEstilos, boolean nwavConImagenes) throws IOException {
        switch (nwavBloque.nwavTipo()) {
            case NWAV_TITULO -> {
                XWPFParagraph nwavParrafo = nwavWord.createParagraph();
                nwavParrafo.setSpacingBefore(240);
                nwavParrafo.setSpacingAfter(120);
                XWPFRun nwavRun = nwavParrafo.createRun();
                nwavRun.setText(nwavBloque.nwavTexto());
                nwavRun.setBold(true);
                nwavRun.setFontSize(Math.max(12, Math.round(nwavBloque.nwavLineas().get(0).nwavTamano())));
                nwavRun.setColor(nwavHex(nwavBloque.nwavLineas().get(0).nwavColor()));
            }
            case NWAV_LISTA -> {
                XWPFParagraph nwavParrafo = nwavWord.createParagraph();
                nwavParrafo.setIndentationLeft(360);
                nwavParrafo.setIndentationHanging(360);
                nwavParrafo.setSpacingAfter(60);
                XWPFRun nwavMarca = nwavParrafo.createRun();
                nwavMarca.setText((nwavBloque.nwavOrdenada() ? nwavBloque.nwavMarca() : "•") + "\t");
                nwavMarca.setFontSize(Math.round(nwavEstilos.nwavCuerpo()));
                nwavRunsConEnlaces(nwavParrafo, nwavBloque, nwavPagina, true);
            }
            case NWAV_TABLA -> {
                List<List<String>> nwavFilas = nwavBloque.nwavFilas();
                int nwavColumnas = nwavFilas.get(0).size();
                XWPFTable nwavTabla = nwavWord.createTable(nwavFilas.size(), nwavColumnas);
                nwavTabla.setWidth("100%");
                for (int nwavFila = 0; nwavFila < nwavFilas.size(); nwavFila++) {
                    XWPFTableRow nwavRow = nwavTabla.getRow(nwavFila);
                    for (int nwavColumna = 0; nwavColumna < nwavColumnas; nwavColumna++) {
                        XWPFParagraph nwavCelda = nwavRow.getCell(nwavColumna).getParagraphs().get(0);
                        XWPFRun nwavRun = nwavCelda.createRun();
                        nwavRun.setText(nwavFilas.get(nwavFila).get(nwavColumna));
                        nwavRun.setBold(nwavFila == 0);
                        nwavRun.setFontSize(Math.round(nwavEstilos.nwavCuerpo()));
                    }
                }
                nwavWord.createParagraph();
            }
            case NWAV_IMAGEN -> {
                if (!nwavConImagenes) {
                    return;
                }
                byte[] nwavPng = nwavPng(nwavBloque.nwavImagen().nwavImagen().getImage());
                double nwavAncho = Math.min(nwavBloque.nwavImagen().nwavZona().nwavAncho(), 468); // ancho util A4/Carta
                double nwavAlto = nwavAncho * nwavBloque.nwavImagen().nwavZona().nwavAlto()
                        / nwavBloque.nwavImagen().nwavZona().nwavAncho();
                XWPFRun nwavRun = nwavWord.createParagraph().createRun();
                try {
                    nwavRun.addPicture(new java.io.ByteArrayInputStream(nwavPng), Document.PICTURE_TYPE_PNG, "imagen.png",
                            org.apache.poi.util.Units.toEMU(nwavAncho), org.apache.poi.util.Units.toEMU(nwavAlto));
                } catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException nwavError) {
                    throw new IOException(nwavError);
                }
            }
            default -> {
                XWPFParagraph nwavParrafo = nwavWord.createParagraph();
                nwavParrafo.setSpacingAfter(160);
                nwavRunsConEnlaces(nwavParrafo, nwavBloque, nwavPagina, false);
            }
        }
    }

    /** Escribe el texto del bloque palabra a palabra, con negrita/cursiva/color y enlaces. */
    private static void nwavRunsConEnlaces(XWPFParagraph nwavParrafo, NwavBloque nwavBloque, NwavPagina nwavPagina,
                                           boolean nwavSaltarMarca) {
        boolean nwavPrimera = true;
        boolean nwavMarcaSaltada = !nwavSaltarMarca;
        XWPFRun nwavRun = null;
        String nwavFirmaActual = null;
        StringBuilder nwavPendiente = new StringBuilder();
        List<NwavLinea> nwavLineas = nwavBloque.nwavLineas();
        for (int nwavL = 0; nwavL < nwavLineas.size(); nwavL++) {
            List<NwavPalabra> nwavPalabras = nwavLineas.get(nwavL).nwavPalabras();
            for (int nwavP = 0; nwavP < nwavPalabras.size(); nwavP++) {
                NwavPalabra nwavPalabra = nwavPalabras.get(nwavP);
                if (!nwavMarcaSaltada) {
                    nwavMarcaSaltada = true;
                    continue;
                }
                String nwavUri = NwavDisposicionPdf.nwavEnlaceDe(nwavPalabra, nwavPagina.nwavEnlaces());
                String nwavFirma = nwavPalabra.nwavNegrita() + "|" + nwavPalabra.nwavCursiva() + "|" + nwavPalabra.nwavColor()
                        + "|" + Math.round(nwavPalabra.nwavTamano()) + "|" + nwavUri;
                String nwavSeparador = nwavPrimera ? "" : " ";
                String nwavTexto = nwavPalabra.nwavTexto();
                // Guion de fin de linea que parte una palabra: se une sin espacio ni guion.
                if (nwavP == nwavPalabras.size() - 1 && nwavL < nwavLineas.size() - 1 && nwavTexto.endsWith("-")
                        && nwavTexto.length() > 1) {
                    String nwavSiguiente = nwavLineas.get(nwavL + 1).nwavPalabras().get(0).nwavTexto();
                    if (!nwavSiguiente.isEmpty() && Character.isLowerCase(nwavSiguiente.charAt(0))) {
                        nwavTexto = nwavTexto.substring(0, nwavTexto.length() - 1) + "\u0000";
                    }
                }
                if (!nwavFirma.equals(nwavFirmaActual)) {
                    nwavVolcar(nwavRun, nwavPendiente);
                    nwavRun = nwavUri != null ? nwavParrafo.createHyperlinkRun(nwavUri) : nwavParrafo.createRun();
                    nwavRun.setBold(nwavPalabra.nwavNegrita());
                    nwavRun.setItalic(nwavPalabra.nwavCursiva());
                    nwavRun.setFontSize(Math.max(6, Math.round(nwavPalabra.nwavTamano())));
                    if (nwavUri != null) {
                        nwavRun.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE);
                        nwavRun.setColor("0563C1");
                    } else if (nwavPalabra.nwavColor() != 0) {
                        nwavRun.setColor(nwavHex(nwavPalabra.nwavColor()));
                    }
                    nwavFirmaActual = nwavFirma;
                }
                if (nwavPendiente.length() > 0 && nwavPendiente.charAt(nwavPendiente.length() - 1) == '\u0000') {
                    nwavPendiente.setLength(nwavPendiente.length() - 1);
                    nwavSeparador = "";
                }
                nwavPendiente.append(nwavSeparador).append(nwavTexto);
                nwavPrimera = false;
            }
        }
        if (nwavPendiente.length() > 0 && nwavPendiente.charAt(nwavPendiente.length() - 1) == '\u0000') {
            nwavPendiente.setCharAt(nwavPendiente.length() - 1, '-');
        }
        nwavVolcar(nwavRun, nwavPendiente);
    }

    private static void nwavVolcar(XWPFRun nwavRun, StringBuilder nwavPendiente) {
        if (nwavRun != null && nwavPendiente.length() > 0) {
            nwavRun.setText(nwavPendiente.toString().replace("\u0000", ""));
        }
        nwavPendiente.setLength(0);
    }

    private static String nwavHex(int nwavRgb) {
        return String.format(Locale.ROOT, "%06X", nwavRgb & 0xFFFFFF);
    }

    // =================================================================== Excel

    /**
     * Las tablas detectadas pasan a hojas de calculo (una hoja por pagina, o todo en una).
     * Los numeros se convierten a numero real (admite 1.234,56 y 1,234.56).
     */
    public Path nwavAExcel(Path nwavEntrada, String nwavContrasena, boolean nwavUnaHoja, boolean nwavSoloTablas,
                           Path nwavDestino, NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena);
             XSSFWorkbook nwavLibro = new XSSFWorkbook()) {
            nwavAvance.nwavInformar(-1, 1, "Buscando tablas…");
            List<NwavPagina> nwavPaginas = NwavDisposicionPdf.nwavAnalizar(nwavPd, nwavTodas(nwavPd), nwavCancelacion);
            NwavEstilos nwavEstilos = NwavDisposicionPdf.nwavEstilos(nwavPaginas);
            CellStyle nwavCabecera = nwavLibro.createCellStyle();
            Font nwavNegrita = nwavLibro.createFont();
            nwavNegrita.setBold(true);
            nwavCabecera.setFont(nwavNegrita);
            XSSFSheet nwavHoja = null;
            int nwavFilaActual = 0;
            for (NwavPagina nwavPagina : nwavPaginas) {
                nwavCancelacion.nwavVerificar();
                List<NwavBloque> nwavBloques = NwavDisposicionPdf.nwavBloques(nwavPagina, nwavEstilos).stream()
                        .filter(nwavB -> nwavB.nwavTipo() != NwavTipoBloque.NWAV_IMAGEN)
                        .filter(nwavB -> !nwavSoloTablas || nwavB.nwavTipo() == NwavTipoBloque.NWAV_TABLA).toList();
                if (nwavBloques.isEmpty()) {
                    continue;
                }
                if (nwavHoja == null || !nwavUnaHoja) {
                    nwavHoja = nwavLibro.createSheet(nwavUnaHoja ? "Datos" : "Página " + (nwavPagina.nwavIndice() + 1));
                    nwavFilaActual = 0;
                }
                for (NwavBloque nwavBloque : nwavBloques) {
                    if (nwavBloque.nwavTipo() == NwavTipoBloque.NWAV_TABLA) {
                        List<List<String>> nwavFilas = nwavBloque.nwavFilas();
                        for (int nwavFila = 0; nwavFila < nwavFilas.size(); nwavFila++) {
                            XSSFRow nwavRow = nwavHoja.createRow(nwavFilaActual++);
                            for (int nwavColumna = 0; nwavColumna < nwavFilas.get(nwavFila).size(); nwavColumna++) {
                                XSSFCell nwavCelda = nwavRow.createCell(nwavColumna);
                                nwavValorCelda(nwavCelda, nwavFilas.get(nwavFila).get(nwavColumna));
                                if (nwavFila == 0) {
                                    nwavCelda.setCellStyle(nwavCabecera);
                                }
                            }
                        }
                    } else {
                        nwavHoja.createRow(nwavFilaActual++).createCell(0).setCellValue(
                                nwavBloque.nwavTipo() == NwavTipoBloque.NWAV_LISTA
                                        ? "• " + nwavBloque.nwavTexto() : nwavBloque.nwavTexto());
                    }
                    nwavFilaActual++; // fila en blanco entre bloques
                }
                for (int nwavColumna = 0; nwavColumna < 12; nwavColumna++) {
                    nwavHoja.autoSizeColumn(nwavColumna);
                }
            }
            if (nwavHoja == null) {
                throw new NwavEditorException(nwavSoloTablas ? "No se encontraron tablas en el documento. Desmarca «Solo "
                        + "tablas» para exportar también el texto." : "El PDF no contiene texto seleccionable (¿es un escaneo?). "
                        + "Aplica primero la herramienta OCR.");
            }
            return NwavEscrituraSegura.nwavEscribir(nwavDestino, nwavTemporal -> {
                try (OutputStream nwavSalida = Files.newOutputStream(nwavTemporal)) {
                    nwavLibro.write(nwavSalida);
                }
            });
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    /** Pone un numero si el texto lo es (formato espanol o ingles, con % o moneda), si no texto. */
    static void nwavValorCelda(XSSFCell nwavCelda, String nwavTexto) {
        BigDecimal nwavNumero = nwavNumero(nwavTexto);
        if (nwavNumero != null) {
            nwavCelda.setCellValue(nwavNumero.doubleValue());
        } else {
            nwavCelda.setCellValue(nwavTexto);
        }
    }

    static BigDecimal nwavNumero(String nwavTexto) {
        if (nwavTexto == null) {
            return null;
        }
        String nwavLimpio = nwavTexto.strip().replace("\u00A0", "").replace(" ", "");
        boolean nwavNegativo = nwavLimpio.length() > 2 && nwavLimpio.startsWith("(") && nwavLimpio.endsWith(")");
        if (nwavNegativo) {
            nwavLimpio = nwavLimpio.substring(1, nwavLimpio.length() - 1);
        }
        nwavLimpio = nwavLimpio.replaceAll("^[€$£]|[€$£%]$", "");
        if (!nwavLimpio.matches("[-+]?[\\d.,]*\\d")) {
            return null;
        }
        int nwavComa = nwavLimpio.lastIndexOf(',');
        int nwavPunto = nwavLimpio.lastIndexOf('.');
        String nwavNormal;
        if (nwavComa >= 0 && nwavPunto >= 0) {
            nwavNormal = nwavComa > nwavPunto ? nwavLimpio.replace(".", "").replace(',', '.') : nwavLimpio.replace(",", "");
        } else if (nwavComa >= 0) {
            // "1,5" decimal; "1,234,567" miles
            nwavNormal = nwavLimpio.matches("[-+]?\\d{1,3}(,\\d{3})+") ? nwavLimpio.replace(",", "") : nwavLimpio.replace(',', '.');
        } else if (nwavPunto >= 0) {
            // "1.234.567" miles; "3.5" decimal; "1.234" se toma como miles (formato espanol)
            nwavNormal = nwavLimpio.matches("[-+]?\\d{1,3}(\\.\\d{3})+") ? nwavLimpio.replace(".", "") : nwavLimpio;
        } else {
            nwavNormal = nwavLimpio;
        }
        try {
            BigDecimal nwavValor = new BigDecimal(nwavNormal);
            return nwavNegativo ? nwavValor.negate() : nwavValor;
        } catch (NumberFormatException nwavError) {
            return null;
        }
    }

    // ============================================================== PowerPoint

    /**
     * Una diapositiva por pagina. Con {@code nwavTextoEditable} la pagina se dibuja como fondo
     * SIN su texto y el texto se anade en cuadros editables encima; si no, la pagina entera es
     * una imagen (identica al PDF, pero no editable).
     */
    public Path nwavAPowerPoint(Path nwavEntrada, String nwavContrasena, boolean nwavTextoEditable, Path nwavDestino,
                                NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena);
             XMLSlideShow nwavPresentacion = new XMLSlideShow()) {
            List<NwavPagina> nwavPaginas = NwavDisposicionPdf.nwavAnalizar(nwavPd, nwavTodas(nwavPd), nwavCancelacion);
            NwavPagina nwavPrimera = nwavPaginas.get(0);
            nwavPresentacion.setPageSize(new Dimension(Math.round(nwavPrimera.nwavAncho()), Math.round(nwavPrimera.nwavAlto())));
            PDFRenderer nwavRender = nwavTextoEditable ? new NwavRenderSinTexto(nwavPd) : new PDFRenderer(nwavPd);
            for (int nwavIndice = 0; nwavIndice < nwavPaginas.size(); nwavIndice++) {
                nwavCancelacion.nwavVerificar();
                NwavPagina nwavPagina = nwavPaginas.get(nwavIndice);
                nwavAvance.nwavInformar(nwavIndice, nwavPaginas.size(), "Diapositiva " + (nwavIndice + 1) + "…");
                XSLFSlide nwavDiapositiva = nwavPresentacion.createSlide();
                double nwavEscala = Math.min(nwavPrimera.nwavAncho() / nwavPagina.nwavAncho(),
                        nwavPrimera.nwavAlto() / nwavPagina.nwavAlto());
                double nwavDx = (nwavPrimera.nwavAncho() - nwavPagina.nwavAncho() * nwavEscala) / 2;
                double nwavDy = (nwavPrimera.nwavAlto() - nwavPagina.nwavAlto() * nwavEscala) / 2;
                BufferedImage nwavFondo = nwavRender.renderImageWithDPI(nwavIndice, 150, ImageType.RGB);
                XSLFPictureData nwavDatos = nwavPresentacion.addPicture(nwavJpeg(nwavFondo),
                        org.apache.poi.sl.usermodel.PictureData.PictureType.JPEG);
                XSLFPictureShape nwavImagen = nwavDiapositiva.createPicture(nwavDatos);
                nwavImagen.setAnchor(new Rectangle2D.Double(nwavDx, nwavDy, nwavPagina.nwavAncho() * nwavEscala,
                        nwavPagina.nwavAlto() * nwavEscala));
                if (nwavTextoEditable) {
                    for (NwavLinea nwavLinea : nwavPagina.nwavLineas()) {
                        nwavCuadroTexto(nwavDiapositiva, nwavLinea, nwavEscala, nwavDx, nwavDy);
                    }
                }
            }
            return NwavEscrituraSegura.nwavEscribir(nwavDestino, nwavTemporal -> {
                try (OutputStream nwavSalida = Files.newOutputStream(nwavTemporal)) {
                    nwavPresentacion.write(nwavSalida);
                }
            });
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    private static void nwavCuadroTexto(XSLFSlide nwavDiapositiva, NwavLinea nwavLinea, double nwavEscala, double nwavDx,
                                        double nwavDy) {
        double nwavTamano = nwavLinea.nwavTamano() * nwavEscala;
        XSLFTextBox nwavCuadro = nwavDiapositiva.createTextBox();
        nwavCuadro.setInsets(new org.apache.poi.sl.usermodel.Insets2D(0, 0, 0, 0));
        nwavCuadro.setWordWrap(false);
        double nwavAncho = (nwavLinea.nwavXFin() - nwavLinea.nwavX()) * nwavEscala + nwavTamano;
        nwavCuadro.setAnchor(new Rectangle2D.Double(nwavDx + nwavLinea.nwavX() * nwavEscala,
                nwavDy + (nwavLinea.nwavY() - nwavLinea.nwavTamano() * 0.95) * nwavEscala, nwavAncho, nwavTamano * 1.25));
        XSLFTextParagraph nwavParrafo = nwavCuadro.addNewTextParagraph();
        nwavParrafo.setSpaceBefore(0d);
        nwavParrafo.setSpaceAfter(0d);
        nwavParrafo.setLineSpacing(100d);
        XSLFTextRun nwavRun = nwavParrafo.addNewTextRun();
        nwavRun.setText(nwavLinea.nwavTexto());
        nwavRun.setFontSize(Math.max(4d, Math.round(nwavTamano * 2) / 2d));
        nwavRun.setBold(nwavLinea.nwavNegrita());
        nwavRun.setItalic(nwavLinea.nwavCursiva());
        nwavRun.setFontColor(new java.awt.Color(nwavLinea.nwavColor()));
        nwavCuadro.getTextParagraphs().stream().filter(nwavP -> nwavP != nwavParrafo && nwavP.getTextRuns().isEmpty())
                .toList().forEach(nwavVacio -> nwavCuadro.getTextBody().removeParagraph(
                        nwavCuadro.getTextParagraphs().indexOf(nwavVacio)));
    }

    /** Renderizador que dibuja todo menos el texto (fondo de las diapositivas editables). */
    private static final class NwavRenderSinTexto extends PDFRenderer {
        NwavRenderSinTexto(PDDocument nwavPd) {
            super(nwavPd);
        }

        @Override
        protected PageDrawer createPageDrawer(PageDrawerParameters nwavParametros) throws IOException {
            return new PageDrawer(nwavParametros) {
                @Override
                protected void showFontGlyph(org.apache.pdfbox.util.Matrix nwavMatriz,
                                             org.apache.pdfbox.pdmodel.font.PDFont nwavFuente, int nwavCodigo,
                                             org.apache.pdfbox.util.Vector nwavDesplazamiento) {
                    // el texto va en cuadros editables
                }

                @Override
                protected void showType3Glyph(org.apache.pdfbox.util.Matrix nwavMatriz,
                                              org.apache.pdfbox.pdmodel.font.PDType3Font nwavFuente, int nwavCodigo,
                                              org.apache.pdfbox.util.Vector nwavDesplazamiento) {
                    // idem
                }
            };
        }
    }

    // ================================================================ internos

    private static List<Integer> nwavTodas(PDDocument nwavPd) {
        return IntStream.range(0, nwavPd.getNumberOfPages()).boxed().toList();
    }

    private static List<Integer> nwavPaginas(PDDocument nwavPd, String nwavRangos) {
        return nwavRangos == null || nwavRangos.isBlank() ? nwavTodas(nwavPd)
                : NwavRangosPaginas.nwavIndices(nwavRangos, nwavPd.getNumberOfPages());
    }

    private static byte[] nwavPng(BufferedImage nwavImagen) throws IOException {
        ByteArrayOutputStream nwavSalida = new ByteArrayOutputStream();
        ImageIO.write(nwavImagen, "png", nwavSalida);
        return nwavSalida.toByteArray();
    }

    private static byte[] nwavJpeg(BufferedImage nwavImagen) throws IOException {
        Path nwavTemporal = Files.createTempFile("nwav-diapositiva", ".jpg");
        try {
            nwavGuardarImagen(nwavImagen, "jpg", 0.88f, nwavTemporal);
            return Files.readAllBytes(nwavTemporal);
        } finally {
            Files.deleteIfExists(nwavTemporal);
        }
    }
}
