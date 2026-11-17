package com.nwav.editorpdf.model;

public enum NwavTipoCampo {
    NWAV_TEXTO("Texto"),
    NWAV_CASILLA("Casilla"),
    NWAV_OPCION("Opción"),
    NWAV_LISTA("Lista"),
    NWAV_FIRMA_DIGITAL("Firma digital");

    private final String nwavDescripcion;

    NwavTipoCampo(String nwavDescripcion) {
        this.nwavDescripcion = nwavDescripcion;
    }

    public String getNwavDescripcion() {
        return nwavDescripcion;
    }
}
