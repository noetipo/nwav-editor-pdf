package com.nwav.editorpdf.ui;

import javafx.application.HostServices;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/** Abre archivos y carpetas con las aplicaciones del sistema. */
@Component
public class NwavEscritorio {

    private HostServices nwavServicios;

    public void nwavRegistrar(HostServices nwavServicios) {
        this.nwavServicios = nwavServicios;
    }

    public void nwavAbrir(Path nwavRuta) {
        if (nwavServicios != null && nwavRuta != null) {
            nwavServicios.showDocument(nwavRuta.toUri().toString());
        }
    }

    /** Abre la carpeta que contiene el archivo (o la carpeta misma). */
    public void nwavMostrarCarpeta(Path nwavRuta) {
        if (nwavRuta == null) {
            return;
        }
        Path nwavCarpeta = Files.isDirectory(nwavRuta) ? nwavRuta : nwavRuta.toAbsolutePath().getParent();
        nwavAbrir(nwavCarpeta);
    }
}
