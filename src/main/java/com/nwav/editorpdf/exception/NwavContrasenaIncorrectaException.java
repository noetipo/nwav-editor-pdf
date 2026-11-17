package com.nwav.editorpdf.exception;

public class NwavContrasenaIncorrectaException extends NwavEditorException {
    public NwavContrasenaIncorrectaException(String nwavArchivo) {
        super("La contraseña de «" + nwavArchivo + "» no es correcta.");
    }

    public NwavContrasenaIncorrectaException(String nwavArchivo, Throwable nwavCausa) {
        super("La contraseña de «" + nwavArchivo + "» no es correcta.", nwavCausa);
    }
}
