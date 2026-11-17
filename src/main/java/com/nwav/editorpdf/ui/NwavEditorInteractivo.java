package com.nwav.editorpdf.ui;

import atlantafx.base.theme.Styles;
import com.nwav.editorpdf.model.NwavColorRgb;
import com.nwav.editorpdf.model.NwavEstiloTexto;
import com.nwav.editorpdf.model.NwavPunto;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.model.NwavTipoForma;
import com.nwav.editorpdf.model.NwavTipoMarca;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;

/**
 * Interaccion de edicion sobre las paginas del visor. Cada pagina tiene una capa
 * transparente encima; segun la herramienta, arrastrar dibuja una vista previa y al
 * soltar se pide la edicion al {@link NwavDestino}. Texto, imagen y nota quedan como
 * elemento "pendiente" (editable, movible) hasta que el usuario lo fija con ✓.
 * <p>
 * Trabaja en pixeles de pantalla y convierte a coordenadas visuales en puntos PDF
 * dividiendo por la escala actual (pixeles por punto).
 */
public class NwavEditorInteractivo {

    /** Lo que el editor pide al visor. Los callbacks indican si la edicion se aplico. */
    public interface NwavDestino {
        void nwavFijarTexto(int nwavPagina, NwavRectangulo nwavZona, String nwavTexto, NwavEstiloTexto nwavEstilo,
                            Runnable nwavSiAplica, Runnable nwavSiFalla);

        void nwavFijarImagen(int nwavPagina, Path nwavImagen, NwavRectangulo nwavZona,
                             Runnable nwavSiAplica, Runnable nwavSiFalla);

        void nwavFijarNota(int nwavPagina, NwavPunto nwavPunto, String nwavTexto, NwavColorRgb nwavColor,
                           Runnable nwavSiAplica, Runnable nwavSiFalla);

        /** Imagen ya en memoria (p. ej. una firma guardada). */
        void nwavFijarImagenBytes(int nwavPagina, byte[] nwavPng, NwavRectangulo nwavZona,
                                  Runnable nwavSiAplica, Runnable nwavSiFalla);

        void nwavMarcar(int nwavPagina, NwavTipoMarca nwavTipo, NwavRectangulo nwavZona, NwavColorRgb nwavColor);

        void nwavDibujar(int nwavPagina, List<NwavPunto> nwavTrazo, NwavColorRgb nwavColor, double nwavGrosor);

        void nwavForma(int nwavPagina, NwavTipoForma nwavTipo, NwavPunto nwavDesde, NwavPunto nwavHasta,
                       NwavColorRgb nwavColor, double nwavGrosor);

        void nwavCubrir(int nwavPagina, NwavRectangulo nwavZona, NwavColorRgb nwavColor);

        void nwavRecortar(int nwavPagina, NwavRectangulo nwavZona);

        void nwavCrearCampo(int nwavPagina, NwavRectangulo nwavZona);

        void nwavRedactar(int nwavPagina, NwavRectangulo nwavZona);

        Optional<Path> nwavElegirImagen();

        boolean nwavPuedeEditar();
    }

    private static final double NWAV_ARRASTRE_MINIMO = 3;
    private static final double NWAV_ANCHO_TEXTO_DEFECTO = 220;
    private static final double NWAV_TAMANO_ASA = 12;

    private final NwavEstadoHerramientas nwavEstado;
    private final NwavDestino nwavDestino;
    private final DoubleSupplier nwavEscala;

    // Arrastre en curso.
    private Pane nwavCapaActiva;
    private int nwavPaginaActiva = -1;
    private Point2D nwavInicio;
    private Shape nwavVistaPrevia;
    private final List<Point2D> nwavTrazo = new ArrayList<>();

    /** Elemento pendiente de fijar (solo uno a la vez). */
    private NwavPendiente nwavPendiente;

    public NwavEditorInteractivo(NwavEstadoHerramientas nwavEstado, NwavDestino nwavDestino, DoubleSupplier nwavEscala) {
        this.nwavEstado = nwavEstado;
        this.nwavDestino = nwavDestino;
        this.nwavEscala = nwavEscala;
        nwavEstado.nwavHerramientaProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavCancelarPendiente());
    }

    /** Prepara la capa de una pagina. La capa debe cubrir exactamente la pagina. */
    public void nwavInstalar(Pane nwavCapa, int nwavPagina) {
        nwavCapa.getStyleClass().add("nwav-capa-edicion");
        nwavCapa.mouseTransparentProperty().bind(
                nwavEstado.nwavHerramientaProperty().isEqualTo(NwavHerramienta.NWAV_NAVEGAR));
        nwavCapa.cursorProperty().bind(Bindings.createObjectBinding(() ->
                switch (nwavEstado.nwavHerramienta()) {
                    case NWAV_NAVEGAR -> Cursor.DEFAULT;
                    case NWAV_TEXTO -> Cursor.TEXT;
                    case NWAV_NOTA, NWAV_IMAGEN -> Cursor.HAND;
                    default -> Cursor.CROSSHAIR;
                }, nwavEstado.nwavHerramientaProperty()));
        Rectangle nwavRecorte = new Rectangle();
        nwavRecorte.widthProperty().bind(nwavCapa.widthProperty());
        nwavRecorte.heightProperty().bind(nwavCapa.heightProperty());
        nwavCapa.setClip(nwavRecorte);
        nwavCapa.setOnMousePressed(nwavEvento -> nwavAlPresionar(nwavCapa, nwavPagina, nwavEvento));
        nwavCapa.setOnMouseDragged(nwavEvento -> nwavAlArrastrar(nwavCapa, nwavEvento));
        nwavCapa.setOnMouseReleased(nwavEvento -> nwavAlSoltar(nwavCapa, nwavEvento));
    }

    public boolean nwavHayPendiente() {
        return nwavPendiente != null;
    }

    /** Descarta el elemento pendiente y cualquier arrastre a medias. */
    public void nwavCancelarPendiente() {
        if (nwavPendiente != null) {
            nwavPendiente.nwavQuitar();
            nwavPendiente = null;
        }
        nwavLimpiarArrastre();
    }

    // ================================================================ raton

    private void nwavAlPresionar(Pane nwavCapa, int nwavPagina, MouseEvent nwavEvento) {
        // Solo cuentan los clics sobre la capa vacia (no sobre un elemento pendiente).
        if (nwavEvento.getTarget() != nwavCapa || !nwavEvento.isPrimaryButtonDown() || nwavPendiente != null
                || !nwavDestino.nwavPuedeEditar()) {
            return;
        }
        nwavCapaActiva = nwavCapa;
        nwavPaginaActiva = nwavPagina;
        nwavInicio = nwavAcotar(nwavCapa, nwavEvento.getX(), nwavEvento.getY());
        nwavTrazo.clear();
        NwavHerramienta nwavHerramienta = nwavEstado.nwavHerramienta();
        Color nwavColor = nwavEstado.nwavColorProperty().get();
        nwavVistaPrevia = switch (nwavHerramienta) {
            case NWAV_RESALTAR -> nwavRectanguloPrevio(Color.color(nwavColor.getRed(), nwavColor.getGreen(),
                    nwavColor.getBlue(), 0.35), null);
            case NWAV_SUBRAYAR, NWAV_TACHAR, NWAV_TEXTO -> nwavRectanguloPrevio(Color.TRANSPARENT, Color.DODGERBLUE);
            case NWAV_RECORTAR -> nwavRectanguloPrevio(Color.color(0.12, 0.56, 1, 0.08), Color.DODGERBLUE);
            case NWAV_REDACTAR -> nwavRectanguloPrevio(Color.color(0, 0, 0, 0.55), Color.BLACK);
            case NWAV_CAMPO -> nwavRectanguloPrevio(Color.color(0.55, 0.36, 0.96, 0.12), Color.web("#8b5cf6"));
            case NWAV_CUBRIR -> nwavRectanguloPrevio(nwavColor, Color.DODGERBLUE);
            case NWAV_RECTANGULO -> nwavRectanguloPrevio(Color.TRANSPARENT, nwavColor);
            case NWAV_ELIPSE -> {
                Ellipse nwavElipse = new Ellipse();
                nwavElipse.setFill(Color.TRANSPARENT);
                nwavElipse.setStroke(nwavColor);
                yield nwavElipse;
            }
            case NWAV_LINEA, NWAV_FLECHA -> new Line(nwavInicio.getX(), nwavInicio.getY(), nwavInicio.getX(),
                    nwavInicio.getY());
            case NWAV_DIBUJO -> {
                nwavTrazo.add(nwavInicio);
                Polyline nwavLinea = new Polyline(nwavInicio.getX(), nwavInicio.getY());
                nwavLinea.setStrokeLineCap(StrokeLineCap.ROUND);
                nwavLinea.setStrokeLineJoin(StrokeLineJoin.ROUND);
                yield nwavLinea;
            }
            default -> null;
        };
        if (nwavVistaPrevia != null) {
            if (nwavHerramienta.nwavUsaGrosor()) {
                nwavVistaPrevia.setStroke(nwavColor);
                nwavVistaPrevia.setStrokeWidth(Math.max(1, nwavEstado.nwavGrosor() * nwavEscala.getAsDouble()));
            }
            nwavVistaPrevia.setMouseTransparent(true);
            nwavCapa.getChildren().add(nwavVistaPrevia);
        }
        nwavEvento.consume();
    }

    private Rectangle nwavRectanguloPrevio(Color nwavRelleno, Color nwavBorde) {
        Rectangle nwavRectangulo = new Rectangle(nwavInicio.getX(), nwavInicio.getY(), 0, 0);
        nwavRectangulo.setFill(nwavRelleno);
        if (nwavBorde != null) {
            nwavRectangulo.setStroke(nwavBorde);
            nwavRectangulo.getStrokeDashArray().setAll(6.0, 4.0);
        }
        return nwavRectangulo;
    }

    private void nwavAlArrastrar(Pane nwavCapa, MouseEvent nwavEvento) {
        if (nwavCapa != nwavCapaActiva || nwavInicio == null) {
            return;
        }
        Point2D nwavActual = nwavAcotar(nwavCapa, nwavEvento.getX(), nwavEvento.getY());
        if (nwavVistaPrevia instanceof Rectangle nwavRectangulo) {
            nwavRectangulo.setX(Math.min(nwavInicio.getX(), nwavActual.getX()));
            nwavRectangulo.setY(Math.min(nwavInicio.getY(), nwavActual.getY()));
            nwavRectangulo.setWidth(Math.abs(nwavActual.getX() - nwavInicio.getX()));
            nwavRectangulo.setHeight(Math.abs(nwavActual.getY() - nwavInicio.getY()));
        } else if (nwavVistaPrevia instanceof Ellipse nwavElipse) {
            nwavElipse.setCenterX((nwavInicio.getX() + nwavActual.getX()) / 2);
            nwavElipse.setCenterY((nwavInicio.getY() + nwavActual.getY()) / 2);
            nwavElipse.setRadiusX(Math.abs(nwavActual.getX() - nwavInicio.getX()) / 2);
            nwavElipse.setRadiusY(Math.abs(nwavActual.getY() - nwavInicio.getY()) / 2);
        } else if (nwavVistaPrevia instanceof Line nwavLinea) {
            nwavLinea.setEndX(nwavActual.getX());
            nwavLinea.setEndY(nwavActual.getY());
        } else if (nwavVistaPrevia instanceof Polyline nwavLinea) {
            nwavTrazo.add(nwavActual);
            nwavLinea.getPoints().addAll(nwavActual.getX(), nwavActual.getY());
        }
        nwavEvento.consume();
    }

    private void nwavAlSoltar(Pane nwavCapa, MouseEvent nwavEvento) {
        if (nwavCapa != nwavCapaActiva || nwavInicio == null) {
            return;
        }
        Point2D nwavFin = nwavAcotar(nwavCapa, nwavEvento.getX(), nwavEvento.getY());
        int nwavPagina = nwavPaginaActiva;
        Point2D nwavDesde = nwavInicio;
        boolean nwavHuboArrastre = nwavDesde.distance(nwavFin) >= NWAV_ARRASTRE_MINIMO;
        NwavRectangulo nwavZona = NwavRectangulo.nwavEntre(nwavAPuntos(nwavDesde), nwavAPuntos(nwavFin));
        NwavColorRgb nwavColor = nwavEstado.nwavColorRgb();
        double nwavGrosor = nwavEstado.nwavGrosor();
        List<NwavPunto> nwavPuntosTrazo = nwavTrazo.stream().map(this::nwavAPuntos).toList();
        nwavLimpiarArrastre();
        nwavEvento.consume();

        switch (nwavEstado.nwavHerramienta()) {
            case NWAV_RESALTAR, NWAV_SUBRAYAR, NWAV_TACHAR -> {
                if (nwavHuboArrastre) {
                    NwavTipoMarca nwavTipo = switch (nwavEstado.nwavHerramienta()) {
                        case NWAV_SUBRAYAR -> NwavTipoMarca.NWAV_SUBRAYADO;
                        case NWAV_TACHAR -> NwavTipoMarca.NWAV_TACHADO;
                        default -> NwavTipoMarca.NWAV_RESALTADO;
                    };
                    nwavDestino.nwavMarcar(nwavPagina, nwavTipo, nwavZona, nwavColor);
                }
            }
            case NWAV_CUBRIR -> {
                if (nwavHuboArrastre) {
                    nwavDestino.nwavCubrir(nwavPagina, nwavZona, nwavColor);
                }
            }
            case NWAV_RECORTAR -> {
                if (nwavHuboArrastre) {
                    nwavDestino.nwavRecortar(nwavPagina, nwavZona);
                }
            }
            case NWAV_CAMPO -> {
                if (nwavHuboArrastre) {
                    nwavDestino.nwavCrearCampo(nwavPagina, nwavZona);
                }
            }
            case NWAV_REDACTAR -> {
                if (nwavHuboArrastre) {
                    nwavDestino.nwavRedactar(nwavPagina, nwavZona);
                }
            }
            case NWAV_RECTANGULO, NWAV_ELIPSE, NWAV_LINEA, NWAV_FLECHA -> {
                if (nwavHuboArrastre) {
                    NwavTipoForma nwavTipo = switch (nwavEstado.nwavHerramienta()) {
                        case NWAV_ELIPSE -> NwavTipoForma.NWAV_ELIPSE;
                        case NWAV_LINEA -> NwavTipoForma.NWAV_LINEA;
                        case NWAV_FLECHA -> NwavTipoForma.NWAV_FLECHA;
                        default -> NwavTipoForma.NWAV_RECTANGULO;
                    };
                    nwavDestino.nwavForma(nwavPagina, nwavTipo, nwavAPuntos(nwavDesde), nwavAPuntos(nwavFin),
                            nwavColor, nwavGrosor);
                }
            }
            case NWAV_DIBUJO -> {
                if (nwavPuntosTrazo.size() >= 2) {
                    nwavDestino.nwavDibujar(nwavPagina, nwavPuntosTrazo, nwavColor, nwavGrosor);
                }
            }
            case NWAV_TEXTO -> {
                double nwavAncho = nwavHuboArrastre && nwavZona.nwavAncho() > 20 ? nwavZona.nwavAncho()
                        : NWAV_ANCHO_TEXTO_DEFECTO;
                nwavPendiente = new NwavTextoPendiente(nwavCapa, nwavPagina, nwavZona.nwavX(), nwavZona.nwavY(),
                        nwavAncho);
            }
            case NWAV_NOTA -> nwavPendiente = new NwavNotaPendiente(nwavCapa, nwavPagina, nwavAPuntos(nwavFin));
            case NWAV_IMAGEN -> nwavDestino.nwavElegirImagen().ifPresent(nwavRuta ->
                    nwavPendiente = new NwavImagenPendiente(nwavCapa, nwavPagina, nwavRuta, null, nwavAPuntos(nwavFin)));
            default -> {
                // navegar: nada
            }
        }
    }

    private void nwavLimpiarArrastre() {
        if (nwavCapaActiva != null && nwavVistaPrevia != null) {
            nwavCapaActiva.getChildren().remove(nwavVistaPrevia);
        }
        nwavVistaPrevia = null;
        nwavCapaActiva = null;
        nwavInicio = null;
        nwavTrazo.clear();
    }

    private static Point2D nwavAcotar(Pane nwavCapa, double nwavX, double nwavY) {
        return new Point2D(Math.max(0, Math.min(nwavCapa.getWidth(), nwavX)),
                Math.max(0, Math.min(nwavCapa.getHeight(), nwavY)));
    }

    private NwavPunto nwavAPuntos(Point2D nwavPixel) {
        double nwavPorPunto = nwavEscala.getAsDouble();
        return new NwavPunto(nwavPixel.getX() / nwavPorPunto, nwavPixel.getY() / nwavPorPunto);
    }

    /**
     * Coloca en el centro de la pagina una imagen en memoria (una firma) como elemento
     * pendiente: se puede mover y redimensionar antes de fijarla.
     */
    public void nwavColocarImagen(Pane nwavCapa, int nwavPagina, byte[] nwavPng) {
        nwavCancelarPendiente();
        double nwavPorPunto = nwavEscala.getAsDouble();
        NwavPunto nwavCentro = new NwavPunto(nwavCapa.getWidth() / nwavPorPunto / 2,
                nwavCapa.getHeight() / nwavPorPunto / 2);
        nwavPendiente = new NwavImagenPendiente(nwavCapa, nwavPagina, null, nwavPng, nwavCentro);
    }

    // ============================================================ pendientes

    /**
     * Elemento flotante sin fijar. Guarda su posicion en puntos PDF para recolocarse si
     * cambia el zoom mientras se edita.
     */
    private abstract class NwavPendiente {

        protected final Pane nwavCapa;
        protected final int nwavPagina;
        protected final VBox nwavRaiz = new VBox(4);
        private final javafx.beans.value.ChangeListener<Number> nwavOyenteTamano =
                (nwavObs, nwavAntes, nwavAhora) -> nwavReubicar();

        NwavPendiente(Pane nwavCapa, int nwavPagina) {
            this.nwavCapa = nwavCapa;
            this.nwavPagina = nwavPagina;
            nwavRaiz.getStyleClass().add("nwav-pendiente");
            nwavRaiz.setOnKeyPressed(nwavEvento -> {
                if (nwavEvento.getCode() == KeyCode.ESCAPE) {
                    nwavCancelarPendiente();
                    nwavEvento.consume();
                } else if (nwavEvento.getCode() == KeyCode.ENTER && nwavEvento.isShortcutDown()) {
                    nwavIntentarFijar();
                    nwavEvento.consume();
                }
            });
            nwavCapa.widthProperty().addListener(nwavOyenteTamano);
        }

        /** Barra con ✓ (fijar) y ✕ (cancelar). */
        protected HBox nwavCrearAcciones(String nwavTextoFijar) {
            Button nwavFijar = new Button(null, new FontIcon(Feather.CHECK));
            nwavFijar.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.SUCCESS, Styles.SMALL);
            nwavFijar.setTooltip(new Tooltip(nwavTextoFijar + " (Ctrl/⌘ + Intro)"));
            nwavFijar.setOnAction(nwavEvento -> nwavIntentarFijar());
            Button nwavCancelar = new Button(null, new FontIcon(Feather.X));
            nwavCancelar.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.DANGER, Styles.SMALL);
            nwavCancelar.setTooltip(new Tooltip("Descartar (Esc)"));
            nwavCancelar.setOnAction(nwavEvento -> nwavCancelarPendiente());
            HBox nwavAcciones = new HBox(4, nwavFijar, nwavCancelar);
            nwavAcciones.setAlignment(Pos.CENTER_LEFT);
            nwavAcciones.getStyleClass().add("nwav-pendiente-acciones");
            return nwavAcciones;
        }

        protected void nwavMostrar() {
            nwavCapa.getChildren().add(nwavRaiz);
            nwavReubicar();
        }

        /** Oculta mientras se aplica la edicion; si falla, se vuelve a mostrar. */
        private void nwavIntentarFijar() {
            nwavRaiz.setVisible(false);
            nwavFijar(() -> {
                if (nwavPendiente == this) {
                    nwavQuitar();
                    nwavPendiente = null;
                }
            }, () -> nwavRaiz.setVisible(true));
        }

        void nwavQuitar() {
            nwavCapa.widthProperty().removeListener(nwavOyenteTamano);
            nwavCapa.getChildren().remove(nwavRaiz);
        }

        abstract void nwavReubicar();

        abstract void nwavFijar(Runnable nwavSiAplica, Runnable nwavSiFalla);
    }

    /** Cuadro de texto: la vista previa usa la misma letra, tamano y color. */
    private final class NwavTextoPendiente extends NwavPendiente {

        private final double nwavX;
        private final double nwavY;
        private final double nwavAncho;
        private final TextArea nwavEditor = new TextArea();
        private final javafx.beans.InvalidationListener nwavOyenteEstilo = nwavObs -> nwavReubicar();

        NwavTextoPendiente(Pane nwavCapa, int nwavPagina, double nwavX, double nwavY, double nwavAncho) {
            super(nwavCapa, nwavPagina);
            this.nwavX = nwavX;
            this.nwavY = nwavY;
            this.nwavAncho = Math.min(nwavAncho, nwavCapa.getWidth() / nwavEscala.getAsDouble() - nwavX);
            nwavEditor.setWrapText(true);
            nwavEditor.setPrefRowCount(3);
            nwavEditor.setPromptText("Escribe aquí…");
            nwavEditor.getStyleClass().add("nwav-texto-pendiente");
            nwavRaiz.getChildren().setAll(nwavEditor, nwavCrearAcciones("Fijar texto"));
            nwavPropiedadesEstilo().forEach(nwavPropiedad -> nwavPropiedad.addListener(nwavOyenteEstilo));
            nwavMostrar();
            Platform.runLater(nwavEditor::requestFocus);
        }

        @Override
        void nwavReubicar() {
            double nwavPorPunto = nwavEscala.getAsDouble();
            nwavEditor.setFont(nwavEstado.nwavFuentePantalla(nwavPorPunto));
            Color nwavColor = nwavEstado.nwavColorProperty().get();
            nwavEditor.setStyle("-fx-text-fill: " + nwavEstado.nwavColorRgb().nwavHex() + ";"
                    + (nwavColor.getBrightness() > 0.85 ? "-fx-control-inner-background: #555;" : ""));
            nwavEditor.setPrefWidth(nwavAncho * nwavPorPunto + 16);
            nwavRaiz.relocate(nwavX * nwavPorPunto - 8, nwavY * nwavPorPunto - 6);
        }

        private List<javafx.beans.Observable> nwavPropiedadesEstilo() {
            return List.of(nwavEstado.nwavFuenteProperty(), nwavEstado.nwavTamanoProperty(),
                    nwavEstado.nwavNegritaProperty(), nwavEstado.nwavCursivaProperty(), nwavEstado.nwavColorProperty());
        }

        @Override
        void nwavQuitar() {
            nwavPropiedadesEstilo().forEach(nwavPropiedad -> nwavPropiedad.removeListener(nwavOyenteEstilo));
            super.nwavQuitar();
        }

        @Override
        void nwavFijar(Runnable nwavSiAplica, Runnable nwavSiFalla) {
            nwavDestino.nwavFijarTexto(nwavPagina, new NwavRectangulo(nwavX, nwavY, nwavAncho, 0),
                    nwavEditor.getText(), nwavEstado.nwavEstiloTexto(), nwavSiAplica, nwavSiFalla);
        }
    }

    /** Nota: un cuadro pequeno para escribir el texto de la nota. */
    private final class NwavNotaPendiente extends NwavPendiente {

        private final NwavPunto nwavPunto;
        private final TextArea nwavEditor = new TextArea();

        NwavNotaPendiente(Pane nwavCapa, int nwavPagina, NwavPunto nwavPunto) {
            super(nwavCapa, nwavPagina);
            this.nwavPunto = nwavPunto;
            FontIcon nwavIcono = new FontIcon(Feather.MESSAGE_SQUARE);
            nwavIcono.getStyleClass().add("nwav-nota-icono");
            nwavEditor.setWrapText(true);
            nwavEditor.setPrefRowCount(4);
            nwavEditor.setPrefWidth(240);
            nwavEditor.setPromptText("Texto de la nota…");
            nwavRaiz.getStyleClass().add("nwav-nota-pendiente");
            nwavRaiz.getChildren().setAll(nwavIcono, nwavEditor, nwavCrearAcciones("Fijar nota"));
            nwavMostrar();
            Platform.runLater(nwavEditor::requestFocus);
        }

        @Override
        void nwavReubicar() {
            double nwavPorPunto = nwavEscala.getAsDouble();
            double nwavX = Math.min(nwavPunto.nwavX() * nwavPorPunto, Math.max(0, nwavCapa.getWidth() - 260));
            nwavRaiz.relocate(nwavX, nwavPunto.nwavY() * nwavPorPunto);
        }

        @Override
        void nwavFijar(Runnable nwavSiAplica, Runnable nwavSiFalla) {
            nwavDestino.nwavFijarNota(nwavPagina, nwavPunto, nwavEditor.getText(), nwavEstado.nwavColorRgb(),
                    nwavSiAplica, nwavSiFalla);
        }
    }

    /** Imagen: se mueve arrastrandola y se redimensiona (manteniendo la proporcion) por la esquina. */
    private final class NwavImagenPendiente extends NwavPendiente {

        private final Path nwavRuta;
        private final byte[] nwavBytes;
        private final ImageView nwavVista = new ImageView();
        private final StackPane nwavMarco = new StackPane();
        private final Region nwavAsa = new Region();
        private final double nwavProporcion;
        private double nwavX;
        private double nwavY;
        private double nwavAncho;
        private Point2D nwavAgarre;

        /** Desde un archivo ({@code nwavRuta}) o desde memoria ({@code nwavBytes}). */
        NwavImagenPendiente(Pane nwavCapa, int nwavPagina, Path nwavRuta, byte[] nwavBytes, NwavPunto nwavCentro) {
            super(nwavCapa, nwavPagina);
            this.nwavRuta = nwavRuta;
            this.nwavBytes = nwavBytes;
            Image nwavImagen = nwavBytes != null
                    ? new Image(new java.io.ByteArrayInputStream(nwavBytes))
                    : new Image(nwavRuta.toUri().toString());
            double nwavAnchoNatural = nwavImagen.getWidth() > 0 ? nwavImagen.getWidth() * 0.75 : 200;
            double nwavAltoNatural = nwavImagen.getHeight() > 0 ? nwavImagen.getHeight() * 0.75 : 150;
            nwavProporcion = nwavAltoNatural / nwavAnchoNatural;
            double nwavAnchoPagina = nwavCapa.getWidth() / nwavEscala.getAsDouble();
            nwavAncho = Math.min(nwavAnchoNatural, nwavAnchoPagina * 0.5);
            nwavX = Math.max(0, nwavCentro.nwavX() - nwavAncho / 2);
            nwavY = Math.max(0, nwavCentro.nwavY() - nwavAncho * nwavProporcion / 2);

            nwavVista.setImage(nwavImagen);
            nwavVista.setPreserveRatio(false);
            nwavAsa.getStyleClass().add("nwav-asa");
            nwavAsa.setPrefSize(NWAV_TAMANO_ASA, NWAV_TAMANO_ASA);
            nwavAsa.setMaxSize(NWAV_TAMANO_ASA, NWAV_TAMANO_ASA);
            nwavAsa.setCursor(Cursor.SE_RESIZE);
            StackPane.setAlignment(nwavAsa, Pos.BOTTOM_RIGHT);
            nwavMarco.getChildren().setAll(nwavVista, nwavAsa);
            nwavMarco.getStyleClass().add("nwav-imagen-pendiente");
            nwavMarco.setCursor(Cursor.MOVE);
            nwavRaiz.getChildren().setAll(nwavCrearAcciones("Fijar imagen"), nwavMarco);

            nwavMarco.setOnMousePressed(nwavEvento -> {
                nwavAgarre = new Point2D(nwavEvento.getSceneX(), nwavEvento.getSceneY());
                nwavEvento.consume();
            });
            nwavMarco.setOnMouseDragged(nwavEvento -> {
                if (nwavAgarre == null) {
                    return;
                }
                double nwavPorPunto = nwavEscala.getAsDouble();
                double nwavDx = (nwavEvento.getSceneX() - nwavAgarre.getX()) / nwavPorPunto;
                double nwavDy = (nwavEvento.getSceneY() - nwavAgarre.getY()) / nwavPorPunto;
                nwavAgarre = new Point2D(nwavEvento.getSceneX(), nwavEvento.getSceneY());
                if (nwavEvento.getTarget() == nwavAsa) {
                    nwavAncho = Math.max(10, nwavAncho + Math.max(nwavDx, nwavDy / nwavProporcion));
                } else {
                    nwavX += nwavDx;
                    nwavY += nwavDy;
                }
                nwavReubicar();
                nwavEvento.consume();
            });
            nwavMarco.setOnMouseReleased(nwavEvento -> {
                nwavAgarre = null;
                nwavEvento.consume();
            });
            nwavMostrar();
            nwavRaiz.requestFocus();
        }

        @Override
        void nwavReubicar() {
            double nwavPorPunto = nwavEscala.getAsDouble();
            double nwavAnchoPx = nwavAncho * nwavPorPunto;
            double nwavAltoPx = nwavAncho * nwavProporcion * nwavPorPunto;
            nwavVista.setFitWidth(nwavAnchoPx);
            nwavVista.setFitHeight(nwavAltoPx);
            nwavMarco.setPrefSize(nwavAnchoPx, nwavAltoPx);
            nwavMarco.setMaxSize(nwavAnchoPx, nwavAltoPx);
            // Las acciones van encima de la imagen: la imagen queda en (x, y).
            nwavRaiz.relocate(nwavX * nwavPorPunto, nwavY * nwavPorPunto - 32);
        }

        @Override
        void nwavFijar(Runnable nwavSiAplica, Runnable nwavSiFalla) {
            NwavRectangulo nwavZona = new NwavRectangulo(nwavX, nwavY, nwavAncho, nwavAncho * nwavProporcion);
            if (nwavBytes != null) {
                nwavDestino.nwavFijarImagenBytes(nwavPagina, nwavBytes, nwavZona, nwavSiAplica, nwavSiFalla);
            } else {
                nwavDestino.nwavFijarImagen(nwavPagina, nwavRuta, nwavZona, nwavSiAplica, nwavSiFalla);
            }
        }
    }

    /** Utilidad para el visor: nodo de ayuda visible solo con una herramienta activa. */
    public static boolean nwavEsEdicion(NwavHerramienta nwavHerramienta) {
        return nwavHerramienta != null && nwavHerramienta != NwavHerramienta.NWAV_NAVEGAR;
    }
}
