package com.nwav.editorpdf.seeder;

import com.nwav.editorpdf.model.Producto;
import com.nwav.editorpdf.model.TipoMovimiento;
import com.nwav.editorpdf.model.Venta;
import com.nwav.editorpdf.model.VentaDetalle;
import com.nwav.editorpdf.repository.CompraRepository;
import com.nwav.editorpdf.repository.MovimientoInventarioRepository;
import com.nwav.editorpdf.repository.ProductoRepository;
import com.nwav.editorpdf.repository.VentaRepository;
import com.nwav.editorpdf.service.InventarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Inventario inicial: compras de apertura (en lotes de 20 productos) fechadas antes de
 * la primera venta. Si la base ya tenia ventas registradas antes de existir el control
 * de stock, la compra de apertura cubre esas unidades y luego se registran sus salidas
 * en el kardex, de modo que el saldo final cuadra.
 * <p>
 * Idempotente: solo actua si todavia no hay ninguna compra.
 */
@Component
@RequiredArgsConstructor
public class InventarioSeeder {

    private static final long SEMILLA = 7L;
    private static final int PRODUCTOS_POR_COMPRA = 20;
    private static final int DIAS_ANTES_DE_LA_PRIMERA_VENTA = 5;
    private static final int DIAS_HISTORIA = 125;

    private final ComprasSimuladas compras;
    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;
    private final VentaRepository ventaRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final InventarioService inventarioService;

    /** @return cantidad de compras de apertura registradas */
    @Transactional
    public int sembrar() {
        if (compraRepository.count() > 0) {
            return 0;
        }
        List<Producto> productos = productoRepository.findAll(Sort.by("id"));
        if (productos.isEmpty()) {
            return 0;
        }

        // Ventas que existian antes del control de stock (emitidas y anuladas).
        List<Venta> previas = ventaRepository.findAll().stream()
                .filter(v -> !movimientoRepository.existsByVentaId(v.getId()))
                .sorted(Comparator.comparing(Venta::getFecha).thenComparing(Venta::getId))
                .toList();
        Map<Long, Integer> vendidas = new HashMap<>();
        for (Venta v : previas) {
            if (!v.isAnulada()) {
                for (VentaDetalle d : v.getDetalles()) {
                    vendidas.merge(d.getProducto().getId(), d.getCantidad(), Integer::sum);
                }
            }
        }

        LocalDateTime apertura = previas.isEmpty()
                ? LocalDateTime.now().minusDays(DIAS_HISTORIA).withHour(9).withMinute(0)
                : previas.get(0).getFecha().minusDays(DIAS_ANTES_DE_LA_PRIMERA_VENTA).withHour(9).withMinute(0);

        Random azar = new Random(SEMILLA);
        int registradas = 0;
        for (int i = 0; i < productos.size(); i += PRODUCTOS_POR_COMPRA) {
            Map<Producto, Integer> lote = new LinkedHashMap<>();
            for (Producto p : productos.subList(i, Math.min(productos.size(), i + PRODUCTOS_POR_COMPRA))) {
                lote.put(p, ComprasSimuladas.cantidadReposicion(p, azar) + vendidas.getOrDefault(p.getId(), 0));
            }
            compras.comprar(lote, apertura.plusMinutes(15L * registradas), azar);
            registradas++;
        }

        // Salidas de las ventas previas, en orden cronologico (las anuladas no descuentan).
        for (Venta v : previas) {
            if (v.isAnulada()) {
                continue;
            }
            for (VentaDetalle d : new ArrayList<>(v.getDetalles())) {
                inventarioService.salida(d.getProducto(), d.getCantidad(), TipoMovimiento.VENTA,
                        v.getComprobante(), v.getFecha(), v.getId(), null);
            }
        }
        return registradas;
    }
}
