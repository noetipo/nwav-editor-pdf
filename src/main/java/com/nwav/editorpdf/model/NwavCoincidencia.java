package com.nwav.editorpdf.model;

import java.util.List;

/** Resultado de una busqueda: una aparicion del texto, con sus rectangulos (una por linea). */
public record NwavCoincidencia(int nwavPagina, List<NwavRectangulo> nwavZonas, String nwavContexto) {
}
