package com.nwav.editorpdf.ui;

import java.nio.file.Path;
import java.util.List;

/** Controlador de una herramienta con vista propia (p. ej. comparar PDF). */
public interface NwavVistaHerramienta {
    void nwavPreparar(NwavDefinicionHerramienta nwavDefinicion, List<Path> nwavArchivos);
}
