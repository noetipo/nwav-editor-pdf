package com.nwav.editorpdf.exception;

/** El archivo no es un PDF valido o esta danado. */
public class NwavPdfCorruptoException extends NwavEditorException {

    public NwavPdfCorruptoException(String nwavArchivo) {
        super("«" + nwavArchivo + "» no es un PDF válido o está dañado.");
    }

    public NwavPdfCorruptoException(String nwavArchivo, Throwable nwavCausa) {
        super("«" + nwavArchivo + "» no es un PDF válido o está dañado.", nwavCausa);
    }
}
