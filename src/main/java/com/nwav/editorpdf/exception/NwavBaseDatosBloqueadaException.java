package com.nwav.editorpdf.exception;

/** SQLite no pudo obtener el bloqueo de escritura dentro del tiempo de espera. */
public class NwavBaseDatosBloqueadaException extends NwavEditorException {

    public NwavBaseDatosBloqueadaException(Throwable nwavCausa) {
        super("La base de datos está bloqueada por otro proceso. "
                + "Cierra otras ventanas del editor e inténtalo de nuevo.", nwavCausa);
    }
}
