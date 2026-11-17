package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavArchivoReciente;
import com.nwav.editorpdf.model.NwavModoZoom;
import com.nwav.editorpdf.model.NwavZoomPreferido;
import com.nwav.editorpdf.repository.NwavArchivoRecienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NwavArchivoRecienteService {
    private final NwavArchivoRecienteRepository nwavRepositorio;
    private final NwavConfiguracionService nwavConfiguracion;

    public record NwavRecienteVista(NwavArchivoReciente nwavArchivo, boolean nwavDisponible) {
    }

    public static String nwavNormalizar(Path nwavRuta) {
        return nwavRuta.toAbsolutePath().normalize().toString();
    }

    @Transactional(readOnly = true)
    public Optional<NwavArchivoReciente> nwavBuscar(Path nwavRuta) {
        return nwavRepositorio.findByNwavRuta(nwavNormalizar(nwavRuta));
    }

    @Transactional
    public NwavArchivoReciente nwavRegistrarApertura(Path nwavRuta) {
        String nwavClave = nwavNormalizar(nwavRuta);
        NwavArchivoReciente nwavReciente = nwavRepositorio.findByNwavRuta(nwavClave).orElseGet(() -> {
            NwavArchivoReciente nwavNuevo = new NwavArchivoReciente();
            NwavZoomPreferido nwavZoom = nwavConfiguracion.nwavZoomPorDefecto();
            nwavNuevo.setNwavRuta(nwavClave);
            nwavNuevo.setNwavUltimaPagina(0);
            nwavNuevo.setNwavModoZoom(nwavZoom.nwavModo());
            nwavNuevo.setNwavZoom(nwavZoom.nwavFactor());
            return nwavNuevo;
        });
        nwavReciente.setNwavNombre(Path.of(nwavClave).getFileName().toString());
        nwavReciente.setNwavUltimaApertura(LocalDateTime.now());
        NwavArchivoReciente nwavGuardado = nwavRepositorio.save(nwavReciente);
        nwavRecortar();
        return nwavGuardado;
    }

    @Transactional
    public void nwavGuardarEstado(Path nwavRuta, int nwavPagina, NwavModoZoom nwavModo, double nwavZoom) {
        nwavRepositorio.findByNwavRuta(nwavNormalizar(nwavRuta)).ifPresent(nwavReciente -> {
            NwavZoomPreferido nwavAcotado = new NwavZoomPreferido(nwavModo, nwavZoom);
            nwavReciente.setNwavUltimaPagina(Math.max(0, nwavPagina));
            nwavReciente.setNwavModoZoom(nwavAcotado.nwavModo());
            nwavReciente.setNwavZoom(nwavAcotado.nwavFactor());
        });
    }

    @Transactional(readOnly = true)
    public List<NwavRecienteVista> nwavListar() {
        return nwavRepositorio.findAllByOrderByNwavUltimaAperturaDesc().stream()
                .limit(nwavConfiguracion.nwavMaximoRecientes())
                .map(nwavArchivo -> new NwavRecienteVista(nwavArchivo,
                        Files.isRegularFile(Path.of(nwavArchivo.getNwavRuta()))))
                .toList();
    }

    @Transactional
    public void nwavQuitar(Long nwavId) {
        nwavRepositorio.deleteById(nwavId);
    }

    @Transactional
    public int nwavQuitarNoDisponibles() {
        List<NwavArchivoReciente> nwavFaltantes = nwavRepositorio.findAll().stream()
                .filter(nwavArchivo -> !Files.isRegularFile(Path.of(nwavArchivo.getNwavRuta())))
                .toList();
        nwavRepositorio.deleteAll(nwavFaltantes);
        return nwavFaltantes.size();
    }

    @Transactional
    public void nwavRecortar() {
        List<NwavArchivoReciente> nwavTodos = nwavRepositorio.findAllByOrderByNwavUltimaAperturaDesc();
        int nwavMaximo = nwavConfiguracion.nwavMaximoRecientes();
        if (nwavTodos.size() > nwavMaximo) {
            nwavRepositorio.deleteAll(nwavTodos.subList(nwavMaximo, nwavTodos.size()));
        }
    }
}
