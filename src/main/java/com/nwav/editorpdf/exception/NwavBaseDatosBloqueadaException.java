package com.nwav.editorpdf.exception;

public class NwavBaseDatosBloqueadaException extends NwavEditorException {
    public NwavBaseDatosBloqueadaException(Throwable nwavCausa) {
        super("La base de datos está bloqueada por otro proceso. "
                + "Cierra otras ventanas del editor e inténtalo de nuevo.", nwavCausa);
    }
}
