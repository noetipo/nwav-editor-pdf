package com.nwav.editorpdf.controller;

import com.nwav.editorpdf.model.NwavInfoDocumento;
import com.nwav.editorpdf.model.NwavMetadatos;
import com.nwav.editorpdf.service.NwavDocumentoPdf;
import com.nwav.editorpdf.service.NwavEdicionService;
import com.nwav.editorpdf.service.NwavMetadatosService;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavPanel;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class NwavPanelMetadatosController implements NwavPanel {
    private static final DateTimeFormatter NWAV_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NwavVisorController nwavVisor;
    private final NwavMetadatosService nwavMetadatos;
    private final NwavEdicionService nwavEdicion;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavNotificador nwavNotificador;

    @FXML private TextField nwavTxtTitulo;
    @FXML private TextField nwavTxtAutor;
    @FXML private TextField nwavTxtAsunto;
    @FXML private TextField nwavTxtPalabrasClave;
    @FXML private Label nwavLblCreador;
    @FXML private Label nwavLblProductor;
    @FXML private Label nwavLblCreacion;
    @FXML private Label nwavLblModificacion;
    @FXML private Label nwavLblArchivo;
    @FXML private Label nwavLblSeguridad;
    @FXML private Button nwavBtnGuardar;

    private NwavMetadatos nwavOriginales;

    @Override
    public void nwavPreparar() {
        nwavBtnGuardar.setDisable(true);
        NwavDocumentoPdf nwavDocumento = nwavVisor.nwavDocumentoActual().orElse(null);
        if (nwavDocumento == null) {
            return;
        }
        nwavTareaFondo.nwavEjecutarSilencioso(() -> nwavMetadatos.nwavLeer(nwavDocumento), this::nwavMostrar);
    }

    private void nwavMostrar(NwavInfoDocumento nwavInfo) {
        nwavOriginales = nwavInfo.nwavMetadatos();
        nwavTxtTitulo.setText(Objects.toString(nwavOriginales.nwavTitulo(), ""));
        nwavTxtAutor.setText(Objects.toString(nwavOriginales.nwavAutor(), ""));
        nwavTxtAsunto.setText(Objects.toString(nwavOriginales.nwavAsunto(), ""));
        nwavTxtPalabrasClave.setText(Objects.toString(nwavOriginales.nwavPalabrasClave(), ""));
        nwavLblCreador.setText(nwavTexto(nwavOriginales.nwavCreador()));
        nwavLblProductor.setText(nwavTexto(nwavOriginales.nwavProductor()));
        nwavLblCreacion.setText(nwavFecha(nwavOriginales.nwavCreacion()));
        nwavLblModificacion.setText(nwavFecha(nwavOriginales.nwavModificacion()));
        nwavLblArchivo.setText(nwavInfo.nwavPaginas() + " páginas · PDF " + nwavInfo.nwavVersionPdf() + " · "
                + nwavTamano(nwavInfo.nwavTamanoBytes())
                + (nwavInfo.nwavCamposFormulario() > 0 ? " · " + nwavInfo.nwavCamposFormulario() + " campos de formulario" : ""));
        nwavLblSeguridad.setText((nwavInfo.nwavCifrado() ? "Protegido con contraseña" : "Sin protección")
                + (nwavInfo.nwavProteccionPendiente() == null ? "" : " · " + nwavInfo.nwavProteccionPendiente()));
        nwavBtnGuardar.setDisable(true);
    }

    @FXML
    public void initialize() {
        for (TextField nwavCampo : new TextField[]{nwavTxtTitulo, nwavTxtAutor, nwavTxtAsunto, nwavTxtPalabrasClave}) {
            nwavCampo.textProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavBtnGuardar.setDisable(!nwavHayCambios()));
        }
    }

    private boolean nwavHayCambios() {
        return nwavOriginales != null && (!nwavTxtTitulo.getText().equals(Objects.toString(nwavOriginales.nwavTitulo(), ""))
                || !nwavTxtAutor.getText().equals(Objects.toString(nwavOriginales.nwavAutor(), ""))
                || !nwavTxtAsunto.getText().equals(Objects.toString(nwavOriginales.nwavAsunto(), ""))
                || !nwavTxtPalabrasClave.getText().equals(Objects.toString(nwavOriginales.nwavPalabrasClave(), "")));
    }

    @FXML
    private void nwavOnGuardar() {
        NwavMetadatos nwavNuevos = new NwavMetadatos(nwavTxtTitulo.getText(), nwavTxtAutor.getText(),
                nwavTxtAsunto.getText(), nwavTxtPalabrasClave.getText(), null, null, null, null);
        nwavVisor.nwavEditar("Guardando propiedades…", nwavDoc -> nwavEdicion.nwavEditarMetadatos(nwavDoc, nwavNuevos),
                nwavResultado -> {
                    nwavNotificador.nwavCerrarPanel();
                    nwavNotificador.nwavExito("Propiedades actualizadas. Se guardan en el archivo con «Guardar como».");
                }, null);
    }

    @FXML
    private void nwavOnCerrar() {
        nwavNotificador.nwavCerrarPanel();
    }

    private static String nwavTexto(String nwavValor) {
        return nwavValor == null || nwavValor.isBlank() ? "—" : nwavValor;
    }

    private static String nwavFecha(LocalDateTime nwavFecha) {
        return nwavFecha == null ? "—" : NWAV_FORMATO.format(nwavFecha);
    }

    private static String nwavTamano(long nwavBytes) {
        if (nwavBytes < 0) {
            return "tamaño desconocido";
        }
        if (nwavBytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.0f KB", nwavBytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", nwavBytes / 1024.0 / 1024.0);
    }
}
