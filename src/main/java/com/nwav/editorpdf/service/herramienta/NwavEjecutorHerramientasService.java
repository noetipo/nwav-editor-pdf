package com.nwav.editorpdf.service.herramienta;

import com.nwav.editorpdf.exception.NwavCanceladoException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.NwavAvance;
import com.nwav.editorpdf.service.NwavCancelacion;
import com.nwav.editorpdf.service.NwavHistorialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NwavEjecutorHerramientasService {
    public record NwavTrabajo(NwavTipoOperacion nwavTipo, boolean nwavPorArchivo, NwavProceso nwavProceso) {
    }

    private final NwavHistorialService nwavHistorial;

    public List<NwavResultadoArchivo> nwavEjecutar(NwavTrabajo nwavTrabajo, List<Path> nwavArchivos,
                                                   Map<String, String> nwavOpciones, Path nwavCarpetaSalida,
                                                   Map<Path, String> nwavContrasenas, NwavAvance nwavAvance,
                                                   NwavCancelacion nwavCancelacion) {
        List<NwavResultadoArchivo> nwavResultados = new ArrayList<>();
        if (!nwavTrabajo.nwavPorArchivo()) {
            nwavResultados.add(nwavUno(nwavTrabajo, null, nwavArchivos, nwavOpciones, nwavCarpetaSalida,
                    nwavContrasenas, nwavAvance, nwavCancelacion));
            return nwavResultados;
        }
        int nwavTotal = nwavArchivos.size();
        for (int nwavIndice = 0; nwavIndice < nwavTotal; nwavIndice++) {
            nwavCancelacion.nwavVerificar();
            Path nwavArchivo = nwavArchivos.get(nwavIndice);
            int nwavPosicion = nwavIndice;
            String nwavPrefijo = nwavTotal > 1 ? "[" + (nwavIndice + 1) + "/" + nwavTotal + "] " : "";
            NwavAvance nwavSubavance = (nwavHecho, nwavDeTotal, nwavMensaje) -> nwavAvance.nwavInformar(
                    nwavPosicion + (nwavDeTotal <= 0 ? 0 : Math.min(1, nwavHecho / nwavDeTotal)), nwavTotal,
                    nwavPrefijo + (nwavMensaje == null ? nwavArchivo.getFileName() : nwavMensaje));
            nwavResultados.add(nwavUno(nwavTrabajo, nwavArchivo, List.of(nwavArchivo), nwavOpciones,
                    nwavCarpetaSalida, nwavContrasenas, nwavSubavance, nwavCancelacion));
        }
        nwavAvance.nwavInformar(nwavTotal, nwavTotal, "Listo");
        return nwavResultados;
    }

    private NwavResultadoArchivo nwavUno(NwavTrabajo nwavTrabajo, Path nwavOrigen, List<Path> nwavEntradas,
                                         Map<String, String> nwavOpciones, Path nwavCarpetaSalida,
                                         Map<Path, String> nwavContrasenas, NwavAvance nwavAvance,
                                         NwavCancelacion nwavCancelacion) {
        Object nwavOrigenHistorial = nwavOrigen != null ? nwavOrigen : nwavEntradas;
        try {
            NwavResultadoProceso nwavResultado = nwavTrabajo.nwavProceso().nwavEjecutar(new NwavEntradaProceso(
                    nwavEntradas, nwavOpciones, nwavCarpetaSalida, nwavContrasenas, nwavAvance, nwavCancelacion));
            Object nwavDestinoHistorial = nwavResultado.nwavGenerados().size() == 1
                    ? nwavResultado.nwavGenerados().get(0)
                    : nwavResultado.nwavGenerados().isEmpty() ? null : nwavResultado.nwavGenerados();
            nwavHistorial.nwavRegistrarExito(nwavTrabajo.nwavTipo(), nwavOrigenHistorial, nwavDestinoHistorial,
                    nwavResultado.nwavDetalle());
            return new NwavResultadoArchivo(nwavOrigen, nwavResultado.nwavGenerados(), nwavResultado.nwavResumen(), null);
        } catch (NwavCanceladoException nwavCancelado) {
            throw nwavCancelado;
        } catch (Exception nwavError) {
            if (nwavCancelacion.nwavCancelado()) {
                throw new NwavCanceladoException();
            }
            nwavLog.warn("Fallo {} sobre {}: {}", nwavTrabajo.nwavTipo(), nwavOrigenHistorial, nwavError.toString());
            nwavHistorial.nwavRegistrarError(nwavTrabajo.nwavTipo(), nwavOrigenHistorial, null, nwavError);
            return new NwavResultadoArchivo(nwavOrigen, List.of(),
                    NwavTraductorErrores.nwavMensaje(nwavError), nwavError);
        }
    }
}
