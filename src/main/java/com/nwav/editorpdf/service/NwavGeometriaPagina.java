package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavPunto;
import com.nwav.editorpdf.model.NwavRectangulo;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.util.Matrix;

final class NwavGeometriaPagina {
    private final float nwavIzquierda;
    private final float nwavAbajo;
    private final float nwavAncho;
    private final float nwavAlto;
    private final int nwavRotacion;

    private NwavGeometriaPagina(PDPage nwavPagina) {
        PDRectangle nwavCaja = nwavPagina.getCropBox();
        nwavIzquierda = nwavCaja.getLowerLeftX();
        nwavAbajo = nwavCaja.getLowerLeftY();
        nwavAncho = nwavCaja.getWidth();
        nwavAlto = nwavCaja.getHeight();
        nwavRotacion = NwavDocumentoPdf.nwavRotacionNormalizada(nwavPagina);
    }

    static NwavGeometriaPagina nwavDe(PDPage nwavPagina) {
        return new NwavGeometriaPagina(nwavPagina);
    }

    float nwavAnchoVisual() {
        return nwavRotacion == 90 || nwavRotacion == 270 ? nwavAlto : nwavAncho;
    }

    float nwavAltoVisual() {
        return nwavRotacion == 90 || nwavRotacion == 270 ? nwavAncho : nwavAlto;
    }

    Matrix nwavMatrizVisual() {
        return switch (nwavRotacion) {
            case 90 -> new Matrix(0, 1, -1, 0, nwavIzquierda + nwavAncho, nwavAbajo);
            case 180 -> new Matrix(-1, 0, 0, -1, nwavIzquierda + nwavAncho, nwavAbajo + nwavAlto);
            case 270 -> new Matrix(0, -1, 1, 0, nwavIzquierda, nwavAbajo + nwavAlto);
            default -> new Matrix(1, 0, 0, 1, nwavIzquierda, nwavAbajo);
        };
    }

    float[] nwavAPdf(double nwavX, double nwavY) {
        Matrix nwavMatriz = nwavMatrizVisual();
        float nwavXv = (float) nwavX;
        float nwavYArriba = (float) (nwavAltoVisual() - nwavY);
        return new float[]{
                nwavMatriz.getScaleX() * nwavXv + nwavMatriz.getShearX() * nwavYArriba + nwavMatriz.getTranslateX(),
                nwavMatriz.getShearY() * nwavXv + nwavMatriz.getScaleY() * nwavYArriba + nwavMatriz.getTranslateY()};
    }

    float[] nwavAPdf(NwavPunto nwavPunto) {
        return nwavAPdf(nwavPunto.nwavX(), nwavPunto.nwavY());
    }

    PDRectangle nwavRectanguloPdf(NwavRectangulo nwavZona) {
        float[] nwavA = nwavAPdf(nwavZona.nwavX(), nwavZona.nwavY());
        float[] nwavB = nwavAPdf(nwavZona.nwavDerecha(), nwavZona.nwavAbajo());
        float nwavMinX = Math.min(nwavA[0], nwavB[0]);
        float nwavMinY = Math.min(nwavA[1], nwavB[1]);
        return new PDRectangle(nwavMinX, nwavMinY, Math.abs(nwavA[0] - nwavB[0]), Math.abs(nwavA[1] - nwavB[1]));
    }

    NwavRectangulo nwavRectanguloVisual(PDRectangle nwavPdf) {
        try {
            java.awt.geom.AffineTransform nwavInversa = nwavMatrizVisual().createAffineTransform().createInverse();
            java.awt.geom.Point2D nwavA = nwavInversa.transform(
                    new java.awt.geom.Point2D.Float(nwavPdf.getLowerLeftX(), nwavPdf.getLowerLeftY()), null);
            java.awt.geom.Point2D nwavB = nwavInversa.transform(
                    new java.awt.geom.Point2D.Float(nwavPdf.getUpperRightX(), nwavPdf.getUpperRightY()), null);
            double nwavX = Math.min(nwavA.getX(), nwavB.getX());
            double nwavYArriba = Math.max(nwavA.getY(), nwavB.getY());
            return new NwavRectangulo(nwavX, nwavAltoVisual() - nwavYArriba, Math.abs(nwavA.getX() - nwavB.getX()),
                    Math.abs(nwavA.getY() - nwavB.getY()));
        } catch (java.awt.geom.NoninvertibleTransformException nwavError) {
            return null;
        }
    }

    float[] nwavQuadPoints(NwavRectangulo nwavZona) {
        float[] nwavAi = nwavAPdf(nwavZona.nwavX(), nwavZona.nwavY());
        float[] nwavAd = nwavAPdf(nwavZona.nwavDerecha(), nwavZona.nwavY());
        float[] nwavBi = nwavAPdf(nwavZona.nwavX(), nwavZona.nwavAbajo());
        float[] nwavBd = nwavAPdf(nwavZona.nwavDerecha(), nwavZona.nwavAbajo());
        return new float[]{nwavAi[0], nwavAi[1], nwavAd[0], nwavAd[1], nwavBi[0], nwavBi[1], nwavBd[0], nwavBd[1]};
    }
}
