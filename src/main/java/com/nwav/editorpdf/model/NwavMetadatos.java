package com.nwav.editorpdf.model;

import java.time.LocalDateTime;

/** Propiedades editables del documento (diccionario Info del PDF). */
public record NwavMetadatos(String nwavTitulo, String nwavAutor, String nwavAsunto, String nwavPalabrasClave,
                            String nwavCreador, String nwavProductor, LocalDateTime nwavCreacion,
                            LocalDateTime nwavModificacion) {
}
