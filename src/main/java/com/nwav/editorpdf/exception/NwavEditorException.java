package com.nwav.editorpdf.exception;

/**
 * Error esperado de la aplicacion. Su mensaje ya esta en espanol y se puede
 * mostrar tal cual al usuario.
 */
public class NwavEditorException extends RuntimeException {

    public NwavEditorException(String nwavMensaje) {
        super(nwavMensaje);
    }

    public NwavEditorException(String nwavMensaje, Throwable nwavCausa) {
        super(nwavMensaje, nwavCausa);
    }
}
