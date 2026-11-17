package com.nwav.editorpdf.exception;

public class NwavArchivoEnUsoException extends NwavEditorException {
    public NwavArchivoEnUsoException(String nwavArchivo) {
        super("«" + nwavArchivo + "» está en uso por otro programa. Ciérralo e inténtalo de nuevo.");
    }

    public NwavArchivoEnUsoException(String nwavArchivo, Throwable nwavCausa) {
        super("«" + nwavArchivo + "» está en uso por otro programa. Ciérralo e inténtalo de nuevo.", nwavCausa);
    }
}
