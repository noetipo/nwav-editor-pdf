package com.nwav.editorpdf;

import javafx.application.Application;

import java.nio.file.Path;
import java.util.Locale;

public class NwavLauncher {

    public static void main(String[] nwavArgumentos) {
        nwavPrepararSistema();
        Application.launch(NwavJavaFxApplication.class, nwavArgumentos);
    }

    /**
     * En Windows los datos de la aplicacion van en %LOCALAPPDATA%\NWAV Editor PDF (lo
     * habitual alli) en lugar de una carpeta oculta en el perfil. Se puede forzar otra con
     * -Dnwav.datos.carpeta=...
     */
    static void nwavPrepararSistema() {
        if (System.getProperty("nwav.datos.carpeta") != null) {
            return;
        }
        String nwavCarpeta = nwavCarpetaDatos(System.getProperty("os.name", ""), System.getenv("LOCALAPPDATA"));
        if (nwavCarpeta != null) {
            System.setProperty("nwav.datos.carpeta", nwavCarpeta);
        }
    }

    /** Carpeta de datos propia del sistema, o null para usar la de application.properties. */
    static String nwavCarpetaDatos(String nwavSistema, String nwavLocalAppData) {
        if (nwavSistema.toLowerCase(Locale.ROOT).startsWith("windows") && nwavLocalAppData != null
                && !nwavLocalAppData.isBlank()) {
            return Path.of(nwavLocalAppData, "NWAV Editor PDF").toString();
        }
        return null;
    }
}
