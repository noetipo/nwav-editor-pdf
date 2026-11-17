package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavModoZoom;
import com.nwav.editorpdf.model.NwavZoomPreferido;
import com.nwav.editorpdf.repository.NwavConfiguracionRepository;
import com.nwav.editorpdf.soporte.NwavPruebaConBaseDatos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class NwavConfiguracionServiceTest extends NwavPruebaConBaseDatos {

    @Autowired private NwavConfiguracionService nwavServicio;
    @Autowired private NwavConfiguracionRepository nwavRepositorio;

    @TempDir
    Path nwavCarpeta;

    @BeforeEach
    void nwavLimpiar() {
        nwavRepositorio.deleteAll();
        nwavServicio.nwavRecargar();
    }

    @Test
    void nwavSinDatosDevuelveLosValoresPorDefecto() {
        assertThat(nwavServicio.nwavTemaOscuro()).isFalse();
        assertThat(nwavServicio.nwavZoomPorDefecto()).isEqualTo(NwavZoomPreferido.NWAV_POR_DEFECTO);
        assertThat(nwavServicio.nwavRestaurarPosicion()).isTrue();
        assertThat(nwavServicio.nwavMaximoRecientes()).isEqualTo(NwavConfiguracionService.NWAV_MAXIMO_RECIENTES_DEFECTO);
        assertThat(nwavServicio.nwavCarpetaPorDefecto()).isEmpty();
    }

    @Test
    void nwavLasPreferenciasSobrevivenAUnaRecarga() {
        nwavServicio.nwavGuardarTemaOscuro(true);
        nwavServicio.nwavGuardarZoomPorDefecto(new NwavZoomPreferido(NwavModoZoom.NWAV_PORCENTAJE, 1.25));
        nwavServicio.nwavGuardarCarpetaPorDefecto(nwavCarpeta);
        nwavServicio.nwavGuardarRestaurarPosicion(false);

        nwavServicio.nwavRecargar();

        assertThat(nwavServicio.nwavTemaOscuro()).isTrue();
        assertThat(nwavServicio.nwavZoomPorDefecto().nwavFactor()).isEqualTo(1.25);
        assertThat(nwavServicio.nwavCarpetaPorDefecto()).contains(nwavCarpeta);
        assertThat(nwavServicio.nwavRestaurarPosicion()).isFalse();
        assertThat(nwavRepositorio.findById(NwavConfiguracionService.NWAV_CLAVE_TEMA)).isPresent();
    }

    @Test
    void nwavUnaCarpetaQueYaNoExisteSeIgnora() {
        nwavServicio.nwavGuardarCarpetaPorDefecto(nwavCarpeta.resolve("no-existe"));

        assertThat(nwavServicio.nwavCarpetaPorDefecto()).isEmpty();
    }

    @Test
    void nwavGuardarVacioEliminaLaPreferencia() {
        nwavServicio.nwavGuardar("nwav.prueba", "valor");
        nwavServicio.nwavGuardar("nwav.prueba", " ");

        assertThat(nwavServicio.nwavObtener("nwav.prueba")).isEmpty();
        assertThat(nwavRepositorio.findById("nwav.prueba")).isEmpty();
    }

    @Test
    void nwavElMaximoDeRecientesSeAcota() {
        nwavServicio.nwavGuardarMaximoRecientes(500);
        assertThat(nwavServicio.nwavMaximoRecientes()).isEqualTo(NwavConfiguracionService.NWAV_MAXIMO_RECIENTES_MAX);

        nwavServicio.nwavGuardar(NwavConfiguracionService.NWAV_CLAVE_MAXIMO_RECIENTES, "abc");
        assertThat(nwavServicio.nwavMaximoRecientes()).isEqualTo(NwavConfiguracionService.NWAV_MAXIMO_RECIENTES_DEFECTO);
    }
}
