package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NegocioException;
import com.nwav.editorpdf.model.Categoria;
import com.nwav.editorpdf.model.Producto;
import com.nwav.editorpdf.repository.CategoriaRepository;
import com.nwav.editorpdf.repository.CompraDetalleRepository;
import com.nwav.editorpdf.repository.ProductoRepository;
import com.nwav.editorpdf.repository.VentaDetalleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductoService {

    private static final int TAMANIO_PAGINA = 15;
    private static final int LIMITE_SUGERENCIAS = 10;

    private final ProductoRepository repository;
    private final CategoriaRepository categoriaRepository;
    private final VentaDetalleRepository ventaDetalleRepository;
    private final CompraDetalleRepository compraDetalleRepository;

    public Page<Producto> listar(String filtro, int pagina) {
        return listar(filtro, pagina, TAMANIO_PAGINA);
    }

    public Page<Producto> listar(String filtro, int pagina, int tamanio) {
        Pageable pageable = PageRequest.of(pagina, tamanio, Sort.by("id").ascending());
        if (filtro == null || filtro.isBlank()) {
            return repository.findAllConCategoria(pageable);
        }
        return repository.buscar(filtro.trim(), pageable);
    }

    /** Sugerencias para el carrito: cada palabra debe aparecer en el codigo o en el nombre. */
    public List<Producto> autocompletar(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        Specification<Producto> spec = Specification.where(null);
        for (String palabra : texto.trim().toLowerCase().split("\\s+")) {
            String patron = "%" + palabra + "%";
            spec = spec.and((raiz, q, cb) -> cb.or(
                    cb.like(cb.lower(raiz.get("codigoProducto")), patron),
                    cb.like(cb.lower(raiz.get("nombre")), patron)));
        }
        return repository.findAll(spec,
                PageRequest.of(0, LIMITE_SUGERENCIAS, Sort.by("nombre"))).getContent();
    }

    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll(Sort.by("nombre").ascending());
    }

    @Transactional
    public Producto guardar(@Valid Producto producto) {
        repository.findByCodigoProducto(producto.getCodigoProducto()).ifPresent(existente -> {
            boolean esOtroProducto = producto.getId() == null
                    || !existente.getId().equals(producto.getId());
            if (esOtroProducto) {
                throw new NegocioException(
                        "Ya existe un producto registrado con el codigo " + producto.getCodigoProducto());
            }
        });

        // El stock y el costo solo cambian con compras y ventas: el formulario no los toca.
        if (producto.getId() != null) {
            repository.findById(producto.getId()).ifPresent(actual -> {
                producto.setStock(actual.getStock());
                producto.setCostoPromedio(actual.getCostoPromedio());
            });
        } else {
            producto.setStock(0);
            producto.setCostoPromedio(null);
        }
        if (producto.getStockMinimo() == null) {
            producto.setStockMinimo(5);
        }
        Producto guardado = repository.save(producto);
        log.info("Producto guardado: id={} codigo={}", guardado.getId(), guardado.getCodigoProducto());
        return guardado;
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new NegocioException("El producto ya no existe en la base de datos.");
        }
        if (ventaDetalleRepository.existsByProductoId(id)) {
            throw new NegocioException("No se puede eliminar: el producto figura en ventas registradas.");
        }
        if (compraDetalleRepository.existsByProductoId(id)) {
            throw new NegocioException("No se puede eliminar: el producto figura en compras registradas.");
        }
        repository.deleteById(id);
    }

    public long contar() {
        return repository.count();
    }
}

