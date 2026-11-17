package com.nwav.editorpdf.service.herramienta;

import com.nwav.editorpdf.model.NwavOpcionHerramienta;
import com.nwav.editorpdf.repository.NwavOpcionHerramientaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NwavOpcionesHerramientaService {
    private static final int NWAV_LARGO_VALOR = 2048;

    private final NwavOpcionHerramientaRepository nwavRepositorio;

    @Transactional(readOnly = true)
    public Map<String, String> nwavCargar(String nwavHerramienta) {
        Map<String, String> nwavValores = new LinkedHashMap<>();
        nwavRepositorio.findByNwavHerramienta(nwavHerramienta)
                .forEach(nwavFila -> nwavValores.put(nwavFila.getNwavClave(), nwavFila.getNwavValor()));
        return nwavValores;
    }

    @Transactional
    public void nwavGuardar(String nwavHerramienta, Map<String, String> nwavValores) {
        try {
            Map<String, NwavOpcionHerramienta> nwavExistentes = nwavRepositorio.findByNwavHerramienta(nwavHerramienta)
                    .stream().collect(Collectors.toMap(NwavOpcionHerramienta::getNwavClave, Function.identity()));
            LocalDateTime nwavAhora = LocalDateTime.now();
            nwavValores.forEach((nwavClave, nwavValor) -> {
                NwavOpcionHerramienta nwavFila = nwavExistentes.computeIfAbsent(nwavClave, nwavNueva -> {
                    NwavOpcionHerramienta nwavCreada = new NwavOpcionHerramienta();
                    nwavCreada.setNwavHerramienta(nwavHerramienta);
                    nwavCreada.setNwavClave(nwavNueva);
                    return nwavCreada;
                });
                nwavFila.setNwavValor(nwavValor != null && nwavValor.length() > NWAV_LARGO_VALOR
                        ? nwavValor.substring(0, NWAV_LARGO_VALOR) : nwavValor);
                nwavFila.setNwavActualizado(nwavAhora);
                nwavRepositorio.save(nwavFila);
            });
        } catch (RuntimeException nwavError) {
            nwavLog.warn("No se pudieron guardar las opciones de {}", nwavHerramienta, nwavError);
        }
    }
}
