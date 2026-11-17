package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.model.NwavHistorialOperacion;
import com.nwav.editorpdf.model.NwavResultadoOperacion;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.repository.NwavHistorialOperacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Registro de operaciones en {@code nwav_historial_operacion}. Registrar nunca hace
 * fallar la operacion original: si la base no responde, solo se anota en el log. Por eso
 * los metodos de registro no son transaccionales: un fallo dentro de una transaccion la
 * marcaria para revertir y el error saltaria igualmente al confirmarla.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NwavHistorialService {

    private static final int NWAV_LARGO_ORIGEN = 4096;
    private static final int NWAV_LARGO_DESTINO = 2048;
    private static final int NWAV_LARGO_DETALLE = 2048;

    private final NwavHistorialOperacionRepository nwavRepositorio;

    public void nwavRegistrarExito(NwavTipoOperacion nwavTipo, Object nwavOrigen, Object nwavDestino,
                                   String nwavDetalle) {
        nwavRegistrar(nwavTipo, nwavOrigen, nwavDestino, NwavResultadoOperacion.NWAV_EXITO, nwavDetalle);
    }

    public void nwavRegistrarError(NwavTipoOperacion nwavTipo, Object nwavOrigen, Object nwavDestino,
                                   Throwable nwavError) {
        nwavRegistrar(nwavTipo, nwavOrigen, nwavDestino, NwavResultadoOperacion.NWAV_ERROR,
                NwavTraductorErrores.nwavMensaje(nwavError));
    }

    public void nwavRegistrar(NwavTipoOperacion nwavTipo, Object nwavOrigen, Object nwavDestino,
                              NwavResultadoOperacion nwavResultado, String nwavDetalle) {
        try {
            NwavHistorialOperacion nwavFila = new NwavHistorialOperacion();
            nwavFila.setNwavTipo(nwavTipo);
            nwavFila.setNwavArchivoOrigen(nwavRecortar(nwavTexto(nwavOrigen), NWAV_LARGO_ORIGEN));
            nwavFila.setNwavArchivoDestino(nwavRecortar(nwavTexto(nwavDestino), NWAV_LARGO_DESTINO));
            nwavFila.setNwavFecha(LocalDateTime.now());
            nwavFila.setNwavResultado(nwavResultado);
            nwavFila.setNwavDetalle(nwavRecortar(nwavDetalle, NWAV_LARGO_DETALLE));
            nwavRepositorio.save(nwavFila);
        } catch (RuntimeException nwavError) {
            nwavLog.warn("No se pudo registrar la operacion {} en el historial", nwavTipo, nwavError);
        }
    }

    @Transactional(readOnly = true)
    public List<NwavHistorialOperacion> nwavListar() {
        return nwavRepositorio.findAllByOrderByNwavFechaDescNwavIdDesc();
    }

    @Transactional
    public long nwavLimpiar() {
        long nwavCantidad = nwavRepositorio.count();
        nwavRepositorio.deleteAllInBatch();
        return nwavCantidad;
    }

    /** Admite una ruta, una lista de rutas (se unen con "; ") o texto. */
    private static String nwavTexto(Object nwavValor) {
        if (nwavValor == null) {
            return null;
        }
        if (nwavValor instanceof Path nwavRuta) {
            return nwavRuta.toAbsolutePath().normalize().toString();
        }
        if (nwavValor instanceof List<?> nwavLista) {
            return String.join("; ", nwavLista.stream().map(NwavHistorialService::nwavTexto).toList());
        }
        return nwavValor.toString();
    }

    private static String nwavRecortar(String nwavTexto, int nwavMaximo) {
        if (nwavTexto == null || nwavTexto.length() <= nwavMaximo) {
            return nwavTexto;
        }
        return nwavTexto.substring(0, nwavMaximo - 1) + "…";
    }
}
