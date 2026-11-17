package com.nwav.editorpdf.controller;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.util.Animations;
import com.nwav.editorpdf.exception.NegocioException;
import com.nwav.editorpdf.model.Persona;
import com.nwav.editorpdf.model.Producto;
import com.nwav.editorpdf.model.TipoComprobante;
import com.nwav.editorpdf.model.Venta;
import com.nwav.editorpdf.service.CalculadoraIgv;
import com.nwav.editorpdf.service.CalculadoraIgv.Totales;
import com.nwav.editorpdf.service.ComprobantePdfService;
import com.nwav.editorpdf.service.NumeroALetras;
import com.nwav.editorpdf.service.PersonaService;
import com.nwav.editorpdf.service.ProductoService;
import com.nwav.editorpdf.service.VentaService;
import com.nwav.editorpdf.service.VentaService.LineaCarrito;
import com.nwav.editorpdf.service.VentaService.NuevaVenta;
import com.nwav.editorpdf.ui.Autocompletar;
import com.nwav.editorpdf.ui.CeldasCarrito;
import com.nwav.editorpdf.ui.Exportador;
import com.nwav.editorpdf.ui.Formularios;
import com.nwav.editorpdf.ui.Notificador;
import com.nwav.editorpdf.ui.Refrescable;
import com.nwav.editorpdf.ui.Tablas;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Punto de venta: cliente, carrito con autocompletado, calculo de IGV y emision. */
@Component
@RequiredArgsConstructor
public class PuntoVentaController implements Refrescable {

    private static final DateTimeFormatter FECHA_LARGA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Tablas.LOCALE);

    private final VentaService ventaService;
    private final PersonaService personaService;
    private final ProductoService productoService;
    private final ComprobantePdfService pdfService;
    private final Exportador exportador;
    private final Notificador notificador;

    @FXML private VBox raiz;

    @FXML private TextField txtDni;
    @FXML private TextField txtNombreCliente;
    @FXML private HBox panelSinCliente;
    @FXML private HBox panelCliente;
    @FXML private Label lblIniciales;
    @FXML private Label lblClienteNombre;
    @FXML private Label lblClienteDni;
    @FXML private Label lblClienteEmail;
    @FXML private Label lblClienteTelefono;

    @FXML private TextField txtProducto;
    @FXML private Spinner<Integer> spnCantidad;
    @FXML private Label lblContadorItems;
    @FXML private TableView<ItemCarrito> tablaCarrito;
    @FXML private TableColumn<ItemCarrito, ItemCarrito> colItem;
    @FXML private TableColumn<ItemCarrito, String> colCodigo;
    @FXML private TableColumn<ItemCarrito, String> colProducto;
    @FXML private TableColumn<ItemCarrito, ItemCarrito> colCantidad;
    @FXML private TableColumn<ItemCarrito, ItemCarrito> colPrecio;
    @FXML private TableColumn<ItemCarrito, BigDecimal> colImporte;
    @FXML private TableColumn<ItemCarrito, ItemCarrito> colQuitar;

    @FXML private Label lblFecha;
    @FXML private ComboBox<TipoComprobante> cmbTipo;
    @FXML private Label lblNumero;
    @FXML private ToggleSwitch swIgv;
    @FXML private ToggleSwitch swIncluyeIgv;
    @FXML private Label lblModoIgv;
    @FXML private TextField txtObservacion;
    @FXML private Label lblArticulos;
    @FXML private Label lblSubtotal;
    @FXML private Label lblEtiquetaBase;
    @FXML private Label lblBase;
    @FXML private Label lblEtiquetaIgv;
    @FXML private Label lblIgv;
    @FXML private Label lblTotal;
    @FXML private Label lblEnLetras;
    @FXML private Button btnRegistrar;

    /** La lista observa cantidad y precio de cada linea para recalcular totales al editarlas. */
    private final ObservableList<ItemCarrito> carrito =
            FXCollections.observableArrayList(i -> new Observable[]{i.cantidad, i.precio});
    private final ObjectProperty<Persona> cliente = new SimpleObjectProperty<>();

    private Autocompletar<Persona> autoDni;
    private Autocompletar<Persona> autoNombre;
    private Autocompletar<Producto> autoProducto;

    @FXML
    public void initialize() {
        configurarCliente();
        configurarCarrito();
        configurarResumen();

        // Atajo F9 para registrar (solo mientras el foco esta dentro de esta vista).
        raiz.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F9 && !btnRegistrar.isDisabled()) {
                e.consume();
                onRegistrar();
            }
        });
    }

    @Override
    public void refrescar() {
        lblFecha.setText(capitalizar(LocalDateTime.now().format(FECHA_LARGA)));
        actualizarNumero();
        Platform.runLater(() -> (cliente.get() == null ? txtDni : txtProducto).requestFocus());
    }

    // ------------------------------------------------------------- cliente

    private void configurarCliente() {
        Formularios.soloDigitos(txtDni, 8);
        Formularios.limpiarAlEditar(txtDni, txtNombreCliente);

        autoDni = new Autocompletar<>(txtDni, personaService::autocompletar, this::seleccionarCliente)
                .presentacion(Persona::getDni, Persona::getNombreCompleto, t -> null)
                .icono(this::avatar)
                .minimoCaracteres(2);
        autoNombre = new Autocompletar<>(txtNombreCliente, personaService::autocompletar, this::seleccionarCliente)
                .presentacion(Persona::getNombreCompleto, p -> "DNI " + p.getDni(), t -> null)
                .icono(this::avatar)
                .minimoCaracteres(2);

        // Enter sin sugerencia elegida: busqueda exacta por DNI.
        txtDni.setOnAction(e -> onBuscarDni());
    }

    @FXML
    private void onBuscarDni() {
        String dni = txtDni.getText().trim();
        autoDni.ocultar();
        if (dni.length() != 8) {
            Formularios.marcarError(txtDni, "Ingresa los 8 digitos del DNI");
            return;
        }
        personaService.buscarPorDni(dni).ifPresentOrElse(this::seleccionarCliente,
                () -> Formularios.marcarError(txtDni, "No hay ningun cliente con DNI " + dni));
    }

    private void seleccionarCliente(Persona p) {
        cliente.set(p);
        Formularios.limpiarErrores(txtDni, txtNombreCliente);
        autoDni.setTexto(p.getDni());
        autoNombre.setTexto(p.getNombreCompleto());

        lblIniciales.setText(Tablas.iniciales(p.getNombreCompleto()));
        lblClienteNombre.setText(p.getNombreCompleto());
        lblClienteDni.setText("DNI " + p.getDni());
        mostrarDato(lblClienteEmail, p.getEmail());
        mostrarDato(lblClienteTelefono, p.getTelefono());

        alternar(panelCliente, panelSinCliente);
        Animations.fadeIn(panelCliente, Duration.millis(250)).play();
        Platform.runLater(txtProducto::requestFocus);
    }

    @FXML
    private void onQuitarCliente() {
        cliente.set(null);
        autoDni.setTexto("");
        autoNombre.setTexto("");
        alternar(panelSinCliente, panelCliente);
        txtDni.requestFocus();
    }

    // ------------------------------------------------------------- carrito

    private void configurarCarrito() {
        spnCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, 1));
        Formularios.soloDigitos(spnCantidad.getEditor(), 4);

        autoProducto = new Autocompletar<>(txtProducto, productoService::autocompletar, this::agregarProducto)
                .presentacion(Producto::getNombre,
                        p -> p.getCodigoProducto() + "  ·  " + (p.isSinStock() ? "Sin stock" : "Stock " + p.getStock()),
                        p -> Tablas.MONEDA.format(p.getPrecio()))
                .icono(p -> new FontIcon(Feather.PACKAGE))
                .minimoCaracteres(1);

        colItem.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colItem.setCellFactory(CeldasCarrito.numeroFila());
        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().producto.getCodigoProducto()));
        colCodigo.setCellFactory(Tablas.celdaChip("—"));
        colProducto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().producto.getNombre()));
        colCantidad.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colCantidad.setCellFactory(CeldasCarrito.cantidad(i -> i.cantidad, i -> i.producto.getStock(),
                i -> "Solo hay " + i.producto.getStock() + " unidades de " + i.producto.getNombre() + " en stock.",
                notificador::advertencia));
        colPrecio.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colPrecio.setCellFactory(CeldasCarrito.importe(i -> i.precio, notificador::advertencia));
        colImporte.setCellValueFactory(c -> c.getValue().importe);
        colImporte.setCellFactory(Tablas.celdaMoneda());
        colQuitar.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colQuitar.setCellFactory(CeldasCarrito.quitar(carrito::remove));

        Tablas.configurar(tablaCarrito, Feather.SHOPPING_CART, "Carrito vacio",
                "Busca un producto arriba para agregarlo al carrito.", i -> { });
        tablaCarrito.setItems(carrito);
        tablaCarrito.getStyleClass().add("tabla-carrito");

        carrito.addListener((ListChangeListener<ItemCarrito>) c -> recalcular());
    }

    private void agregarProducto(Producto p) {
        if (p.isSinStock()) {
            notificador.advertencia(p.getNombre() + " no tiene stock disponible. Registra una compra para reponerlo.");
            autoProducto.setTexto("");
            return;
        }
        int cantidad = spnCantidad.getValue() == null ? 1 : spnCantidad.getValue();
        ItemCarrito existente = carrito.stream()
                .filter(i -> i.producto.getId().equals(p.getId()))
                .findFirst().orElse(null);
        int enCarrito = existente == null ? 0 : existente.cantidad.get();
        if (enCarrito + cantidad > p.getStock()) {
            cantidad = p.getStock() - enCarrito;
            notificador.advertencia("Solo hay " + p.getStock() + " unidades de " + p.getNombre() + " en stock.");
            if (cantidad <= 0) {
                autoProducto.setTexto("");
                return;
            }
        }
        if (existente != null) {
            existente.cantidad.set(enCarrito + cantidad);
            tablaCarrito.getSelectionModel().select(existente);
            tablaCarrito.scrollTo(existente);
            notificador.info(p.getNombre() + ": cantidad actualizada a " + existente.cantidad.get());
        } else {
            ItemCarrito nuevo = new ItemCarrito(p, cantidad, p.getPrecio().setScale(2, RoundingMode.HALF_UP));
            carrito.add(nuevo);
            tablaCarrito.getSelectionModel().select(nuevo);
            tablaCarrito.scrollTo(nuevo);
        }
        autoProducto.setTexto("");
        spnCantidad.getValueFactory().setValue(1);
        Platform.runLater(txtProducto::requestFocus);
    }

    // ------------------------------------------------------------- resumen

    private void configurarResumen() {
        cmbTipo.getItems().setAll(TipoComprobante.values());
        cmbTipo.setValue(TipoComprobante.BOLETA);
        cmbTipo.valueProperty().addListener((obs, a, b) -> actualizarNumero());

        swIncluyeIgv.disableProperty().bind(swIgv.selectedProperty().not());
        swIgv.selectedProperty().addListener((obs, a, b) -> recalcular());
        swIncluyeIgv.selectedProperty().addListener((obs, a, b) -> recalcular());

        btnRegistrar.disableProperty().bind(Bindings.isEmpty(carrito).or(cliente.isNull()));
        recalcular();
    }

    private void recalcular() {
        boolean aplica = swIgv.isSelected();
        boolean incluye = swIncluyeIgv.isSelected();
        Totales t = CalculadoraIgv.calcular(carrito.stream().map(i -> i.importe.get()).toList(), aplica, incluye);
        BigDecimal suma = carrito.stream().map(i -> i.importe.get()).reduce(BigDecimal.ZERO, BigDecimal::add);
        int articulos = carrito.stream().mapToInt(i -> i.cantidad.get()).sum();

        lblArticulos.setText(String.valueOf(articulos));
        lblContadorItems.setText(carrito.size() == 1 ? "1 item" : carrito.size() + " items");
        lblSubtotal.setText(Tablas.MONEDA.format(suma));
        lblEtiquetaBase.setText(aplica ? "Base imponible (Op. gravada)" : "Op. exonerada / inafecta");
        lblBase.setText(Tablas.MONEDA.format(t.baseImponible()));
        lblEtiquetaIgv.setText(aplica ? "IGV (18%)" : "IGV (no aplica)");
        lblIgv.setText(Tablas.MONEDA.format(t.igv()));
        lblTotal.setText(Tablas.MONEDA.format(t.total()));
        lblEnLetras.setText("SON: " + NumeroALetras.importe(t.total()));
        lblModoIgv.setText(!aplica ? "Sin IGV: el total es la suma de importes"
                : incluye ? "El IGV se extrae del precio (total ÷ 1.18)"
                : "El IGV (18%) se suma al precio");
    }

    private void actualizarNumero() {
        lblNumero.setText(ventaService.siguienteComprobante(cmbTipo.getValue()));
    }

    // ------------------------------------------------------------- acciones

    @FXML
    private void onRegistrar() {
        if (cliente.get() == null) {
            Formularios.marcarError(txtDni, "Selecciona un cliente");
            return;
        }
        if (carrito.isEmpty()) {
            notificador.advertencia("Agrega al menos un producto al carrito.");
            return;
        }
        List<LineaCarrito> lineas = carrito.stream()
                .map(i -> new LineaCarrito(i.producto.getId(), i.cantidad.get(), i.precio.get()))
                .toList();
        try {
            Venta venta = ventaService.registrar(new NuevaVenta(
                    cmbTipo.getValue(), cliente.get().getId(),
                    swIgv.isSelected(), swIncluyeIgv.isSelected(),
                    txtObservacion.getText(), lineas));

            Long id = venta.getId();
            String comprobante = venta.getComprobante();
            notificador.exitoConAcciones("Venta registrada",
                    "Se emitio el comprobante " + comprobante + " por " + Tablas.MONEDA.format(venta.getTotal())
                            + " a nombre de " + cliente.get().getNombreCompleto() + ".",
                    "Nueva venta", this::limpiar,
                    "Imprimir comprobante", () -> {
                        exportador.abrirTemporal(comprobante, Exportador.Formato.PDF,
                                destino -> pdfService.generar(id, destino));
                        limpiar();
                    });
        } catch (NegocioException e) {
            notificador.advertencia(e.getMessage());
        } catch (Exception e) {
            notificador.error("No se pudo registrar la venta: " + e.getMessage());
        }
    }

    @FXML
    private void onLimpiar() {
        if (carrito.isEmpty() && cliente.get() == null) {
            limpiar();
            return;
        }
        notificador.confirmar("¿Vaciar la venta en curso?",
                "Se quitaran el cliente y los " + carrito.size() + " productos del carrito.",
                "Vaciar", this::limpiar);
    }

    private void limpiar() {
        carrito.clear();
        onQuitarCliente();
        txtObservacion.clear();
        autoProducto.setTexto("");
        spnCantidad.getValueFactory().setValue(1);
        swIgv.setSelected(true);
        swIncluyeIgv.setSelected(true);
        actualizarNumero();
    }

    // ---------------------------------------------------------------- apoyo

    private StackPane avatar(Persona p) {
        StackPane a = new StackPane(new Label(Tablas.iniciales(p.getNombreCompleto())));
        a.getStyleClass().add("avatar");
        return a;
    }

    private static void mostrarDato(Label etiqueta, String valor) {
        boolean hay = valor != null && !valor.isBlank();
        etiqueta.setText(hay ? valor : "");
        etiqueta.setVisible(hay);
        etiqueta.setManaged(hay);
    }

    private static void alternar(Node mostrar, Node ocultar) {
        mostrar.setVisible(true);
        mostrar.setManaged(true);
        ocultar.setVisible(false);
        ocultar.setManaged(false);
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : texto.substring(0, 1).toUpperCase(Locale.ROOT) + texto.substring(1);
    }

    // ------------------------------------------------------ modelo y celdas

    /** Linea del carrito con propiedades observables para editar en la tabla. */
    static final class ItemCarrito {
        final Producto producto;
        final IntegerProperty cantidad;
        final ObjectProperty<BigDecimal> precio;
        final ObjectBinding<BigDecimal> importe;

        ItemCarrito(Producto producto, int cantidad, BigDecimal precio) {
            this.producto = producto;
            this.cantidad = new SimpleIntegerProperty(cantidad);
            this.precio = new SimpleObjectProperty<>(precio);
            this.importe = Bindings.createObjectBinding(
                    () -> CalculadoraIgv.importe(this.cantidad.get(), this.precio.get()),
                    this.cantidad, this.precio);
        }
    }
}
