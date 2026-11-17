package com.nwav.editorpdf.ui;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;

public final class NwavImagenFx {
    private NwavImagenFx() {
    }

    public static byte[] nwavAPng(Image nwavImagen) {
        int nwavAncho = (int) nwavImagen.getWidth();
        int nwavAlto = (int) nwavImagen.getHeight();
        int[] nwavPixeles = new int[nwavAncho * nwavAlto];
        nwavImagen.getPixelReader().getPixels(0, 0, nwavAncho, nwavAlto, PixelFormat.getIntArgbInstance(),
                nwavPixeles, 0, nwavAncho);
        BufferedImage nwavDestino = new BufferedImage(nwavAncho, nwavAlto, BufferedImage.TYPE_INT_ARGB);
        nwavDestino.setRGB(0, 0, nwavAncho, nwavAlto, nwavPixeles, 0, nwavAncho);
        try (java.io.ByteArrayOutputStream nwavSalida = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(nwavDestino, "png", nwavSalida);
            return nwavSalida.toByteArray();
        } catch (java.io.IOException nwavError) {
            throw new java.io.UncheckedIOException(nwavError);
        }
    }

    public static Image nwavConvertir(BufferedImage nwavOrigen) {
        int nwavAncho = nwavOrigen.getWidth();
        int nwavAlto = nwavOrigen.getHeight();
        int[] nwavPixeles = nwavOrigen.getRGB(0, 0, nwavAncho, nwavAlto, null, 0, nwavAncho);
        WritableImage nwavDestino = new WritableImage(nwavAncho, nwavAlto);
        nwavDestino.getPixelWriter().setPixels(0, 0, nwavAncho, nwavAlto,
                PixelFormat.getIntArgbInstance(), nwavPixeles, 0, nwavAncho);
        return nwavDestino;
    }
}
