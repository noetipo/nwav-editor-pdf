package com.nwav.editorpdf.controller;

import com.nwav.editorpdf.ui.NwavCategoriaHerramienta;
import com.nwav.editorpdf.ui.NwavDefinicionHerramienta;
import com.nwav.editorpdf.ui.NwavRefrescable;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NwavInicioController implements NwavRefrescable {
    private final NwavCatalogoHerramientas nwavCatalogo;
    private final NwavMainController nwavMain;
    private final NwavTareaFondo nwavTareaFondo;
    private final com.nwav.editorpdf.service.herramienta.NwavDependenciasService nwavDependencias;

    @FXML private TextField nwavTxtBuscar;
    @FXML private VBox nwavContenedorCategorias;
    @FXML private Label nwavLblSinResultados;

    @FXML
    public void initialize() {
        nwavTxtBuscar.textProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavPintar());
        nwavPintar();
    }

    @Override
    public void nwavRefrescar() {
        nwavTareaFondo.nwavEjecutarSilencioso(() -> {
            nwavCatalogo.nwavTodas().forEach(NwavDefinicionHerramienta::nwavEstado);
            return null;
        }, nwavNada -> nwavPintar());
    }

    private void nwavPintar() {
        String nwavBusqueda = nwavTxtBuscar.getText();
        nwavContenedorCategorias.getChildren().clear();
        int nwavMostradas = 0;
        for (NwavCategoriaHerramienta nwavCategoria : NwavCategoriaHerramienta.values()) {
            List<NwavDefinicionHerramienta> nwavHerramientas = nwavCatalogo.nwavTodas().stream()
                    .filter(nwavDefinicion -> nwavDefinicion.getNwavCategoria() == nwavCategoria)
                    .filter(nwavDefinicion -> nwavDefinicion.nwavCoincide(nwavBusqueda)).toList();
            if (nwavHerramientas.isEmpty()) {
                continue;
            }
            Label nwavTitulo = new Label(nwavCategoria.getNwavNombre().toUpperCase(java.util.Locale.ROOT));
            nwavTitulo.getStyleClass().add("nwav-titulo-categoria");
            FlowPane nwavTarjetas = new FlowPane(14, 14);
            nwavHerramientas.forEach(nwavDefinicion -> nwavTarjetas.getChildren().add(nwavTarjeta(nwavDefinicion)));
            nwavContenedorCategorias.getChildren().add(new VBox(10, nwavTitulo, nwavTarjetas));
            nwavMostradas += nwavHerramientas.size();
        }
        nwavLblSinResultados.setVisible(nwavMostradas == 0);
        nwavLblSinResultados.setManaged(nwavMostradas == 0);
    }

    private Node nwavTarjeta(NwavDefinicionHerramienta nwavDefinicion) {
        StackPane nwavIcono = new StackPane(new FontIcon(nwavDefinicion.getNwavIcono()));
        nwavIcono.getStyleClass().addAll("nwav-icono-herramienta", nwavDefinicion.getNwavCategoria().getNwavClaseEstilo());
        Label nwavNombre = new Label(nwavDefinicion.getNwavNombre());
        nwavNombre.getStyleClass().add("nwav-tarjeta-herramienta-titulo");
        Label nwavDescripcion = new Label(nwavDefinicion.getNwavDescripcion());
        nwavDescripcion.getStyleClass().add("nwav-tarjeta-herramienta-texto");
        nwavDescripcion.setWrapText(true);
        VBox nwavTarjeta = new VBox(10, nwavIcono, nwavNombre, nwavDescripcion);
        nwavTarjeta.setAlignment(Pos.TOP_LEFT);
        nwavTarjeta.getStyleClass().add("nwav-tarjeta-herramienta");
        var nwavEstado = nwavDependencias.nwavComprobado() ? nwavDefinicion.nwavEstado() : null;
        if (nwavEstado != null && !nwavEstado.nwavDisponible()) {
            nwavTarjeta.getStyleClass().add("nwav-tarjeta-no-disponible");
            Label nwavFalta = new Label(nwavEstado.nwavDetalle());
            nwavFalta.getStyleClass().add("nwav-tarjeta-herramienta-aviso");
            nwavFalta.setWrapText(true);
            nwavTarjeta.getChildren().add(nwavFalta);
            Tooltip.install(nwavTarjeta, new Tooltip(nwavEstado.nwavInstrucciones()));
        }
        nwavTarjeta.setOnMouseClicked(nwavEvento -> nwavMain.nwavAbrirHerramienta(nwavDefinicion, List.of()));
        return nwavTarjeta;
    }
}
