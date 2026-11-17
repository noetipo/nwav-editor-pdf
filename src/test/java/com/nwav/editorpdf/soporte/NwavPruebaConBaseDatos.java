package com.nwav.editorpdf.soporte;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest
public abstract class NwavPruebaConBaseDatos {
    protected static Path nwavCarpetaDatos;

    @DynamicPropertySource
    static void nwavBaseTemporal(DynamicPropertyRegistry nwavRegistro) throws IOException {
        nwavCarpetaDatos = Files.createTempDirectory("nwav-prueba-db");
        nwavCarpetaDatos.toFile().deleteOnExit();
        nwavRegistro.add("nwav.datos.carpeta", () -> nwavCarpetaDatos.toString());
    }
}
