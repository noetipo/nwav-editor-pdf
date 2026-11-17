package com.nwav.editorpdf.controller;

import com.nwav.editorpdf.model.NwavAlineacion;
import com.nwav.editorpdf.model.NwavFuente;
import com.nwav.editorpdf.model.NwavOpcionesEncabezado;
import com.nwav.editorpdf.service.NwavEdicionService;
import com.nwav.editorpdf.ui.NwavEstadoHerramientas;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavPanel;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NwavPanelEncabezadoController implements NwavPanel {
    private final NwavVisorController nwavVisor;
    private final NwavEdicionService nwavEdicion;
    private final NwavNotificador nwavNotificador;

    @FXML private TextField nwavTxtEncabezado;
    @FXML private ComboBox<NwavAlineacion> nwavCmbAlineacionEncabezado;
    @FXML private TextField nwavTxtPie;
    @FXML private ComboBox<NwavAlineacion> nwavCmbAlineacionPie;
    @FXML private ComboBox<NwavFuente> nwavCmbFuente;
    @FXML private Spinner<Integer> nwavSpnTamano;
    @FXML private ColorPicker nwavSelectorColor;
    @FXML private Spinner<Integer> nwavSpnMargen;
    @FXML private Spinner<Integer> nwavSpnNumeroInicial;
    @FXML private TextField nwavTxtPaginas;

    @FXML
    public void initialize() {
        for (ComboBox<NwavAlineacion> nwavCombo : java.util.List.of(nwavCmbAlineacionEncabezado, nwavCmbAlineacionPie)) {
            nwavCombo.getItems().setAll(NwavAlineacion.values());
            nwavCombo.setValue(NwavAlineacion.NWAV_CENTRO);
        }
        nwavCmbFuente.getItems().setAll(NwavFuente.values());
        nwavCmbFuente.setValue(NwavFuente.NWAV_HELVETICA);
        nwavSpnTamano.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 72, 10));
        nwavSpnMargen.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 200, 28));
        nwavSpnNumeroInicial.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100_000, 1));
        nwavSelectorColor.setValue(Color.rgb(60, 60, 60));
    }

    @Override
    public void nwavPreparar() {
    }

    public void nwavPrepararNumeracion() {
        nwavTxtEncabezado.clear();
        nwavOnPresetPaginaDeTotal();
    }

    @FXML
    private void nwavOnPresetNumero() {
        nwavTxtPie.setText("{n}");
        nwavCmbAlineacionPie.setValue(NwavAlineacion.NWAV_CENTRO);
    }

    @FXML
    private void nwavOnPresetPaginaDeTotal() {
        nwavTxtPie.setText("Página {n} de {total}");
        nwavCmbAlineacionPie.setValue(NwavAlineacion.NWAV_CENTRO);
    }

    @FXML
    private void nwavOnAplicar() {
        NwavOpcionesEncabezado nwavOpciones = new NwavOpcionesEncabezado(nwavTxtEncabezado.getText(),
                nwavCmbAlineacionEncabezado.getValue(), nwavTxtPie.getText(), nwavCmbAlineacionPie.getValue(),
                nwavCmbFuente.getValue(), nwavSpnTamano.getValue(),
                NwavEstadoHerramientas.nwavConvertir(nwavSelectorColor.getValue()), nwavSpnMargen.getValue(),
                nwavSpnNumeroInicial.getValue(), nwavTxtPaginas.getText());
        nwavVisor.nwavEditar("Aplicando encabezado y pie…", nwavDoc -> nwavEdicion.nwavEncabezado(nwavDoc, nwavOpciones),
                nwavResultado -> {
                    nwavNotificador.nwavCerrarPanel();
                    nwavNotificador.nwavExito(nwavResultado.nwavDescripcion() + ". Puedes deshacerlo con Ctrl/⌘ + Z.");
                }, null);
    }

    @FXML
    private void nwavOnCerrar() {
        nwavNotificador.nwavCerrarPanel();
    }
}
