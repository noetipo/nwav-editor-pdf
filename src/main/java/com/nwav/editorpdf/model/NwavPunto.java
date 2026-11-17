package com.nwav.editorpdf.model;

/**
 * Punto en coordenadas visuales de una pagina: puntos PDF, origen arriba a la izquierda
 * y con la rotacion de la pagina ya aplicada (tal como se ve en pantalla).
 */
public record NwavPunto(double nwavX, double nwavY) {
}
