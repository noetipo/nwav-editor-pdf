package com.nwav.editorpdf.model;

import java.util.List;

public record NwavCampoFormulario(String nwavNombre, String nwavEtiqueta, NwavTipoCampo nwavTipo, String nwavValor,
                                  List<String> nwavOpciones, boolean nwavMultilinea, boolean nwavSoloLectura,
                                  boolean nwavRequerido, int nwavPagina, NwavRectangulo nwavZona) {
    public static final String NWAV_MARCADA = "true";
    public static final String NWAV_DESMARCADA = "false";
}
