package com.nwav.editorpdf.ui;

import java.io.File;
import java.util.List;

public interface NwavReceptorArchivos {
    boolean nwavAcepta(File nwavArchivo);

    void nwavRecibir(List<File> nwavArchivos);
}
