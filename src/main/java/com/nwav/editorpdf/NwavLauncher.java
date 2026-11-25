package com.nwav.editorpdf;

import javafx.application.Application;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

public class NwavLauncher {
    private static FileChannel nwavCanalInstancia;
    private static FileLock nwavCandadoInstancia;

    public static void main(String[] nwavArgumentos) {
        nwavPrepararSistema();
        if (!nwavTomarInstancia()) {
            return;
        }
        Application.launch(NwavJavaFxApplication.class, nwavArgumentos);
    }

    static boolean nwavTomarInstancia() {
        String nwavCarpeta = System.getProperty("nwav.datos.carpeta");
        if (nwavCarpeta == null || nwavCarpeta.isBlank()) {
            return true;
        }
        try {
            Path nwavDirectorio = Path.of(nwavCarpeta);
            Files.createDirectories(nwavDirectorio);
            nwavCanalInstancia = FileChannel.open(nwavDirectorio.resolve("editor-pdf-instancia.lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            nwavCandadoInstancia = nwavCanalInstancia.tryLock();
            return nwavCandadoInstancia != null;
        } catch (IOException nwavError) {
            return true;
        }
    }

    static void nwavPrepararSistema() {
        if (System.getProperty("nwav.datos.carpeta") != null) {
            return;
        }
        String nwavCarpeta = nwavCarpetaDatos(System.getProperty("os.name", ""), System.getenv("LOCALAPPDATA"));
        if (nwavCarpeta != null) {
            System.setProperty("nwav.datos.carpeta", nwavCarpeta);
        }
    }

    static String nwavCarpetaDatos(String nwavSistema, String nwavLocalAppData) {
        if (nwavSistema.toLowerCase(Locale.ROOT).startsWith("windows") && nwavLocalAppData != null
                && !nwavLocalAppData.isBlank()) {
            return Path.of(nwavLocalAppData, "NWAV Editor PDF").toString();
        }
        return null;
    }
}
