package com.nwav.editorpdf.exception;

public class NwavContrasenaRequeridaException extends NwavEditorException {
    public NwavContrasenaRequeridaException(String nwavArchivo) {
        super("«" + nwavArchivo + "» está protegido con contraseña.");
    }

    public NwavContrasenaRequeridaException(String nwavArchivo, Throwable nwavCausa) {
        super("«" + nwavArchivo + "» está protegido con contraseña.", nwavCausa);
    }
}
