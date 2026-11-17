package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NegocioException;
import com.nwav.editorpdf.model.Persona;
import com.nwav.editorpdf.repository.PersonaRepository;
import com.nwav.editorpdf.repository.VentaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@Validated
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PersonaService {

    private static final int TAMANIO_PAGINA = 15;
    private static final int LIMITE_SUGERENCIAS = 8;

    private final PersonaRepository repository;
    private final VentaRepository ventaRepository;

    public Page<Persona> listar(String filtro, int pagina) {
        return listar(filtro, pagina, TAMANIO_PAGINA);
    }

    public Page<Persona> listar(String filtro, int pagina, int tamanio) {
        Pageable pageable = PageRequest.of(pagina, tamanio, Sort.by("id").ascending());
        if (filtro == null || filtro.isBlank()) {
            return repository.findAll(pageable);
        }
        return repository.buscar(filtro.trim(), pageable);
    }

    /**
     * Sugerencias para el autocompletado de clientes. Si el texto es numerico busca por
     * DNI (prefijo); si no, cada palabra debe aparecer en el nombre o en los apellidos,
     * en cualquier orden ("quispe maria" encuentra a "Maria Quispe Rojas").
     */
    public List<Persona> autocompletar(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        String t = texto.trim();
        Specification<Persona> filtro = t.matches("\\d+")
                ? (raiz, q, cb) -> cb.like(raiz.get("dni"), t + "%")
                : porPalabras(t);
        return repository.findAll(filtro,
                PageRequest.of(0, LIMITE_SUGERENCIAS, Sort.by("apellido", "nombre"))).getContent();
    }

    public Optional<Persona> buscarPorDni(String dni) {
        return dni == null ? Optional.empty() : repository.findByDni(dni.trim());
    }

    private static Specification<Persona> porPalabras(String texto) {
        Specification<Persona> spec = Specification.where(null);
        for (String palabra : texto.toLowerCase().split("\\s+")) {
            String patron = "%" + palabra + "%";
            spec = spec.and((raiz, q, cb) -> cb.or(
                    cb.like(cb.lower(raiz.get("nombre")), patron),
                    cb.like(cb.lower(raiz.get("apellido")), patron)));
        }
        return spec;
    }

    @Transactional
    public Persona guardar(@Valid Persona persona) {
        repository.findByDni(persona.getDni()).ifPresent(existente -> {
            boolean esOtraPersona = persona.getId() == null
                    || !existente.getId().equals(persona.getId());
            if (esOtraPersona) {
                throw new NegocioException(
                        "Ya existe una persona registrada con el DNI " + persona.getDni());
            }
        });

        Persona guardada = repository.save(persona);
        log.info("Persona guardada: id={} dni={}", guardada.getId(), guardada.getDni());
        return guardada;
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new NegocioException("La persona ya no existe en la base de datos.");
        }
        if (ventaRepository.existsByClienteId(id)) {
            throw new NegocioException("No se puede eliminar: la persona tiene ventas registradas como cliente.");
        }
        repository.deleteById(id);
    }

    public long contar() {
        return repository.count();
    }
}

