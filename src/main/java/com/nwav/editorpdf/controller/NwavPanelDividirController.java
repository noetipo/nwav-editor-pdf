package com.nwav.editorpdf.controller;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.service.NwavArchivoPdfService;
import com.nwav.editorpdf.service.NwavDocumentoPdf;
import com.nwav.editorpdf.service.NwavRangosPaginas;
import com.nwav.editorpdf.service.NwavRangosPaginas.NwavRango;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavPanel;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NwavPanelDividirController implements NwavPanel {
    private static final int NWAV_RANGOS_EN_RESUMEN = 6;

    private final NwavVisorController nwavVisor;
    private final NwavArchivoPdfService nwavArchivos;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavNotificador nwavNotificador;

    @FXML private Label nwavLblDocumento;
    @FXML private ToggleGroup nwavGrupoModo;
    @FXML private RadioButton nwavRdbRangos;
    @FXML private RadioButton nwavRdbCada;
    @FXML private TextField nwavTxtRangos;
    @FXML private Spinner<Integer> nwavSpnCada;
    @FXML private TextField nwavTxtNombreBase;
    @FXML private Label nwavLblResumen;
    @FXML private Button nwavBtnDividir;

    @FXML
    public void initialize() {
        nwavSpnCada.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10_000, 10));
        nwavTxtRangos.disableProperty().bind(nwavRdbRangos.selectedProperty().not());
        nwavSpnCada.disableProperty().bind(nwavRdbCada.selectedProperty().not());
        nwavGrupoModo.selectedToggleProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavCalcular());
        nwavTxtRangos.textProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavCalcular());
        nwavSpnCada.valueProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavCalcular());
    }

    @Override
    public void nwavPreparar() {
        nwavVisor.nwavDocumentoActual().ifPresent(nwavDocumento -> {
            nwavLblDocumento.setText(nwavDocumento.getNwavNombre() + "  ·  "
                    + nwavDocumento.getNwavNumeroPaginas() + " páginas");
            nwavTxtNombreBase.setText(NwavArchivoPdfService.nwavSinExtension(nwavDocumento.getNwavNombre()));
        });
        nwavCalcular();
    }

    private List<NwavRango> nwavCalcular() {
        NwavDocumentoPdf nwavDocumento = nwavVisor.nwavDocumentoActual().orElse(null);
        try {
            if (nwavDocumento == null) {
                throw new NwavEditorException("No hay ningún documento abierto.");
            }
            int nwavTotal = nwavDocumento.getNwavNumeroPaginas();
            List<NwavRango> nwavRangos = nwavRdbCada.isSelected()
                    ? NwavRangosPaginas.nwavCada(nwavSpnCada.getValue(), nwavTotal)
                    : NwavRangosPaginas.nwavInterpretar(nwavTxtRangos.getText(), nwavTotal);
            List<String> nwavMuestra = nwavRangos.stream().limit(NWAV_RANGOS_EN_RESUMEN)
                    .map(NwavRango::nwavDescripcion).toList();
            nwavLblResumen.setText("Se crearán " + nwavRangos.size()
                    + (nwavRangos.size() == 1 ? " archivo" : " archivos") + ": páginas "
                    + String.join(" · ", nwavMuestra) + (nwavRangos.size() > NWAV_RANGOS_EN_RESUMEN ? " …" : ""));
            nwavLblResumen.getStyleClass().remove("nwav-campo-error");
            nwavBtnDividir.setDisable(false);
            return nwavRangos;
        } catch (NwavEditorException nwavError) {
            nwavLblResumen.setText(nwavError.getMessage());
            if (!nwavLblResumen.getStyleClass().contains("nwav-campo-error")) {
                nwavLblResumen.getStyleClass().add("nwav-campo-error");
            }
            nwavBtnDividir.setDisable(true);
            return List.of();
        }
    }

    @FXML
    private void nwavOnDividir() {
        List<NwavRango> nwavRangos = nwavCalcular();
        NwavDocumentoPdf nwavDocumento = nwavVisor.nwavDocumentoActual().orElse(null);
        if (nwavRangos.isEmpty() || nwavDocumento == null) {
            return;
        }
        DirectoryChooser nwavSelector = new DirectoryChooser();
        nwavSelector.setTitle("Carpeta donde guardar las partes");
        Path nwavCarpetaDoc = nwavDocumento.getNwavRuta().getParent();
        if (nwavCarpetaDoc != null && nwavCarpetaDoc.toFile().isDirectory()) {
            nwavSelector.setInitialDirectory(nwavCarpetaDoc.toFile());
        }
        File nwavCarpeta = nwavSelector.showDialog(nwavVisor.nwavVentana());
        if (nwavCarpeta == null) {
            return;
        }
        String nwavBase = nwavTxtNombreBase.getText();
        nwavTareaFondo.nwavEjecutarConProgreso("Dividiendo «" + nwavDocumento.getNwavNombre() + "»…",
                nwavAvance -> nwavArchivos.nwavDividir(nwavDocumento, nwavRangos, nwavCarpeta.toPath(), nwavBase,
                        nwavAvance),
                nwavCreados -> {
                    nwavNotificador.nwavCerrarPanel();
                    nwavNotificador.nwavExito("Se crearon " + nwavCreados.size() + " archivos en «"
                            + nwavCarpeta.getName() + "».");
                }, null);
    }

    @FXML
    private void nwavOnCerrar() {
        nwavNotificador.nwavCerrarPanel();
    }
}
