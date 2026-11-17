package com.nwav.editorpdf.ui;

import javafx.application.HostServices;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

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

    public void nwavMostrarCarpeta(Path nwavRuta) {
        if (nwavRuta == null) {
            return;
        }
        Path nwavCarpeta = Files.isDirectory(nwavRuta) ? nwavRuta : nwavRuta.toAbsolutePath().getParent();
        nwavAbrir(nwavCarpeta);
    }
}
