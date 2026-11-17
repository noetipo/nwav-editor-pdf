package com.nwav.editorpdf.model;

import java.util.List;

public record NwavCoincidencia(int nwavPagina, List<NwavRectangulo> nwavZonas, String nwavContexto) {
}
