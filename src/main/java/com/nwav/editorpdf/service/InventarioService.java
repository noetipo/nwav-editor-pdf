package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NegocioException;
import com.nwav.editorpdf.model.MovimientoInventario;
import com.nwav.editorpdf.model.Producto;
import com.nwav.editorpdf.model.TipoMovimiento;
import com.nwav.editorpdf.repository.MovimientoInventarioRepository;
import com.nwav.editorpdf.repository.ProductoRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Unico punto que modifica el stock. Cada entrada o salida actualiza el producto y
 * deja un {@link MovimientoInventario} con el saldo resultante (kardex).
 * <ul>
 *   <li>Compra: suma stock y recalcula el costo promedio ponderado.</li>
 *   <li>Venta: resta stock; no permite quedar en negativo.</li>
 *   <li>Anulaciones: revierten el movimiento original.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventarioService {

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    @Getter
    @RequiredArgsConstructor
    public enum FiltroStock {
        TODOS("Todos los productos"),
        DISPONIBLE("Stock suficiente"),
        BAJO("Stock bajo"),
        SIN_STOCK("Sin stock");

        private final String etiqueta;

        @Override
        public String toString() {
            return etiqueta;
        }
    }

    public record ResumenInventario(long productos, long unidades, BigDecimal valorCosto,
                                    BigDecimal valorVenta, long stockBajo, long sinStock) {}

    // ------------------------------------------------------------ consultas

    public Page<Producto> listar(String texto, FiltroStock filtro, int pagina, int tamanio) {
        return productoRepository.inventario(texto == null ? "" : texto.trim(),
                (filtro == null ? FiltroStock.TODOS : filtro).name(),
                PageRequest.of(pagina, tamanio, Sort.by("codigoProducto")));
    }

    public List<Producto> listarTodo() {
        return productoRepository.listarConCategoria();
    }

    public ResumenInventario resumen() {
        List<Producto> productos = productoRepository.findAll();
        long unidades = 0;
        long bajo = 0;
        long sin = 0;
        BigDecimal costo = BigDecimal.ZERO;
        BigDecimal venta = BigDecimal.ZERO;
        for (Producto p : productos) {
            int stock = Math.max(0, p.getStock());
            unidades += stock;
            if (p.isSinStock()) {
                sin++;
            } else if (p.isStockBajo()) {
                bajo++;
            }
            costo = costo.add(valorizado(p));
            venta = venta.add(p.getPrecio().multiply(BigDecimal.valueOf(stock)));
        }
        return new ResumenInventario(productos.size(), unidades,
                costo.setScale(2, RoundingMode.HALF_UP), venta.setScale(2, RoundingMode.HALF_UP), bajo, sin);
    }

    public List<MovimientoInventario> kardex(Long productoId) {
        return movimientoRepository.kardex(productoId);
    }

    /** Stock x costo promedio (cero si el producto nunca se compro). */
    public static BigDecimal valorizado(Producto p) {
        if (p.getCostoPromedio() == null || p.getStock() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return p.getCostoPromedio().multiply(BigDecimal.valueOf(p.getStock())).setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------- comandos

    /**
     * Verifica que haya stock para todas las cantidades pedidas (agrupadas por producto)
     * y, si falta en alguno, informa todos los faltantes juntos.
     */
    public void validarDisponible(Map<Producto, Integer> pedido) {
        String faltantes = pedido.entrySet().stream()
                .filter(e -> e.getValue() > e.getKey().getStock())
                .map(e -> e.getKey().getNombre() + " (disponible " + Math.max(0, e.getKey().getStock())
                        + ", solicitado " + e.getValue() + ")")
                .collect(Collectors.joining("; "));
        if (!faltantes.isEmpty()) {
            throw new NegocioException("Stock insuficiente: " + faltantes + ".");
        }
    }

    @Transactional
    public MovimientoInventario entrada(Producto producto, int cantidad, BigDecimal costoUnitario,
                                        TipoMovimiento tipo, String documento, LocalDateTime fecha,
                                        Long ventaId, Long compraId) {
        if (!tipo.isEntrada()) {
            throw new IllegalArgumentException(tipo + " no es un movimiento de entrada");
        }
        int anterior = Math.max(0, producto.getStock());
        if (tipo == TipoMovimiento.COMPRA && costoUnitario != null) {
            producto.setCostoPromedio(costoPromedio(producto.getCostoPromedio(), anterior, costoUnitario, cantidad));
        }
        producto.setStock(producto.getStock() + cantidad);
        return registrar(producto, tipo, cantidad, costoUnitario, documento, fecha, ventaId, compraId);
    }

    @Transactional
    public MovimientoInventario salida(Producto producto, int cantidad, TipoMovimiento tipo, String documento,
                                       LocalDateTime fecha, Long ventaId, Long compraId) {
        if (tipo.isEntrada()) {
            throw new IllegalArgumentException(tipo + " no es un movimiento de salida");
        }
        if (cantidad > producto.getStock()) {
            throw new NegocioException("Stock insuficiente de " + producto.getNombre() + ": disponible "
                    + Math.max(0, producto.getStock()) + ", se requieren " + cantidad + ".");
        }
        producto.setStock(producto.getStock() - cantidad);
        return registrar(producto, tipo, cantidad, null, documento, fecha, ventaId, compraId);
    }

    private MovimientoInventario registrar(Producto producto, TipoMovimiento tipo, int cantidad, BigDecimal costo,
                                           String documento, LocalDateTime fecha, Long ventaId, Long compraId) {
        return movimientoRepository.save(MovimientoInventario.builder()
                .producto(producto)
                .tipo(tipo)
                .cantidad(cantidad)
                .stockResultante(producto.getStock())
                .costoUnitario(costo)
                .documento(documento)
                .fecha(fecha)
                .ventaId(ventaId)
                .compraId(compraId)
                .build());
    }

    /** Promedio ponderado: (stock x costo actual + cantidad x costo nuevo) / (stock + cantidad). */
    static BigDecimal costoPromedio(BigDecimal actual, int stock, BigDecimal nuevo, int cantidad) {
        if (actual == null || stock <= 0) {
            return nuevo.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal total = actual.multiply(BigDecimal.valueOf(stock))
                .add(nuevo.multiply(BigDecimal.valueOf(cantidad)));
        return total.divide(BigDecimal.valueOf((long) stock + cantidad), 4, RoundingMode.HALF_UP);
    }
}
