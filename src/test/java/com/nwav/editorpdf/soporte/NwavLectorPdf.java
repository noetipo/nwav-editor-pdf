package com.nwav.editorpdf.soporte;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Utilidades de las pruebas para comprobar el contenido de un PDF. */
public final class NwavLectorPdf {

    private NwavLectorPdf() {
    }

    /**
     * Texto de cada pagina sin espacios ("Pagina1", "Pagina2"…). Se quitan los espacios
     * porque el extractor parte en lineas el texto de las paginas rotadas.
     */
    public static List<String> nwavTextos(PDDocument nwavDocumento) throws IOException {
        PDFTextStripper nwavExtractor = new PDFTextStripper();
        List<String> nwavTextos = new ArrayList<>();
        for (int nwavPagina = 1; nwavPagina <= nwavDocumento.getNumberOfPages(); nwavPagina++) {
            nwavExtractor.setStartPage(nwavPagina);
            nwavExtractor.setEndPage(nwavPagina);
            nwavTextos.add(nwavExtractor.getText(nwavDocumento).replaceAll("\\s+", ""));
        }
        return nwavTextos;
    }

    public static List<String> nwavTextos(Path nwavArchivo, String nwavContrasena) throws IOException {
        try (PDDocument nwavDocumento = nwavContrasena == null
                ? Loader.loadPDF(nwavArchivo.toFile())
                : Loader.loadPDF(nwavArchivo.toFile(), nwavContrasena)) {
            return nwavTextos(nwavDocumento);
        }
    }

    public static List<String> nwavTextos(Path nwavArchivo) throws IOException {
        return nwavTextos(nwavArchivo, null);
    }

    /** Crea una imagen PNG de un color, del tamano indicado. */
    public static Path nwavCrearPng(Path nwavCarpeta, String nwavNombre, int nwavAncho, int nwavAlto)
            throws IOException {
        BufferedImage nwavImagen = new BufferedImage(nwavAncho, nwavAlto, BufferedImage.TYPE_INT_RGB);
        Graphics2D nwavLienzo = nwavImagen.createGraphics();
        nwavLienzo.setColor(new Color(42, 120, 214));
        nwavLienzo.fillRect(0, 0, nwavAncho, nwavAlto);
        nwavLienzo.dispose();
        Path nwavDestino = nwavCarpeta.resolve(nwavNombre);
        ImageIO.write(nwavImagen, "png", nwavDestino.toFile());
        return nwavDestino;
    }
}
