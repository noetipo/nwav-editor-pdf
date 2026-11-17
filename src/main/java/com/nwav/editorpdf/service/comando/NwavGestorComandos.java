package com.nwav.editorpdf.service.comando;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public class NwavGestorComandos {
    public static final int NWAV_MAXIMO_PASOS = 100;

    private final Deque<NwavComando> nwavHechos = new ArrayDeque<>();
    private final Deque<NwavComando> nwavDeshechos = new ArrayDeque<>();
    private NwavComando nwavMarcaGuardado;
    private boolean nwavMarcaPerdida;

    public synchronized boolean nwavEjecutar(NwavComando nwavComando) {
        nwavComando.nwavEjecutar();
        if (!nwavComando.nwavTuvoEfecto()) {
            return false;
        }
        nwavHechos.push(nwavComando);
        if (nwavDeshechos.contains(nwavMarcaGuardado)) {
            nwavMarcaPerdida = true;
        }
        nwavDeshechos.clear();
        if (nwavHechos.size() > NWAV_MAXIMO_PASOS) {
            NwavComando nwavDescartado = nwavHechos.removeLast();
            if (nwavDescartado == nwavMarcaGuardado) {
                nwavMarcaPerdida = true;
            }
        }
        return true;
    }

    public synchronized Optional<NwavComando> nwavDeshacer() {
        if (nwavHechos.isEmpty()) {
            return Optional.empty();
        }
        NwavComando nwavComando = nwavHechos.peek();
        nwavComando.nwavDeshacer();
        nwavHechos.pop();
        nwavDeshechos.push(nwavComando);
        return Optional.of(nwavComando);
    }

    public synchronized Optional<NwavComando> nwavRehacer() {
        if (nwavDeshechos.isEmpty()) {
            return Optional.empty();
        }
        NwavComando nwavComando = nwavDeshechos.peek();
        nwavComando.nwavEjecutar();
        nwavDeshechos.pop();
        nwavHechos.push(nwavComando);
        return Optional.of(nwavComando);
    }

    public synchronized boolean nwavPuedeDeshacer() {
        return !nwavHechos.isEmpty();
    }

    public synchronized boolean nwavPuedeRehacer() {
        return !nwavDeshechos.isEmpty();
    }

    public synchronized Optional<String> nwavDescripcionDeshacer() {
        return Optional.ofNullable(nwavHechos.peek()).map(NwavComando::nwavDescripcion);
    }

    public synchronized Optional<String> nwavDescripcionRehacer() {
        return Optional.ofNullable(nwavDeshechos.peek()).map(NwavComando::nwavDescripcion);
    }

    public synchronized void nwavMarcarGuardado() {
        nwavMarcaGuardado = nwavHechos.peek();
        nwavMarcaPerdida = false;
    }

    public synchronized boolean nwavHayCambiosSinGuardar() {
        return nwavMarcaPerdida || nwavHechos.peek() != nwavMarcaGuardado;
    }

    public synchronized void nwavLimpiar() {
        nwavHechos.clear();
        nwavDeshechos.clear();
        nwavMarcaGuardado = null;
        nwavMarcaPerdida = false;
    }
}
