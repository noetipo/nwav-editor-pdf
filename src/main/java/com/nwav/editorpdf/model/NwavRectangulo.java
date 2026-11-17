package com.nwav.editorpdf.model;

public record NwavRectangulo(double nwavX, double nwavY, double nwavAncho, double nwavAlto) {
    public NwavRectangulo {
        if (nwavAncho < 0) {
            nwavX += nwavAncho;
            nwavAncho = -nwavAncho;
        }
        if (nwavAlto < 0) {
            nwavY += nwavAlto;
            nwavAlto = -nwavAlto;
        }
    }

    public static NwavRectangulo nwavEntre(NwavPunto nwavDesde, NwavPunto nwavHasta) {
        return new NwavRectangulo(nwavDesde.nwavX(), nwavDesde.nwavY(),
                nwavHasta.nwavX() - nwavDesde.nwavX(), nwavHasta.nwavY() - nwavDesde.nwavY());
    }

    public double nwavDerecha() {
        return nwavX + nwavAncho;
    }

    public double nwavAbajo() {
        return nwavY + nwavAlto;
    }

    public boolean nwavContiene(double nwavPx, double nwavPy) {
        return nwavPx >= nwavX && nwavPx <= nwavDerecha() && nwavPy >= nwavY && nwavPy <= nwavAbajo();
    }

    public boolean nwavSeCruzaCon(NwavRectangulo nwavOtro) {
        return nwavX < nwavOtro.nwavDerecha() && nwavOtro.nwavX < nwavDerecha()
                && nwavY < nwavOtro.nwavAbajo() && nwavOtro.nwavY < nwavAbajo();
    }
}
