package com.nwav.editorpdf.ui;

import java.io.File;
import java.util.List;

/** Vista que recibe los archivos que se arrastran a la ventana (en vez de abrirlos en el visor). */
public interface NwavReceptorArchivos {

    boolean nwavAcepta(File nwavArchivo);

    void nwavRecibir(List<File> nwavArchivos);
}
