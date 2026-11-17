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
    NWAV_TEXTO("Agregar texto"),
    NWAV_IMAGEN("Insertar imagen"),
    NWAV_CUBRIR("Cubrir zona"),
    NWAV_RESALTAR("Resaltar"),
    NWAV_SUBRAYAR("Subrayar"),
    NWAV_TACHAR("Tachar"),
    NWAV_NOTA("Agregar nota"),
    NWAV_DIBUJO("Dibujo a mano alzada"),
    NWAV_FORMA("Agregar forma"),
    NWAV_FORMULARIO("Rellenar formulario"),
    NWAV_MARCA_AGUA("Marca de agua"),
    NWAV_ENCABEZADO("Encabezado, pie y numeración"),
    NWAV_PROTEGER("Proteger con contraseña"),
    NWAV_QUITAR_PROTECCION("Quitar protección"),
    NWAV_FIRMA("Insertar firma"),
    NWAV_METADATOS("Editar metadatos"),
    NWAV_RECORTAR("Recortar páginas"),
    NWAV_COMPRIMIR("Comprimir PDF"),
    NWAV_OCR("OCR"),
    NWAV_REPARAR("Reparar PDF"),
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
