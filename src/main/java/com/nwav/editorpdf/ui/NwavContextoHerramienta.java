package com.nwav.editorpdf.ui;

import javafx.stage.Window;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface NwavContextoHerramienta {
    List<Path> nwavArchivos();

    Map<String, String> nwavOpciones();

    String nwavContrasena(Path nwavArchivo);

    Window nwavVentana();
}
