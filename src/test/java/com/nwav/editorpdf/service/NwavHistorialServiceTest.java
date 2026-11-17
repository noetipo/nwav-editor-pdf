package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.repository.NwavHistorialOperacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NwavHistorialServiceTest {
    @Test
    void nwavUnFalloDeLaBaseNoInterrumpeLaOperacion() {
        NwavHistorialOperacionRepository nwavRepositorio = mock(NwavHistorialOperacionRepository.class);
        when(nwavRepositorio.save(any())).thenThrow(
                new CannotAcquireLockException("[SQLITE_BUSY] database is locked"));
        NwavHistorialService nwavServicio = new NwavHistorialService(nwavRepositorio);

        assertThatCode(() -> nwavServicio.nwavRegistrarExito(NwavTipoOperacion.NWAV_UNIR,
                List.of(Path.of("/a.pdf"), Path.of("/b.pdf")), Path.of("/c.pdf"), "detalle"))
                .doesNotThrowAnyException();
        assertThatCode(() -> nwavServicio.nwavRegistrarError(NwavTipoOperacion.NWAV_GUARDAR, null, null,
                new IllegalStateException("x"))).doesNotThrowAnyException();
    }
}
