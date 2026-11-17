package com.nwav.editorpdf.soporte;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base para pruebas con Spring: la base SQLite se crea en una carpeta temporal y
 * Flyway aplica las migraciones reales, sin tocar la carpeta de datos del usuario.
 */
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
