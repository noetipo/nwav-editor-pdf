package com.nwav.editorpdf.ui;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Celda del panel de miniaturas. Como la lista es virtual, solo existen celdas para
 * las miniaturas visibles: cada una pide su imagen al mostrarse (renderizado perezoso)
 * y descarta el resultado si entretanto se reutilizo para otra pagina.
 * <p>
 * Tambien permite reordenar arrastrando: se arrastra la seleccion (o la pagina bajo el
 * cursor) y se suelta antes o despues de otra miniatura.
 */
public class NwavCeldaMiniatura extends ListCell<Integer> {

    /** Formato del portapapeles para arrastrar paginas dentro del panel. */
    public static final DataFormat NWAV_FORMATO_PAGINAS = new DataFormat("application/x-nwav-paginas");

    private static final PseudoClass NWAV_SOLTAR_ANTES = PseudoClass.getPseudoClass("nwav-soltar-antes");
    private static final PseudoClass NWAV_SOLTAR_DESPUES = PseudoClass.getPseudoClass("nwav-soltar-despues");

    /** Lo que la celda necesita del visor. */
    public interface NwavProveedorMiniaturas {
        Image nwavEnCache(int nwavIndice);

        /** Alto/ancho de la pagina, para reservar el espacio antes de tener la imagen. */
        double nwavProporcion(int nwavIndice);

        void nwavSolicitar(int nwavIndice, IntSupplier nwavIndiceDeseado, Consumer<Image> nwavAlRecibir);

        /** Mueve las paginas para que queden delante de {@code nwavDestino}. */
        void nwavMover(List<Integer> nwavPaginas, int nwavDestino);

        /** {@code false} mientras se aplica otra edicion. */
        boolean nwavPuedeEditar();
    }

    private final NwavProveedorMiniaturas nwavProveedor;
    private final double nwavAncho;
    private final ImageView nwavVista = new ImageView();
    private final StackPane nwavMarco = new StackPane(nwavVista);
    private final Label nwavNumero = new Label();
    private final VBox nwavContenido = new VBox(6, nwavMarco, nwavNumero);
    private final AtomicInteger nwavIndiceDeseado = new AtomicInteger(-1);

    public NwavCeldaMiniatura(NwavProveedorMiniaturas nwavProveedor, double nwavAncho) {
        this.nwavProveedor = nwavProveedor;
        this.nwavAncho = nwavAncho;
        nwavVista.setPreserveRatio(true);
        nwavVista.setSmooth(true);
        nwavVista.setFitWidth(nwavAncho);
        nwavMarco.getStyleClass().add("nwav-miniatura-marco");
        nwavNumero.getStyleClass().add("nwav-miniatura-numero");
        nwavContenido.setAlignment(Pos.CENTER);
        nwavContenido.getStyleClass().add("nwav-miniatura");
        getStyleClass().add("nwav-celda-miniatura");
        nwavConfigurarArrastre();
    }

    @Override
    protected void updateItem(Integer nwavIndice, boolean nwavVacia) {
        super.updateItem(nwavIndice, nwavVacia);
        if (nwavVacia || nwavIndice == null) {
            nwavIndiceDeseado.set(-1);
            nwavVista.setImage(null);
            setGraphic(null);
            setText(null);
            return;
        }
        nwavIndiceDeseado.set(nwavIndice);
        nwavNumero.setText(String.valueOf(nwavIndice + 1));
        double nwavAlto = nwavAncho * nwavProveedor.nwavProporcion(nwavIndice);
        nwavMarco.setMinSize(nwavAncho, nwavAlto);
        nwavMarco.setPrefSize(nwavAncho, nwavAlto);
        nwavMarco.setMaxSize(nwavAncho, nwavAlto);

        Image nwavGuardada = nwavProveedor.nwavEnCache(nwavIndice);
        nwavVista.setImage(nwavGuardada);
        if (nwavGuardada == null) {
            nwavProveedor.nwavSolicitar(nwavIndice, nwavIndiceDeseado::get, nwavImagen -> {
                if (nwavIndiceDeseado.get() == nwavIndice) {
                    nwavVista.setImage(nwavImagen);
                }
            });
        }
        setText(null);
        setGraphic(nwavContenido);
    }

    // ------------------------------------------------------- arrastrar y soltar

    private void nwavConfigurarArrastre() {
        setOnDragDetected(nwavEvento -> {
            if (isEmpty() || getItem() == null || !nwavProveedor.nwavPuedeEditar()) {
                return;
            }
            List<Integer> nwavSeleccion = new ArrayList<>(getListView().getSelectionModel().getSelectedIndices());
            if (!nwavSeleccion.contains(getIndex())) {
                nwavSeleccion = List.of(getIndex());
            }
            Dragboard nwavPortapapeles = startDragAndDrop(TransferMode.MOVE);
            ClipboardContent nwavContenidoArrastre = new ClipboardContent();
            nwavContenidoArrastre.put(NWAV_FORMATO_PAGINAS, new ArrayList<>(nwavSeleccion.stream().sorted().toList()));
            nwavPortapapeles.setContent(nwavContenidoArrastre);
            SnapshotParameters nwavParametros = new SnapshotParameters();
            nwavParametros.setFill(Color.TRANSPARENT);
            nwavPortapapeles.setDragView(nwavMarco.snapshot(nwavParametros, null));
            nwavEvento.consume();
        });
        setOnDragOver(nwavEvento -> {
            if (nwavEsArrastreDePaginas(nwavEvento)) {
                nwavEvento.acceptTransferModes(TransferMode.MOVE);
                boolean nwavAntes = !isEmpty() && nwavEvento.getY() < getHeight() / 2;
                pseudoClassStateChanged(NWAV_SOLTAR_ANTES, nwavAntes);
                pseudoClassStateChanged(NWAV_SOLTAR_DESPUES, !nwavAntes && !isEmpty());
                nwavEvento.consume();
            }
        });
        setOnDragExited(nwavEvento -> nwavLimpiarMarcas());
        setOnDragDropped(nwavEvento -> {
            if (!nwavEsArrastreDePaginas(nwavEvento)) {
                return;
            }
            @SuppressWarnings("unchecked")
            List<Integer> nwavPaginas = (List<Integer>) nwavEvento.getDragboard().getContent(NWAV_FORMATO_PAGINAS);
            int nwavDestino = isEmpty() || getItem() == null
                    ? getListView().getItems().size()
                    : (nwavEvento.getY() < getHeight() / 2 ? getIndex() : getIndex() + 1);
            nwavLimpiarMarcas();
            nwavProveedor.nwavMover(nwavPaginas, nwavDestino);
            nwavEvento.setDropCompleted(true);
            nwavEvento.consume();
        });
    }

    private boolean nwavEsArrastreDePaginas(DragEvent nwavEvento) {
        return nwavEvento.getDragboard().hasContent(NWAV_FORMATO_PAGINAS)
                && nwavEvento.getGestureSource() instanceof NwavCeldaMiniatura nwavOrigen
                && nwavOrigen.getListView() == getListView();
    }

    private void nwavLimpiarMarcas() {
        pseudoClassStateChanged(NWAV_SOLTAR_ANTES, false);
        pseudoClassStateChanged(NWAV_SOLTAR_DESPUES, false);
    }
}
