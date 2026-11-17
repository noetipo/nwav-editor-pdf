package com.nwav.editorpdf.exception;

public class NwavPdfCorruptoException extends NwavEditorException {
    public NwavPdfCorruptoException(String nwavArchivo) {
        super("«" + nwavArchivo + "» no es un PDF válido o está dañado.");
    }

    public NwavPdfCorruptoException(String nwavArchivo, Throwable nwavCausa) {
        super("«" + nwavArchivo + "» no es un PDF válido o está dañado.", nwavCausa);
    }
}
