package com.nwav.editorpdf.exception;

import org.junit.jupiter.api.Test;

import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class NwavTraductorErroresTest {
    @Test
    void nwavReconoceLaBaseDeDatosBloqueada() {
        RuntimeException nwavError = new RuntimeException("fallo al guardar",
                new SQLException("[SQLITE_BUSY] The database file is locked (database is locked)"));

        NwavEditorException nwavTraducido = NwavTraductorErrores.nwavTraducir(nwavError, null);

        assertThat(nwavTraducido).isInstanceOf(NwavBaseDatosBloqueadaException.class);
        assertThat(nwavTraducido.getMessage()).contains("bloqueada");
    }

    @Test
    void nwavReconoceFaltaDePermisos() {
        NwavEditorException nwavTraducido = NwavTraductorErrores.nwavTraducir(
                new AccessDeniedException("/x/y.pdf"), "y.pdf");

        assertThat(nwavTraducido).isInstanceOf(NwavSinPermisoException.class);
        assertThat(nwavTraducido.getMessage()).contains("y.pdf");
    }

    @Test
    void nwavReconoceArchivoEnUso() {
        FileSystemException nwavError = new FileSystemException("C:\\a.pdf", null,
                "The process cannot access the file because it is being used by another process");

        assertThat(NwavTraductorErrores.nwavTraducir(nwavError, "a.pdf"))
                .isInstanceOf(NwavArchivoEnUsoException.class);
    }

    @Test
    void nwavRespetaLosErroresPropios() {
        NwavPdfCorruptoException nwavPropio = new NwavPdfCorruptoException("z.pdf");

        assertThat(NwavTraductorErrores.nwavTraducir(new RuntimeException(nwavPropio), null)).isSameAs(nwavPropio);
    }

    @Test
    void nwavLosDemasErroresLlevanMensajeGenerico() {
        assertThat(NwavTraductorErrores.nwavMensaje(new IllegalStateException("algo raro")))
                .startsWith("Ocurrió un error inesperado")
                .contains("algo raro");
    }
}
