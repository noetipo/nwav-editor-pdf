package com.nwav.editorpdf.soporte;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

public final class NwavPdfConImagenes {
    private NwavPdfConImagenes() {
    }

    public static BufferedImage nwavFoto(int nwavAncho, int nwavAlto, boolean nwavConAlfa) {
        BufferedImage nwavImagen = new BufferedImage(nwavAncho, nwavAlto,
                nwavConAlfa ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D nwavLienzo = nwavImagen.createGraphics();
        nwavLienzo.setPaint(new GradientPaint(0, 0, new Color(30, 90, 200), nwavAncho, nwavAlto, new Color(240, 180, 40)));
        nwavLienzo.fillRect(0, 0, nwavAncho, nwavAlto);
        nwavLienzo.dispose();
        Random nwavAzar = new Random(7);
        for (int nwavY = 0; nwavY < nwavAlto; nwavY++) {
            for (int nwavX = 0; nwavX < nwavAncho; nwavX++) {
                int nwavPixel = nwavImagen.getRGB(nwavX, nwavY);
                int nwavRuido = nwavAzar.nextInt(25) - 12;
                int nwavR = Math.max(0, Math.min(255, ((nwavPixel >> 16) & 0xFF) + nwavRuido));
                int nwavG = Math.max(0, Math.min(255, ((nwavPixel >> 8) & 0xFF) + nwavRuido));
                int nwavB = Math.max(0, Math.min(255, (nwavPixel & 0xFF) + nwavRuido));
                int nwavAlfa = nwavConAlfa ? (nwavX < nwavAncho / 2 ? 255 : 90) : 255;
                nwavImagen.setRGB(nwavX, nwavY, (nwavAlfa << 24) | (nwavR << 16) | (nwavG << 8) | nwavB);
            }
        }
        return nwavImagen;
    }

    public static Path nwavCrearPesado(Path nwavCarpeta, String nwavNombre) throws IOException {
        Path nwavDestino = nwavCarpeta.resolve(nwavNombre);
        try (PDDocument nwavPd = new PDDocument()) {
            BufferedImage nwavFoto = nwavFoto(1800, 1200, false);
            BufferedImage nwavTransparente = nwavFoto(600, 400, true);
            BufferedImage nwavBinaria = new BufferedImage(400, 200, BufferedImage.TYPE_BYTE_BINARY);
            Graphics2D nwavLienzo = nwavBinaria.createGraphics();
            nwavLienzo.setColor(Color.WHITE);
            nwavLienzo.fillRect(0, 0, 400, 200);
            nwavLienzo.setColor(Color.BLACK);
            nwavLienzo.fillRect(50, 50, 300, 100);
            nwavLienzo.dispose();
            PDType1Font nwavFuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int nwavIndice = 0; nwavIndice < 2; nwavIndice++) {
                PDPage nwavPagina = new PDPage(PDRectangle.A4);
                nwavPd.addPage(nwavPagina);
                PDImageXObject nwavImagen = LosslessFactory.createFromImage(nwavPd, nwavFoto);
                try (PDPageContentStream nwavFlujo = new PDPageContentStream(nwavPd, nwavPagina)) {
                    nwavFlujo.drawImage(nwavImagen, 72, 400, 216, 144);
                    if (nwavIndice == 0) {
                        nwavFlujo.drawImage(LosslessFactory.createFromImage(nwavPd, nwavTransparente), 72, 200, 144, 96);
                        nwavFlujo.drawImage(LosslessFactory.createFromImage(nwavPd, nwavBinaria), 300, 200, 100, 50);
                    }
                    nwavFlujo.setNonStrokingColor(0.9f, 0.1f, 0.1f);
                    nwavFlujo.addRect(400, 700, 100, 50);
                    nwavFlujo.fill();
                    nwavFlujo.beginText();
                    nwavFlujo.setNonStrokingColor(0f, 0f, 0f);
                    nwavFlujo.setFont(nwavFuente, 20);
                    nwavFlujo.newLineAtOffset(72, 780);
                    nwavFlujo.showText("Pagina " + (nwavIndice + 1) + " texto de prueba");
                    nwavFlujo.endText();
                }
            }
            nwavPd.getDocumentInformation().setTitle("Documento pesado");
            nwavPd.save(nwavDestino.toFile());
        }
        return nwavDestino;
    }

    public static Path nwavCrearEscaneado(Path nwavCarpeta, String nwavNombre, String nwavTexto) throws IOException {
        Path nwavDestino = nwavCarpeta.resolve(nwavNombre);
        BufferedImage nwavImagen = new BufferedImage(2480, 1000, BufferedImage.TYPE_INT_RGB);
        Graphics2D nwavLienzo = nwavImagen.createGraphics();
        nwavLienzo.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        nwavLienzo.setColor(Color.WHITE);
        nwavLienzo.fillRect(0, 0, 2480, 1000);
        nwavLienzo.setColor(Color.BLACK);
        nwavLienzo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 110));
        nwavLienzo.drawString(nwavTexto, 150, 400);
        nwavLienzo.dispose();
        try (PDDocument nwavPd = new PDDocument()) {
            PDPage nwavPagina = new PDPage(PDRectangle.A4);
            nwavPd.addPage(nwavPagina);
            try (PDPageContentStream nwavFlujo = new PDPageContentStream(nwavPd, nwavPagina)) {
                nwavFlujo.drawImage(LosslessFactory.createFromImage(nwavPd, nwavImagen), 0, 500, 595.27f, 240);
            }
            nwavPd.save(nwavDestino.toFile());
        }
        return nwavDestino;
    }
}
