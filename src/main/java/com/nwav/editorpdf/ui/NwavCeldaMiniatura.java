package com.nwav.editorpdf.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Celda del panel de miniaturas. Como la lista es virtual, solo existen celdas para
 * las miniaturas visibles: cada una pide su imagen al mostrarse (renderizado perezoso)
 * y descarta el resultado si entretanto se reutilizo para otra pagina.
 */
public class NwavCeldaMiniatura extends ListCell<Integer> {

    /** Lo que la celda necesita del visor. */
    public interface NwavProveedorMiniaturas {
        Image nwavEnCache(int nwavIndice);

        /** Alto/ancho de la pagina, para reservar el espacio antes de tener la imagen. */
        double nwavProporcion(int nwavIndice);

        void nwavSolicitar(int nwavIndice, IntSupplier nwavIndiceDeseado, Consumer<Image> nwavAlRecibir);
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
}
