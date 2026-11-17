package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.comando.NwavComando;
import com.nwav.editorpdf.service.comando.NwavGestorComandos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NwavGestorComandosTest {
    private record NwavComandoFalso(String nwavNombre, List<String> nwavBitacora) implements NwavComando {
        @Override
        public void nwavEjecutar() {
            nwavBitacora.add("+" + nwavNombre);
        }

        @Override
        public void nwavDeshacer() {
            nwavBitacora.add("-" + nwavNombre);
        }

        @Override
        public NwavTipoOperacion nwavTipo() {
            return NwavTipoOperacion.NWAV_ROTAR;
        }

        @Override
        public String nwavDescripcion() {
            return nwavNombre;
        }
    }

    private final List<String> nwavBitacora = new ArrayList<>();
    private final NwavGestorComandos nwavGestor = new NwavGestorComandos();

    private NwavComandoFalso nwavComando(String nwavNombre) {
        return new NwavComandoFalso(nwavNombre, nwavBitacora);
    }

    @Test
    void nwavDeshaceYRehaceEnOrdenInverso() {
        nwavGestor.nwavEjecutar(nwavComando("a"));
        nwavGestor.nwavEjecutar(nwavComando("b"));
        nwavGestor.nwavDeshacer();
        nwavGestor.nwavDeshacer();
        nwavGestor.nwavRehacer();

        assertThat(nwavBitacora).containsExactly("+a", "+b", "-b", "-a", "+a");
        assertThat(nwavGestor.nwavDescripcionDeshacer()).contains("a");
        assertThat(nwavGestor.nwavDescripcionRehacer()).contains("b");
    }

    @Test
    void nwavUnComandoNuevoBorraLaPilaDeRehacer() {
        nwavGestor.nwavEjecutar(nwavComando("a"));
        nwavGestor.nwavDeshacer();
        nwavGestor.nwavEjecutar(nwavComando("b"));

        assertThat(nwavGestor.nwavPuedeRehacer()).isFalse();
    }

    @Test
    void nwavSabeCuandoSeVuelveAlEstadoGuardado() {
        nwavGestor.nwavEjecutar(nwavComando("a"));
        nwavGestor.nwavMarcarGuardado();
        assertThat(nwavGestor.nwavHayCambiosSinGuardar()).isFalse();

        nwavGestor.nwavEjecutar(nwavComando("b"));
        assertThat(nwavGestor.nwavHayCambiosSinGuardar()).isTrue();
        nwavGestor.nwavDeshacer();
        assertThat(nwavGestor.nwavHayCambiosSinGuardar()).isFalse();
        nwavGestor.nwavDeshacer();
        assertThat(nwavGestor.nwavHayCambiosSinGuardar()).isTrue();

        nwavGestor.nwavEjecutar(nwavComando("c"));
        nwavGestor.nwavDeshacer();
        assertThat(nwavGestor.nwavHayCambiosSinGuardar()).isTrue();
    }

    @Test
    void nwavLimitaElNumeroDePasos() {
        for (int nwavIndice = 0; nwavIndice < NwavGestorComandos.NWAV_MAXIMO_PASOS + 10; nwavIndice++) {
            nwavGestor.nwavEjecutar(nwavComando("c" + nwavIndice));
        }
        int nwavDeshechos = 0;
        while (nwavGestor.nwavDeshacer().isPresent()) {
            nwavDeshechos++;
        }
        assertThat(nwavDeshechos).isEqualTo(NwavGestorComandos.NWAV_MAXIMO_PASOS);
    }
}
