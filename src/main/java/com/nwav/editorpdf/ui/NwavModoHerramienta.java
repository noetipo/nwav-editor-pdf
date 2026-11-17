package com.nwav.editorpdf.ui;

/** Como se usa una herramienta desde la pantalla de inicio. */
public enum NwavModoHerramienta {
    /** Pagina comun: archivos -> opciones -> procesar -> resultados (admite lotes). */
    NWAV_LOTE,
    /** Abre un PDF en el editor y lanza una accion (panel o herramienta del visor). */
    NWAV_EDITOR,
    /** Vista propia (p. ej. comparar documentos). */
    NWAV_VISTA
}
