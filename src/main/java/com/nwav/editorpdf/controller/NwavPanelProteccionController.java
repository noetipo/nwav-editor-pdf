package com.nwav.editorpdf.controller;

import com.nwav.editorpdf.exception.NwavContrasenaIncorrectaException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.model.NwavOpcionesProteccion;
import com.nwav.editorpdf.service.NwavDocumentoPdf;
import com.nwav.editorpdf.service.NwavEdicionService;
import com.nwav.editorpdf.service.NwavSeguridadService;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavPanel;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NwavPanelProteccionController implements NwavPanel {
    private final NwavVisorController nwavVisor;
    private final NwavEdicionService nwavEdicion;
    private final NwavSeguridadService nwavSeguridad;
    private final NwavNotificador nwavNotificador;

    @FXML private Label nwavLblEstado;
    @FXML private PasswordField nwavPwdUsuario;
    @FXML private PasswordField nwavPwdUsuarioRepetir;
    @FXML private PasswordField nwavPwdPropietario;
    @FXML private PasswordField nwavPwdPropietarioRepetir;
    @FXML private VBox nwavSeccionPermisos;
    @FXML private CheckBox nwavChkImprimir;
    @FXML private CheckBox nwavChkCopiar;
    @FXML private CheckBox nwavChkModificar;
    @FXML private CheckBox nwavChkAnotar;
    @FXML private CheckBox nwavChkFormularios;
    @FXML private Label nwavLblErrorProteger;
    @FXML private VBox nwavSeccionQuitar;
    @FXML private VBox nwavSeccionPropietarioQuitar;
    @FXML private PasswordField nwavPwdQuitar;
    @FXML private Button nwavBtnQuitar;

    @FXML
    public void initialize() {
        nwavSeccionPermisos.disableProperty().bind(nwavPwdPropietario.textProperty().isEmpty());
        for (CheckBox nwavPermiso : List.of(nwavChkImprimir, nwavChkCopiar, nwavChkModificar, nwavChkAnotar,
                nwavChkFormularios)) {
            nwavPermiso.setSelected(true);
        }
        nwavChkModificar.setSelected(false);
        nwavBtnQuitar.disableProperty().bind(Bindings.createBooleanBinding(
                () -> nwavSeccionPropietarioQuitar.isVisible() && nwavPwdQuitar.getText().isEmpty(),
                nwavPwdQuitar.textProperty(), nwavSeccionPropietarioQuitar.visibleProperty()));
    }

    @Override
    public void nwavPreparar() {
        for (PasswordField nwavCampo : List.of(nwavPwdUsuario, nwavPwdUsuarioRepetir, nwavPwdPropietario,
                nwavPwdPropietarioRepetir, nwavPwdQuitar)) {
            nwavCampo.clear();
        }
        nwavLblErrorProteger.setText("");
        NwavDocumentoPdf nwavDocumento = nwavVisor.nwavDocumentoActual().orElse(null);
        if (nwavDocumento == null) {
            return;
        }
        boolean nwavCifrado = nwavDocumento.isNwavCifrado();
        nwavLblEstado.setText((nwavCifrado ? "El documento está protegido." : "El documento no está protegido.")
                + (nwavDocumento.getNwavProteccionPendiente() == null ? ""
                : " Pendiente: " + nwavDocumento.getNwavProteccionPendiente().nwavDescripcion().toLowerCase() + "."));
        nwavSeccionQuitar.setVisible(nwavCifrado);
        nwavSeccionQuitar.setManaged(nwavCifrado);
        boolean nwavPideClave = nwavCifrado && !nwavSeguridad.nwavAbiertoComoPropietario(nwavDocumento);
        nwavSeccionPropietarioQuitar.setVisible(nwavPideClave);
        nwavSeccionPropietarioQuitar.setManaged(nwavPideClave);
    }

    @FXML
    private void nwavOnProteger() {
        String nwavUsuario = nwavPwdUsuario.getText();
        String nwavPropietario = nwavPwdPropietario.getText();
        if (!nwavUsuario.equals(nwavPwdUsuarioRepetir.getText())) {
            nwavLblErrorProteger.setText("Las contraseñas para abrir no coinciden.");
            return;
        }
        if (!nwavPropietario.equals(nwavPwdPropietarioRepetir.getText())) {
            nwavLblErrorProteger.setText("Las contraseñas de permisos no coinciden.");
            return;
        }
        nwavLblErrorProteger.setText("");
        NwavOpcionesProteccion nwavOpciones = new NwavOpcionesProteccion(nwavUsuario, nwavPropietario,
                nwavChkImprimir.isSelected(), nwavChkCopiar.isSelected(), nwavChkModificar.isSelected(),
                nwavChkAnotar.isSelected(), nwavChkFormularios.isSelected());
        nwavVisor.nwavEditar("Preparando protección…", nwavDoc -> nwavEdicion.nwavProteger(nwavDoc, nwavOpciones),
                nwavResultado -> nwavTerminar("Se protegerá al guardar. Usa «Guardar como» para crear la copia protegida."),
                nwavError -> nwavLblErrorProteger.setText(NwavTraductorErrores.nwavMensaje(nwavError)));
    }

    @FXML
    private void nwavOnQuitar() {
        String nwavClave = nwavPwdQuitar.getText();
        nwavVisor.nwavEditar("Comprobando contraseña…", nwavDoc -> nwavEdicion.nwavQuitarProteccion(nwavDoc, nwavClave),
                nwavResultado -> nwavTerminar("Se quitará la protección al guardar. Usa «Guardar como»."),
                nwavError -> nwavNotificador.nwavError(nwavError instanceof NwavContrasenaIncorrectaException
                        ? "La contraseña de permisos no es correcta."
                        : NwavTraductorErrores.nwavMensaje(nwavError)));
    }

    private void nwavTerminar(String nwavMensaje) {
        nwavNotificador.nwavCerrarPanel();
        nwavNotificador.nwavExito(nwavMensaje);
    }

    @FXML
    private void nwavOnCerrar() {
        nwavNotificador.nwavCerrarPanel();
    }
}
