package com.nwav.editorpdf.exception;

/** Sin permisos de lectura o escritura sobre la ruta. */
public class NwavSinPermisoException extends NwavEditorException {

    public NwavSinPermisoException(String nwavArchivo) {
        super("No tienes permisos suficientes sobre «" + nwavArchivo + "».");
    }

    public NwavSinPermisoException(String nwavArchivo, Throwable nwavCausa) {
        super("No tienes permisos suficientes sobre «" + nwavArchivo + "».", nwavCausa);
    }
}
