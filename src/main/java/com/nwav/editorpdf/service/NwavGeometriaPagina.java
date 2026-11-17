package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavPunto;
import com.nwav.editorpdf.model.NwavRectangulo;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.util.Matrix;

/**
 * Traduce coordenadas "visuales" (como se ve la pagina en pantalla: puntos PDF, origen
 * arriba a la izquierda, rotacion ya aplicada) al espacio de usuario del PDF, que tiene el
 * origen abajo a la izquierda de la CropBox y no conoce la rotacion de la pagina.
 * <p>
 * Para dibujar contenido se usa {@link #nwavMatrizVisual()}: tras aplicarla, se dibuja con
 * coordenadas visuales con el eje Y hacia arriba ({@code yArriba = altoVisual - yVisual}) y
 * el resultado aparece derecho en pantalla aunque la pagina este rotada.
 */
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

    /** Matriz de (x visual, y visual hacia arriba) a espacio PDF. */
    Matrix nwavMatrizVisual() {
        return switch (nwavRotacion) {
            case 90 -> new Matrix(0, 1, -1, 0, nwavIzquierda + nwavAncho, nwavAbajo);
            case 180 -> new Matrix(-1, 0, 0, -1, nwavIzquierda + nwavAncho, nwavAbajo + nwavAlto);
            case 270 -> new Matrix(0, -1, 1, 0, nwavIzquierda, nwavAbajo + nwavAlto);
            default -> new Matrix(1, 0, 0, 1, nwavIzquierda, nwavAbajo);
        };
    }

    /** Punto visual (Y hacia abajo) en espacio PDF: {x, y}. */
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

    /** Rectangulo visual en espacio PDF (siempre alineado a los ejes: la rotacion es de 90°). */
    PDRectangle nwavRectanguloPdf(NwavRectangulo nwavZona) {
        float[] nwavA = nwavAPdf(nwavZona.nwavX(), nwavZona.nwavY());
        float[] nwavB = nwavAPdf(nwavZona.nwavDerecha(), nwavZona.nwavAbajo());
        float nwavMinX = Math.min(nwavA[0], nwavB[0]);
        float nwavMinY = Math.min(nwavA[1], nwavB[1]);
        return new PDRectangle(nwavMinX, nwavMinY, Math.abs(nwavA[0] - nwavB[0]), Math.abs(nwavA[1] - nwavB[1]));
    }

    /**
     * Cuatro esquinas de un rectangulo visual como QuadPoints de PDF, en el orden que usan
     * los lectores: arriba-izquierda, arriba-derecha, abajo-izquierda, abajo-derecha (visual).
     */
    float[] nwavQuadPoints(NwavRectangulo nwavZona) {
        float[] nwavAi = nwavAPdf(nwavZona.nwavX(), nwavZona.nwavY());
        float[] nwavAd = nwavAPdf(nwavZona.nwavDerecha(), nwavZona.nwavY());
        float[] nwavBi = nwavAPdf(nwavZona.nwavX(), nwavZona.nwavAbajo());
        float[] nwavBd = nwavAPdf(nwavZona.nwavDerecha(), nwavZona.nwavAbajo());
        return new float[]{nwavAi[0], nwavAi[1], nwavAd[0], nwavAd[1], nwavBi[0], nwavBi[1], nwavBd[0], nwavBd[1]};
    }
}
