package com.nwav.editorpdf.service;

import com.nwav.editorpdf.service.NwavConversionAPdfService.NwavOpcionesImagenes;
import com.nwav.editorpdf.service.NwavConversionAPdfService.NwavOrientacion;
import com.nwav.editorpdf.service.NwavConversionAPdfService.NwavTamanoHoja;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class NwavConversionAPdfTest {
    @TempDir
    Path nwavCarpeta;

    private final NwavConversionAPdfService nwavServicio = new NwavConversionAPdfService();

    private Path nwavImagen(String nwavNombre, int nwavAncho, int nwavAlto, String nwavFormato) throws Exception {
        BufferedImage nwavImagen = new BufferedImage(nwavAncho, nwavAlto, BufferedImage.TYPE_INT_RGB);
        Graphics2D nwavG = nwavImagen.createGraphics();
        nwavG.setColor(Color.ORANGE);
        nwavG.fillRect(0, 0, nwavAncho, nwavAlto);
        nwavG.setColor(Color.BLUE);
        nwavG.fillRect(0, 0, nwavAncho / 4, nwavAlto / 4);
        nwavG.dispose();
        Path nwavRuta = nwavCarpeta.resolve(nwavNombre);
        ImageIO.write(nwavImagen, nwavFormato, nwavRuta.toFile());
        return nwavRuta;
    }

    private Path nwavJpegConOrientacion(String nwavNombre, int nwavOrientacion) throws Exception {
        Path nwavPlano = nwavImagen("plano_" + nwavNombre, 400, 200, "jpg");
        byte[] nwavJpeg = Files.readAllBytes(nwavPlano);
        byte[] nwavTiff = {'M', 'M', 0, 42, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) nwavOrientacion, 0, 0,
                0, 0, 0, 0};
        ByteArrayOutputStream nwavApp1 = new ByteArrayOutputStream();
        nwavApp1.write(new byte[]{'E', 'x', 'i', 'f', 0, 0});
        nwavApp1.write(nwavTiff);
        int nwavLongitud = nwavApp1.size() + 2;
        ByteArrayOutputStream nwavSalida = new ByteArrayOutputStream();
        nwavSalida.write(nwavJpeg, 0, 2);
        nwavSalida.write(new byte[]{(byte) 0xFF, (byte) 0xE1, (byte) (nwavLongitud >> 8), (byte) nwavLongitud});
        nwavSalida.write(nwavApp1.toByteArray());
        nwavSalida.write(nwavJpeg, 2, nwavJpeg.length - 2);
        Path nwavRuta = nwavCarpeta.resolve(nwavNombre);
        Files.write(nwavRuta, nwavSalida.toByteArray());
        return nwavRuta;
    }

    @Test
    void nwavImagenesEnA4ConVariasPorPagina() throws Exception {
        List<Path> nwavImagenes = new ArrayList<>();
        for (int nwavIndice = 0; nwavIndice < 5; nwavIndice++) {
            nwavImagenes.add(nwavImagen("img" + nwavIndice + ".png", 300, 200, "png"));
        }
        Path nwavPdf = nwavServicio.nwavImagenesAPdf(nwavImagenes,
                new NwavOpcionesImagenes(NwavTamanoHoja.NWAV_A4, NwavOrientacion.NWAV_VERTICAL, 10, 4),
                nwavCarpeta.resolve("salida.pdf"), NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);

        try (PDDocument nwavPd = Loader.loadPDF(nwavPdf.toFile())) {
            assertThat(nwavPd.getNumberOfPages()).isEqualTo(2);
            PDPage nwavPrimera = nwavPd.getPage(0);
            assertThat(nwavPrimera.getMediaBox().getWidth()).isCloseTo(595.27f, within(0.5f));
            assertThat(nwavImagenesDe(nwavPrimera)).isEqualTo(4);
            assertThat(nwavImagenesDe(nwavPd.getPage(1))).isEqualTo(1);
        }
    }

    @Test
    void nwavOrientacionAutomaticaYHojaAjustada() throws Exception {
        Path nwavApaisada = nwavImagen("ancha.jpg", 800, 400, "jpg");
        Path nwavPdf = nwavServicio.nwavImagenesAPdf(List.of(nwavApaisada),
                new NwavOpcionesImagenes(NwavTamanoHoja.NWAV_A4, NwavOrientacion.NWAV_AUTO, 0, 1),
                nwavCarpeta.resolve("auto.pdf"), NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);
        Path nwavAjustada = nwavServicio.nwavImagenesAPdf(List.of(nwavApaisada),
                new NwavOpcionesImagenes(NwavTamanoHoja.NWAV_AJUSTAR, NwavOrientacion.NWAV_AUTO, 0, 1),
                nwavCarpeta.resolve("ajustada.pdf"), NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);

        try (PDDocument nwavPd = Loader.loadPDF(nwavPdf.toFile());
             PDDocument nwavAj = Loader.loadPDF(nwavAjustada.toFile())) {
            assertThat(nwavPd.getPage(0).getMediaBox().getWidth()).isGreaterThan(nwavPd.getPage(0).getMediaBox().getHeight());
            assertThat(nwavAj.getPage(0).getMediaBox().getWidth()).isCloseTo(600f, within(0.5f));
            assertThat(nwavAj.getPage(0).getMediaBox().getHeight()).isCloseTo(300f, within(0.5f));
            PDImageXObject nwavIncrustada = nwavPrimeraImagen(nwavPd.getPage(0));
            assertThat(nwavIncrustada.getCOSObject().getCOSName(COSName.FILTER)).isEqualTo(COSName.DCT_DECODE);
        }
    }

    @Test
    void nwavRespetaLaOrientacionExif() throws Exception {
        Path nwavGirada = nwavJpegConOrientacion("girada.jpg", 6);
        assertThat(NwavConversionAPdfService.nwavOrientacionExif(nwavGirada)).isEqualTo(6);

        Path nwavPdf = nwavServicio.nwavImagenesAPdf(List.of(nwavGirada),
                new NwavOpcionesImagenes(NwavTamanoHoja.NWAV_AJUSTAR, NwavOrientacion.NWAV_AUTO, 0, 1),
                nwavCarpeta.resolve("exif.pdf"), NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);
        try (PDDocument nwavPd = Loader.loadPDF(nwavPdf.toFile())) {
            PDImageXObject nwavImagen = nwavPrimeraImagen(nwavPd.getPage(0));
            assertThat(nwavImagen.getWidth()).isEqualTo(200);
            assertThat(nwavImagen.getHeight()).isEqualTo(400);
            BufferedImage nwavPixeles = nwavImagen.getImage();
            Color nwavArribaDerecha = new Color(nwavPixeles.getRGB(190, 10));
            assertThat(nwavArribaDerecha.getBlue()).isGreaterThan(150);
            assertThat(nwavArribaDerecha.getRed()).isLessThan(100);
        }
    }

    private static int nwavImagenesDe(PDPage nwavPagina) {
        int nwavCuenta = 0;
        for (COSName nwavNombre : nwavPagina.getResources().getXObjectNames()) {
            nwavCuenta++;
        }
        return nwavCuenta;
    }

    private static PDImageXObject nwavPrimeraImagen(PDPage nwavPagina) throws Exception {
        COSName nwavNombre = nwavPagina.getResources().getXObjectNames().iterator().next();
        return (PDImageXObject) nwavPagina.getResources().getXObject(nwavNombre);
    }
}
