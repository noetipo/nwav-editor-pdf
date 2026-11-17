package com.nwav.editorpdf.exception;

/** El usuario cancelo la operacion. No es un error: la interfaz lo muestra como aviso. */
public class NwavCanceladoException extends NwavEditorException {

    public NwavCanceladoException() {
        super("Operación cancelada.");
    }
}
