package com.nwav.editorpdf.exception;

import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.util.Locale;

/**
 * Convierte cualquier excepcion en una {@link NwavEditorException} con un mensaje
 * claro en espanol, para mostrarlo en la interfaz.
 */
public final class NwavTraductorErrores {

    private static final int NWAV_PROFUNDIDAD_MAXIMA = 12;

    private NwavTraductorErrores() {
    }

    public static String nwavMensaje(Throwable nwavError) {
        return nwavTraducir(nwavError, null).getMessage();
    }

    /**
     * @param nwavArchivo nombre del archivo involucrado, si se conoce; se usa en los
     *                    mensajes de errores de entrada/salida.
     */
    public static NwavEditorException nwavTraducir(Throwable nwavError, String nwavArchivo) {
        String nwavNombre = nwavArchivo == null ? "el archivo" : nwavArchivo;
        Throwable nwavActual = nwavError;
        for (int nwavNivel = 0; nwavActual != null && nwavNivel < NWAV_PROFUNDIDAD_MAXIMA; nwavNivel++) {
            if (nwavActual instanceof NwavEditorException nwavPropia) {
                return nwavPropia;
            }
            if (nwavEsBloqueoSqlite(nwavActual)) {
                return new NwavBaseDatosBloqueadaException(nwavError);
            }
            if (nwavActual instanceof NoSuchFileException) {
                return new NwavArchivoNoEncontradoException(nwavNombre, nwavError);
            }
            if (nwavActual instanceof AccessDeniedException) {
                return new NwavSinPermisoException(nwavNombre, nwavError);
            }
            if (nwavActual instanceof FileSystemException nwavFs && nwavEsArchivoEnUso(nwavFs.getReason())) {
                return new NwavArchivoEnUsoException(nwavNombre, nwavError);
            }
            if (nwavActual instanceof OutOfMemoryError) {
                return new NwavEditorException(
                        "No hay memoria suficiente para completar la operación. Reduce el zoom o cierra otros documentos.",
                        nwavError);
            }
            nwavActual = nwavActual.getCause();
        }
        String nwavDetalle = nwavError == null || nwavError.getMessage() == null
                ? (nwavError == null ? "desconocido" : nwavError.getClass().getSimpleName())
                : nwavError.getMessage();
        return new NwavEditorException("Ocurrió un error inesperado: " + nwavDetalle, nwavError);
    }

    static boolean nwavEsBloqueoSqlite(Throwable nwavError) {
        String nwavTexto = String.valueOf(nwavError.getMessage()).toUpperCase(Locale.ROOT);
        return nwavTexto.contains("SQLITE_BUSY") || nwavTexto.contains("SQLITE_LOCKED")
                || nwavTexto.contains("DATABASE IS LOCKED");
    }

    /** Mensajes que da el sistema operativo cuando otro proceso bloquea el archivo. */
    public static boolean nwavEsArchivoEnUso(String nwavMensaje) {
        if (nwavMensaje == null) {
            return false;
        }
        String nwavTexto = nwavMensaje.toLowerCase(Locale.ROOT);
        return nwavTexto.contains("being used by another process")
                || nwavTexto.contains("used by another process")
                || nwavTexto.contains("utilizado por otro proceso")
                || nwavTexto.contains("lock");
    }
}
