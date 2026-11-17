package com.nwav.editorpdf.ui;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;

/**
 * Convierte imagenes AWT (las que produce PDFBox) a imagenes JavaFX sin depender del
 * modulo javafx-swing. Se puede llamar desde un hilo de fondo.
 */
public final class NwavImagenFx {

    private NwavImagenFx() {
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
