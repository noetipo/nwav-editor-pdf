package com.nwav.editorpdf.seeder;

import com.nwav.editorpdf.model.Categoria;
import com.nwav.editorpdf.model.Producto;
import com.nwav.editorpdf.repository.CategoriaRepository;
import com.nwav.editorpdf.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registra 100 productos distintos (codigos PRD-001 ... PRD-100): 50 productos base,
 * cada uno en dos presentaciones. Debe ejecutarse despues de {@link CategoriaSeeder}.
 * Es idempotente: omite los codigos que ya existen en la base de datos.
 */
@Component
@RequiredArgsConstructor
public class ProductoSeeder {

    /** Producto base, categoria, precio de la presentacion A, presentaciones A y B, factor de precio de B. */
    private record Plantilla(String nombre, String categoria, double precio,
                             String presentacionA, String presentacionB, double factorB) {}

    private static final List<Plantilla> PLANTILLAS = List.of(
            new Plantilla("Arroz extra", "Arroces", 4.20, "1 kg", "5 kg", 4.6),
            new Plantilla("Aceite vegetal", "Aceites y vinagres", 9.90, "1 L", "5 L", 4.5),
            new Plantilla("Agua mineral sin gas", "Aguas", 1.80, "625 ml", "2.5 L", 2.2),
            new Plantilla("Azucar rubia", "Azucar y endulzantes", 3.80, "1 kg", "5 kg", 4.7),
            new Plantilla("Gaseosa cola", "Bebidas gaseosas", 3.50, "500 ml", "3 L", 3.1),
            new Plantilla("Bebida energizante", "Bebidas energeticas", 4.50, "250 ml", "473 ml", 1.6),
            new Plantilla("Nectar de durazno", "Jugos y nectares", 2.90, "300 ml", "1 L", 2.4),
            new Plantilla("Cerveza rubia", "Cervezas", 5.50, "lata 355 ml", "six pack", 5.4),
            new Plantilla("Vino tinto reserva", "Vinos", 32.00, "750 ml", "caja x6", 5.6),
            new Plantilla("Pisco quebranta", "Licores", 38.00, "500 ml", "750 ml", 1.4),
            new Plantilla("Cafe molido", "Cafe", 14.50, "250 g", "500 g", 1.9),
            new Plantilla("Te verde", "Te e infusiones", 4.90, "caja x25", "caja x100", 3.6),
            new Plantilla("Chocolate bitter 70%", "Chocolates", 7.90, "100 g", "250 g", 2.3),
            new Plantilla("Galletas de avena", "Galletas", 3.20, "paquete x6", "paquete x12", 1.9),
            new Plantilla("Papas fritas clasicas", "Snacks salados", 2.50, "45 g", "180 g", 3.2),
            new Plantilla("Gomitas frutales", "Golosinas", 1.50, "80 g", "500 g", 5.2),
            new Plantilla("Avena en hojuelas", "Cereales", 5.40, "500 g", "1 kg", 1.8),
            new Plantilla("Lentejas", "Menestras", 6.20, "500 g", "1 kg", 1.9),
            new Plantilla("Spaghetti", "Pastas", 3.10, "500 g", "1 kg", 1.9),
            new Plantilla("Atun en aceite", "Conservas de pescado", 6.50, "170 g", "pack x3", 2.8),
            new Plantilla("Mayonesa", "Salsas y aderezos", 4.80, "100 g", "475 g", 3.4),
            new Plantilla("Leche evaporada", "Leches", 3.90, "400 g", "pack x6", 5.6),
            new Plantilla("Yogur griego natural", "Yogures", 5.90, "500 g", "1 kg", 1.8),
            new Plantilla("Queso fresco", "Quesos", 12.50, "500 g", "1 kg", 1.9),
            new Plantilla("Mantequilla con sal", "Mantequillas y margarinas", 8.40, "200 g", "400 g", 1.9),
            new Plantilla("Huevos pardos", "Huevos", 7.20, "x10", "x30", 2.8),
            new Plantilla("Jamon ingles", "Embutidos", 9.90, "250 g", "500 g", 1.9),
            new Plantilla("Pechuga de pollo", "Aves", 16.90, "1 kg", "2 kg", 1.95),
            new Plantilla("Helado de vainilla", "Helados", 11.90, "1 L", "2 L", 1.8),
            new Plantilla("Pan de molde integral", "Panaderia", 7.50, "480 g", "700 g", 1.4),
            new Plantilla("Panales etapa 3", "Panales", 42.90, "x40", "x80", 1.9),
            new Plantilla("Shampoo anticaspa", "Cuidado del cabello", 18.90, "400 ml", "750 ml", 1.6),
            new Plantilla("Crema dental triple accion", "Cuidado bucal", 5.50, "75 ml", "pack x3", 2.7),
            new Plantilla("Papel higienico doble hoja", "Papel higienico", 12.90, "x12", "x24", 1.9),
            new Plantilla("Detergente en polvo", "Detergentes", 21.90, "2 kg", "4 kg", 1.9),
            new Plantilla("Lavavajillas liquido", "Lavavajillas", 6.90, "500 ml", "1.2 L", 2.1),
            new Plantilla("Desinfectante lavanda", "Desinfectantes", 8.50, "900 ml", "1.9 L", 1.9),
            new Plantilla("Sarten antiadherente", "Utensilios de cocina", 59.90, "24 cm", "28 cm", 1.3),
            new Plantilla("Juego de sabanas", "Textil hogar", 89.90, "1.5 plazas", "2 plazas", 1.3),
            new Plantilla("Foco LED", "Iluminacion", 9.90, "9 W", "pack x4", 3.5),
            new Plantilla("Taladro percutor", "Herramientas", 189.00, "600 W", "850 W", 1.5),
            new Plantilla("Televisor Smart 4K", "Televisores", 1499.00, "50 pulgadas", "65 pulgadas", 1.8),
            new Plantilla("Audifonos inalambricos", "Audio", 129.00, "estandar", "con cancelacion de ruido", 3.1),
            new Plantilla("Smartphone", "Celulares", 899.00, "128 GB", "256 GB", 1.35),
            new Plantilla("Laptop ultradelgada", "Computadoras", 2799.00, "8 GB RAM", "16 GB RAM", 1.3),
            new Plantilla("Licuadora", "Electrodomesticos pequenos", 149.00, "1.5 L", "2 L profesional", 1.7),
            new Plantilla("Refrigeradora", "Electrodomesticos grandes", 1899.00, "250 L", "400 L", 1.6),
            new Plantilla("Cuaderno cuadriculado", "Utiles escolares", 4.50, "A4 x100 hojas", "pack x5", 4.6),
            new Plantilla("Pelota de futbol", "Deportes", 69.90, "N° 4", "N° 5 profesional", 2.1),
            new Plantilla("Alimento para perro adulto", "Alimento para perros", 39.90, "3 kg", "15 kg", 4.4));

    private final ProductoRepository repository;
    private final CategoriaRepository categoriaRepository;

    @Transactional
    public int sembrar() {
        Set<String> existentes = repository.findAll().stream()
                .map(Producto::getCodigoProducto)
                .collect(Collectors.toSet());
        Map<String, Categoria> categorias = categoriaRepository.findAll().stream()
                .collect(Collectors.toMap(Categoria::getNombre, Function.identity(), (a, b) -> a));

        List<Producto> nuevos = new ArrayList<>();
        int numero = 0;
        for (Plantilla p : PLANTILLAS) {
            for (boolean esB : new boolean[]{false, true}) {
                numero++;
                String codigo = String.format("PRD-%03d", numero);
                if (existentes.contains(codigo)) {
                    continue;
                }
                String presentacion = esB ? p.presentacionB() : p.presentacionA();
                BigDecimal precio = BigDecimal.valueOf(esB ? p.precio() * p.factorB() : p.precio())
                        .setScale(2, RoundingMode.HALF_UP);
                // Algunos productos quedan sin categoria para probar ese caso en tablas y graficos.
                Categoria categoria = numero % 15 == 0 ? null : categorias.get(p.categoria());

                nuevos.add(Producto.builder()
                        .codigoProducto(codigo)
                        .nombre(p.nombre() + " " + presentacion)
                        .descripcion(numero % 6 == 0 ? null : p.nombre() + ", presentacion " + presentacion)
                        .precio(precio)
                        .categoria(categoria)
                        .build());
            }
        }
        repository.saveAll(nuevos);
        return nuevos.size();
    }
}
