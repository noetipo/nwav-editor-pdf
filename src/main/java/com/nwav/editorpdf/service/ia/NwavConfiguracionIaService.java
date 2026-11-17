package com.nwav.editorpdf.service.ia;

import com.nwav.editorpdf.service.NwavConfiguracionService;
import com.nwav.editorpdf.service.herramienta.NwavDependenciasService.NwavEstado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Preferencias de la IA: desactivada por defecto; la clave de la API se guarda cifrada y
 * nunca se muestra ni se registra.
 */
@Service
@RequiredArgsConstructor
public class NwavConfiguracionIaService {

    public static final String NWAV_CLAVE_ACTIVA = "nwav.ia.activa";
    public static final String NWAV_CLAVE_API = "nwav.ia.clave.cifrada";
    public static final String NWAV_CLAVE_MODELO = "nwav.ia.modelo";
    public static final String NWAV_MODELO_DEFECTO = "claude-opus-5-5";

    private final NwavConfiguracionService nwavConfiguracion;
    private final NwavCifradoService nwavCifrado;

    public boolean nwavActiva() {
        return Boolean.parseBoolean(nwavConfiguracion.nwavObtener(NWAV_CLAVE_ACTIVA, "false"));
    }

    public boolean nwavTieneClave() {
        return !nwavConfiguracion.nwavObtener(NWAV_CLAVE_API, "").isBlank();
    }

    public String nwavModelo() {
        String nwavModelo = nwavConfiguracion.nwavObtener(NWAV_CLAVE_MODELO, "").strip();
        return nwavModelo.isEmpty() ? NWAV_MODELO_DEFECTO : nwavModelo;
    }

    /** Clave en claro, solo para la llamada; nunca se guarda descifrada. */
    String nwavClaveApi() {
        return nwavCifrado.nwavDescifrar(nwavConfiguracion.nwavObtener(NWAV_CLAVE_API, ""));
    }

    /**
     * @param nwavClaveNueva null o vacia = conservar la actual
     */
    public void nwavGuardar(boolean nwavActiva, String nwavClaveNueva, String nwavModelo) {
        if (nwavClaveNueva != null && !nwavClaveNueva.isBlank()) {
            nwavConfiguracion.nwavGuardar(NWAV_CLAVE_API, nwavCifrado.nwavCifrar(nwavClaveNueva.strip()));
        }
        nwavConfiguracion.nwavGuardar(NWAV_CLAVE_MODELO, nwavModelo == null ? "" : nwavModelo.strip());
        nwavConfiguracion.nwavGuardar(NWAV_CLAVE_ACTIVA, String.valueOf(nwavActiva));
    }

    public void nwavBorrarClave() {
        nwavConfiguracion.nwavGuardar(NWAV_CLAVE_API, "");
        nwavConfiguracion.nwavGuardar(NWAV_CLAVE_ACTIVA, "false");
    }

    /** Estado para las tarjetas de herramientas (desactivadas hasta que se configura). */
    public NwavEstado nwavEstado() {
        if (!nwavActiva()) {
            return new NwavEstado(false, "IA desactivada",
                    "Las funciones de IA envían el contenido del documento a Anthropic (Claude). Actívalas en "
                            + "Preferencias → Inteligencia artificial, con tu propia clave de API.");
        }
        if (!nwavTieneClave()) {
            return new NwavEstado(false, "Falta la clave de API",
                    "Escribe tu clave de API de Anthropic en Preferencias → Inteligencia artificial.");
        }
        return new NwavEstado(true, "IA activada (" + nwavModelo() + ")", "");
    }
}
