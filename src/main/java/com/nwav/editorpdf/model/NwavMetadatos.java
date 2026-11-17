package com.nwav.editorpdf.model;

import java.time.LocalDateTime;

public record NwavMetadatos(String nwavTitulo, String nwavAutor, String nwavAsunto, String nwavPalabrasClave,
                            String nwavCreador, String nwavProductor, LocalDateTime nwavCreacion,
                            LocalDateTime nwavModificacion) {
}
