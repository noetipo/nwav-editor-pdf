package com.nwav.editorpdf.ui;

import atlantafx.base.theme.Styles;
import com.nwav.editorpdf.model.NwavOpcionesCompresion;
import com.nwav.editorpdf.service.NwavCompresorService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

public class NwavVistaPreviaCompresion {
    private static final float NWAV_ESCALA = 1.5f;

    private final NwavCompresorService nwavCompresor;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavNotificador nwavNotificador;
    private final Function<Map<String, String>, NwavOpcionesCompresion> nwavConvertir;

    public NwavVistaPreviaCompresion(NwavCompresorService nwavCompresor, NwavTareaFondo nwavTareaFondo,
                                     NwavNotificador nwavNotificador,
                                     Function<Map<String, String>, NwavOpcionesCompresion> nwavConvertir) {
        this.nwavCompresor = nwavCompresor;
        this.nwavTareaFondo = nwavTareaFondo;
        this.nwavNotificador = nwavNotificador;
        this.nwavConvertir = nwavConvertir;
    }

    public Node nwavCrear(NwavContextoHerramienta nwavContexto) {
        Label nwavTitulo = new Label("Vista previa de la calidad");
        nwavTitulo.getStyleClass().add("nwav-campo-label");
        Spinner<Integer> nwavPagina = new Spinner<>(1, 100_000, 1);
        nwavPagina.setEditable(true);
        nwavPagina.setPrefWidth(90);
        Button nwavComparar = new Button("Comparar", new FontIcon(Feather.EYE));
        nwavComparar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        HBox nwavControles = new HBox(8, new Label("Página"), nwavPagina, nwavComparar);
        nwavControles.setAlignment(Pos.CENTER_LEFT);

        ImageView nwavAntes = new ImageView();
        ImageView nwavDespues = new ImageView();
        ScrollPane nwavMarcoAntes = nwavMarco(nwavAntes);
        ScrollPane nwavMarcoDespues = nwavMarco(nwavDespues);
        nwavMarcoDespues.hvalueProperty().bindBidirectional(nwavMarcoAntes.hvalueProperty());
        nwavMarcoDespues.vvalueProperty().bindBidirectional(nwavMarcoAntes.vvalueProperty());
        Label nwavEtiquetaAntes = new Label("Original");
        Label nwavEtiquetaDespues = new Label("Comprimido");
        VBox nwavColumnaAntes = new VBox(4, nwavEtiquetaAntes, nwavMarcoAntes);
        VBox nwavColumnaDespues = new VBox(4, nwavEtiquetaDespues, nwavMarcoDespues);
        HBox.setHgrow(nwavColumnaAntes, Priority.ALWAYS);
        HBox.setHgrow(nwavColumnaDespues, Priority.ALWAYS);
        HBox nwavComparacion = new HBox(8, nwavColumnaAntes, nwavColumnaDespues);
        nwavComparacion.setVisible(false);
        nwavComparacion.setManaged(false);

        nwavComparar.setOnAction(nwavEvento -> {
            if (nwavContexto.nwavArchivos().isEmpty()) {
                nwavNotificador.nwavAdvertencia("Añade primero un PDF.");
                return;
            }
            Path nwavArchivo = nwavContexto.nwavArchivos().get(0);
            NwavOpcionesCompresion nwavOpciones = nwavConvertir.apply(nwavContexto.nwavOpciones());
            int nwavIndice = nwavPagina.getValue() - 1;
            nwavComparar.setDisable(true);
            nwavTareaFondo.nwavEjecutarCancelable("Preparando la vista previa…", (nwavAvance, nwavCancelacion) ->
                            nwavCompresor.nwavVistaPrevia(nwavArchivo, nwavContexto.nwavContrasena(nwavArchivo), nwavOpciones,
                                    nwavIndice, NWAV_ESCALA, nwavAvance, nwavCancelacion),
                    nwavVista -> {
                        nwavComparar.setDisable(false);
                        nwavAntes.setImage(NwavImagenFx.nwavConvertir(nwavVista.nwavAntes()));
                        nwavDespues.setImage(NwavImagenFx.nwavConvertir(nwavVista.nwavDespues()));
                        nwavEtiquetaAntes.setText("Original · " + NwavCompresorService.nwavTamano(nwavVista.nwavTamanoAntes()));
                        nwavEtiquetaDespues.setText("Comprimido · " + NwavCompresorService.nwavTamano(nwavVista.nwavTamanoDespues())
                                + (nwavVista.nwavTamanoDespues() >= nwavVista.nwavTamanoAntes() ? " (no reduce)" : ""));
                        nwavComparacion.setVisible(true);
                        nwavComparacion.setManaged(true);
                    }, nwavError -> {
                        nwavComparar.setDisable(false);
                        if (!(nwavError instanceof com.nwav.editorpdf.exception.NwavCanceladoException)) {
                            nwavNotificador.nwavError(com.nwav.editorpdf.exception.NwavTraductorErrores.nwavMensaje(nwavError));
                        }
                    });
        });

        VBox nwavRaiz = new VBox(8, nwavTitulo, nwavControles, nwavComparacion);
        nwavRaiz.getStyleClass().add("nwav-vista-previa");
        return nwavRaiz;
    }

    private static ScrollPane nwavMarco(ImageView nwavVista) {
        ScrollPane nwavMarco = new ScrollPane(nwavVista);
        nwavMarco.setPrefViewportHeight(260);
        nwavMarco.setPrefViewportWidth(180);
        nwavMarco.setPannable(true);
        nwavMarco.getStyleClass().add("nwav-marco-vista-previa");
        return nwavMarco;
    }
}
