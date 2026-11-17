package com.nwav.editorpdf.exception;

public class NwavEditorException extends RuntimeException {
    public NwavEditorException(String nwavMensaje) {
        super(nwavMensaje);
    }

    public NwavEditorException(String nwavMensaje, Throwable nwavCausa) {
        super(nwavMensaje, nwavCausa);
    }
}
