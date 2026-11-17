package com.nwav.editorpdf.controller;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.service.NwavArchivoPdfService;
import com.nwav.editorpdf.service.NwavDocumentoPdf;
import com.nwav.editorpdf.service.NwavRangosPaginas;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavPanel;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/** Panel "Extraer páginas": copia una seleccion de paginas a un PDF nuevo. */
@Component
@RequiredArgsConstructor
public class NwavPanelExtraerController implements NwavPanel {

    private final NwavVisorController nwavVisor;
    private final NwavArchivoPdfService nwavArchivos;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavNotificador nwavNotificador;

    @FXML private Label nwavLblDocumento;
    @FXML private TextField nwavTxtPaginas;
    @FXML private Label nwavLblError;
    @FXML private CheckBox nwavChkAbrir;
    @FXML private Button nwavBtnExtraer;

    @FXML
    public void initialize() {
        nwavTxtPaginas.textProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavValidar());
        nwavTxtPaginas.setOnAction(nwavEvento -> nwavOnExtraer());
    }

    @Override
    public void nwavPreparar() {
        nwavVisor.nwavDocumentoActual().ifPresent(nwavDocumento -> {
            nwavLblDocumento.setText(nwavDocumento.getNwavNombre() + "  ·  "
                    + nwavDocumento.getNwavNumeroPaginas() + " páginas");
            nwavTxtPaginas.setText(NwavRangosPaginas.nwavDescribir(nwavVisor.nwavSeleccionPaginas()));
        });
        nwavValidar();
    }

    private List<Integer> nwavValidar() {
        NwavDocumentoPdf nwavDocumento = nwavVisor.nwavDocumentoActual().orElse(null);
        try {
            if (nwavDocumento == null) {
                throw new NwavEditorException("No hay ningún documento abierto.");
            }
            List<Integer> nwavIndices = NwavRangosPaginas.nwavIndices(nwavTxtPaginas.getText(),
                    nwavDocumento.getNwavNumeroPaginas());
            nwavLblError.setText(nwavIndices.size() == 1 ? "Se extraerá 1 página."
                    : "Se extraerán " + nwavIndices.size() + " páginas.");
            nwavLblError.getStyleClass().remove("nwav-campo-error");
            nwavBtnExtraer.setDisable(false);
            return nwavIndices;
        } catch (NwavEditorException nwavError) {
            nwavLblError.setText(nwavError.getMessage());
            if (!nwavLblError.getStyleClass().contains("nwav-campo-error")) {
                nwavLblError.getStyleClass().add("nwav-campo-error");
            }
            nwavBtnExtraer.setDisable(true);
            return List.of();
        }
    }

    @FXML
    private void nwavOnExtraer() {
        List<Integer> nwavIndices = nwavValidar();
        NwavDocumentoPdf nwavDocumento = nwavVisor.nwavDocumentoActual().orElse(null);
        if (nwavIndices.isEmpty() || nwavDocumento == null) {
            return;
        }
        FileChooser nwavSelector = new FileChooser();
        nwavSelector.setTitle("Guardar páginas extraídas");
        nwavSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        Path nwavCarpeta = nwavDocumento.getNwavRuta().getParent();
        if (nwavCarpeta != null && nwavCarpeta.toFile().isDirectory()) {
            nwavSelector.setInitialDirectory(nwavCarpeta.toFile());
        }
        nwavSelector.setInitialFileName(NwavArchivoPdfService.nwavSinExtension(nwavDocumento.getNwavNombre())
                + " (págs. " + NwavRangosPaginas.nwavDescribir(nwavIndices) + ").pdf");
        File nwavElegido = nwavSelector.showSaveDialog(nwavVisor.nwavVentana());
        if (nwavElegido == null) {
            return;
        }
        Path nwavDestino = NwavVisorController.nwavConExtensionPdf(nwavElegido.toPath());
        boolean nwavAbrir = nwavChkAbrir.isSelected();
        nwavTareaFondo.nwavEjecutar("Extrayendo páginas…",
                () -> nwavArchivos.nwavExtraer(nwavDocumento, nwavIndices, nwavDestino),
                nwavRuta -> {
                    nwavNotificador.nwavCerrarPanel();
                    nwavNotificador.nwavExito("Se creó «" + nwavRuta.getFileName() + "» con "
                            + (nwavIndices.size() == 1 ? "1 página." : nwavIndices.size() + " páginas."));
                    if (nwavAbrir) {
                        nwavVisor.nwavAbrirArchivo(nwavRuta);
                    }
                }, null);
    }

    @FXML
    private void nwavOnCerrar() {
        nwavNotificador.nwavCerrarPanel();
    }
}
