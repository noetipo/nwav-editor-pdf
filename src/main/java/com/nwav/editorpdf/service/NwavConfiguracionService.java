package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavConfiguracion;
import com.nwav.editorpdf.model.NwavZoomPreferido;
import com.nwav.editorpdf.repository.NwavConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class NwavConfiguracionService {
    public static final String NWAV_CLAVE_TEMA = "nwav.tema";
    public static final String NWAV_CLAVE_ZOOM_DEFECTO = "nwav.zoom.defecto";
    public static final String NWAV_CLAVE_CARPETA_DEFECTO = "nwav.carpeta.defecto";
    public static final String NWAV_CLAVE_ULTIMA_CARPETA = "nwav.carpeta.ultima";
    public static final String NWAV_CLAVE_MAXIMO_RECIENTES = "nwav.recientes.maximo";
    public static final String NWAV_CLAVE_RESTAURAR_POSICION = "nwav.recientes.restaurar";
    public static final String NWAV_CLAVE_MINIATURAS_VISIBLES = "nwav.visor.miniaturas";

    public static final String NWAV_TEMA_CLARO = "claro";
    public static final String NWAV_TEMA_OSCURO = "oscuro";
    public static final int NWAV_MAXIMO_RECIENTES_DEFECTO = 15;
    public static final int NWAV_MAXIMO_RECIENTES_MIN = 5;
    public static final int NWAV_MAXIMO_RECIENTES_MAX = 50;

    private final NwavConfiguracionRepository nwavRepositorio;

    private final Map<String, String> nwavMemoria = new ConcurrentHashMap<>();
    private volatile boolean nwavCargada;

    public Optional<String> nwavObtener(String nwavClave) {
        nwavCargarSiHaceFalta();
        return Optional.ofNullable(nwavMemoria.get(nwavClave));
    }

    public String nwavObtener(String nwavClave, String nwavPorDefecto) {
        return nwavObtener(nwavClave).orElse(nwavPorDefecto);
    }

    @Transactional
    public void nwavGuardar(String nwavClave, String nwavValor) {
        nwavCargarSiHaceFalta();
        if (nwavValor == null || nwavValor.isBlank()) {
            nwavRepositorio.deleteById(nwavClave);
            nwavMemoria.remove(nwavClave);
        } else {
            nwavRepositorio.save(new NwavConfiguracion(nwavClave, nwavValor));
            nwavMemoria.put(nwavClave, nwavValor);
        }
    }

    @Transactional(readOnly = true)
    public synchronized void nwavRecargar() {
        nwavMemoria.clear();
        for (NwavConfiguracion nwavFila : nwavRepositorio.findAll()) {
            if (nwavFila.getNwavValor() != null) {
                nwavMemoria.put(nwavFila.getNwavClave(), nwavFila.getNwavValor());
            }
        }
        nwavCargada = true;
    }

    private void nwavCargarSiHaceFalta() {
        if (!nwavCargada) {
            synchronized (this) {
                if (!nwavCargada) {
                    nwavRecargar();
                }
            }
        }
    }

    public boolean nwavTemaOscuro() {
        return NWAV_TEMA_OSCURO.equals(nwavObtener(NWAV_CLAVE_TEMA, NWAV_TEMA_CLARO));
    }

    @Transactional
    public void nwavGuardarTemaOscuro(boolean nwavOscuro) {
        nwavGuardar(NWAV_CLAVE_TEMA, nwavOscuro ? NWAV_TEMA_OSCURO : NWAV_TEMA_CLARO);
    }

    public NwavZoomPreferido nwavZoomPorDefecto() {
        return NwavZoomPreferido.nwavDeserializar(nwavObtener(NWAV_CLAVE_ZOOM_DEFECTO, null));
    }

    @Transactional
    public void nwavGuardarZoomPorDefecto(NwavZoomPreferido nwavZoom) {
        nwavGuardar(NWAV_CLAVE_ZOOM_DEFECTO, nwavZoom.nwavSerializar());
    }

    public Optional<Path> nwavCarpetaPorDefecto() {
        return nwavCarpetaExistente(NWAV_CLAVE_CARPETA_DEFECTO);
    }

    @Transactional
    public void nwavGuardarCarpetaPorDefecto(Path nwavCarpeta) {
        nwavGuardar(NWAV_CLAVE_CARPETA_DEFECTO, nwavCarpeta == null ? null : nwavCarpeta.toString());
    }

    public Optional<Path> nwavUltimaCarpeta() {
        return nwavCarpetaExistente(NWAV_CLAVE_ULTIMA_CARPETA);
    }

    @Transactional
    public void nwavGuardarUltimaCarpeta(Path nwavCarpeta) {
        nwavGuardar(NWAV_CLAVE_ULTIMA_CARPETA, nwavCarpeta == null ? null : nwavCarpeta.toString());
    }

    public Optional<Path> nwavCarpetaInicial() {
        return nwavCarpetaPorDefecto().or(this::nwavUltimaCarpeta);
    }

    public int nwavMaximoRecientes() {
        try {
            int nwavValor = Integer.parseInt(nwavObtener(NWAV_CLAVE_MAXIMO_RECIENTES,
                    String.valueOf(NWAV_MAXIMO_RECIENTES_DEFECTO)));
            return Math.max(NWAV_MAXIMO_RECIENTES_MIN, Math.min(NWAV_MAXIMO_RECIENTES_MAX, nwavValor));
        } catch (NumberFormatException nwavError) {
            return NWAV_MAXIMO_RECIENTES_DEFECTO;
        }
    }

    @Transactional
    public void nwavGuardarMaximoRecientes(int nwavMaximo) {
        int nwavAcotado = Math.max(NWAV_MAXIMO_RECIENTES_MIN, Math.min(NWAV_MAXIMO_RECIENTES_MAX, nwavMaximo));
        nwavGuardar(NWAV_CLAVE_MAXIMO_RECIENTES, String.valueOf(nwavAcotado));
    }

    public boolean nwavRestaurarPosicion() {
        return Boolean.parseBoolean(nwavObtener(NWAV_CLAVE_RESTAURAR_POSICION, "true"));
    }

    @Transactional
    public void nwavGuardarRestaurarPosicion(boolean nwavRestaurar) {
        nwavGuardar(NWAV_CLAVE_RESTAURAR_POSICION, String.valueOf(nwavRestaurar));
    }

    public boolean nwavMiniaturasVisibles() {
        return Boolean.parseBoolean(nwavObtener(NWAV_CLAVE_MINIATURAS_VISIBLES, "true"));
    }

    @Transactional
    public void nwavGuardarMiniaturasVisibles(boolean nwavVisibles) {
        nwavGuardar(NWAV_CLAVE_MINIATURAS_VISIBLES, String.valueOf(nwavVisibles));
    }

    private Optional<Path> nwavCarpetaExistente(String nwavClave) {
        return nwavObtener(nwavClave)
                .map(Path::of)
                .filter(Files::isDirectory);
    }
}
