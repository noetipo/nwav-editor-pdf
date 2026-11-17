package com.nwav.editorpdf.exception;

public class NwavArchivoNoEncontradoException extends NwavEditorException {
    public NwavArchivoNoEncontradoException(String nwavArchivo) {
        super("No se encontró el archivo «" + nwavArchivo + "». Puede que se haya movido o eliminado.");
    }

    public NwavArchivoNoEncontradoException(String nwavArchivo, Throwable nwavCausa) {
        super("No se encontró el archivo «" + nwavArchivo + "». Puede que se haya movido o eliminado.", nwavCausa);
    }
}
