package com.nwav.editorpdf.model;

/** Operaciones que quedan registradas en {@code nwav_historial_operacion}. */
public enum NwavTipoOperacion {
    NWAV_ROTAR("Rotar páginas"),
    NWAV_ELIMINAR("Eliminar páginas"),
    NWAV_DUPLICAR("Duplicar páginas"),
    NWAV_REORDENAR("Reordenar páginas"),
    NWAV_INSERTAR_PDF("Insertar desde PDF"),
    NWAV_INSERTAR_IMAGENES("Insertar imágenes"),
    NWAV_EXTRAER("Extraer páginas"),
    NWAV_DIVIDIR("Dividir PDF"),
    NWAV_UNIR("Unir PDF"),
    NWAV_GUARDAR("Guardar"),
    NWAV_DESHACER("Deshacer"),
    NWAV_REHACER("Rehacer");

    private final String nwavDescripcion;

    NwavTipoOperacion(String nwavDescripcion) {
        this.nwavDescripcion = nwavDescripcion;
    }

    public String getNwavDescripcion() {
        return nwavDescripcion;
    }

    @Override
    public String toString() {
        return nwavDescripcion;
    }
}
