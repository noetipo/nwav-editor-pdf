package com.nwav.editorpdf.exception;

public class NwavSinPermisoException extends NwavEditorException {
    public NwavSinPermisoException(String nwavArchivo) {
        super("No tienes permisos suficientes sobre «" + nwavArchivo + "».");
    }

    public NwavSinPermisoException(String nwavArchivo, Throwable nwavCausa) {
        super("No tienes permisos suficientes sobre «" + nwavArchivo + "».", nwavCausa);
    }
}
