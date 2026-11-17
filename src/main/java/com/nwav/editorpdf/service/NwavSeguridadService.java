package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavContrasenaIncorrectaException;
import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.model.NwavOpcionesProteccion;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.comando.NwavComando;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class NwavSeguridadService {
    public static final int NWAV_LARGO_MINIMO = 4;

    public NwavComando nwavComandoProteger(NwavDocumentoPdf nwavDocumento, NwavOpcionesProteccion nwavOpciones) {
        if (nwavOpciones.nwavUsuario() == null || nwavOpciones.nwavUsuario().length() < NWAV_LARGO_MINIMO) {
            throw new NwavEditorException("La contraseña para abrir debe tener al menos " + NWAV_LARGO_MINIMO + " caracteres.");
        }
        if (nwavOpciones.nwavTienePropietario() && nwavOpciones.nwavPropietario().equals(nwavOpciones.nwavUsuario())) {
            throw new NwavEditorException("La contraseña de permisos debe ser distinta de la de apertura.");
        }
        return new NwavComandoProteccion(nwavDocumento, new NwavCambioProteccion(nwavOpciones),
                NwavTipoOperacion.NWAV_PROTEGER, "Proteger con contraseña");
    }

    public NwavComando nwavComandoQuitarProteccion(NwavDocumentoPdf nwavDocumento, String nwavContrasenaPropietario) {
        if (!nwavDocumento.isNwavCifrado()) {
            throw new NwavEditorException("El documento no está protegido.");
        }
        if (!nwavEsPropietario(nwavDocumento, nwavContrasenaPropietario)) {
            throw new NwavContrasenaIncorrectaException(nwavDocumento.getNwavNombre());
        }
        return new NwavComandoProteccion(nwavDocumento, NwavCambioProteccion.NWAV_QUITAR,
                NwavTipoOperacion.NWAV_QUITAR_PROTECCION, "Quitar protección");
    }

    public boolean nwavAbiertoComoPropietario(NwavDocumentoPdf nwavDocumento) {
        synchronized (nwavDocumento) {
            nwavDocumento.nwavVerificarAbierto();
            PDDocument nwavPd = nwavDocumento.nwavPdDocument();
            return !nwavPd.isEncrypted() || nwavPd.getCurrentAccessPermission().isOwnerPermission();
        }
    }

    private boolean nwavEsPropietario(NwavDocumentoPdf nwavDocumento, String nwavContrasena) {
        if (nwavAbiertoComoPropietario(nwavDocumento)) {
            return true;
        }
        if (nwavContrasena == null || nwavContrasena.isEmpty()) {
            return false;
        }
        try (PDDocument nwavPrueba = Loader.loadPDF(nwavDocumento.getNwavRuta().toFile(), nwavContrasena)) {
            return nwavPrueba.getCurrentAccessPermission().isOwnerPermission();
        } catch (InvalidPasswordException nwavError) {
            return false;
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, nwavDocumento.getNwavNombre());
        }
    }

    static StandardProtectionPolicy nwavPolitica(NwavOpcionesProteccion nwavOpciones) {
        AccessPermission nwavPermisos = new AccessPermission();
        String nwavPropietario = nwavOpciones.nwavUsuario();
        if (nwavOpciones.nwavTienePropietario()) {
            nwavPropietario = nwavOpciones.nwavPropietario();
            nwavPermisos.setCanPrint(nwavOpciones.nwavImprimir());
            nwavPermisos.setCanPrintFaithful(nwavOpciones.nwavImprimir());
            nwavPermisos.setCanExtractContent(nwavOpciones.nwavCopiar());
            nwavPermisos.setCanExtractForAccessibility(true);
            nwavPermisos.setCanModify(nwavOpciones.nwavModificar());
            nwavPermisos.setCanAssembleDocument(nwavOpciones.nwavModificar());
            nwavPermisos.setCanModifyAnnotations(nwavOpciones.nwavAnotar());
            nwavPermisos.setCanFillInForm(nwavOpciones.nwavRellenarFormularios() || nwavOpciones.nwavAnotar());
        }
        StandardProtectionPolicy nwavPolitica = new StandardProtectionPolicy(nwavPropietario, nwavOpciones.nwavUsuario(),
                nwavPermisos);
        nwavPolitica.setEncryptionKeyLength(256);
        return nwavPolitica;
    }

    private static final class NwavComandoProteccion implements NwavComando {
        private final NwavDocumentoPdf nwavDocumento;
        private final NwavCambioProteccion nwavNuevo;
        private final NwavTipoOperacion nwavTipo;
        private final String nwavDescripcion;
        private NwavCambioProteccion nwavAnterior;

        private NwavComandoProteccion(NwavDocumentoPdf nwavDocumento, NwavCambioProteccion nwavNuevo,
                                      NwavTipoOperacion nwavTipo, String nwavDescripcion) {
            this.nwavDocumento = nwavDocumento;
            this.nwavNuevo = nwavNuevo;
            this.nwavTipo = nwavTipo;
            this.nwavDescripcion = nwavDescripcion;
        }

        @Override
        public void nwavEjecutar() {
            nwavAnterior = nwavDocumento.getNwavProteccionPendiente();
            nwavDocumento.nwavEstablecerProteccionPendiente(nwavNuevo);
        }

        @Override
        public void nwavDeshacer() {
            nwavDocumento.nwavEstablecerProteccionPendiente(nwavAnterior);
        }

        @Override
        public NwavTipoOperacion nwavTipo() {
            return nwavTipo;
        }

        @Override
        public String nwavDescripcion() {
            return nwavDescripcion + " (se aplica al guardar)";
        }

        @Override
        public boolean nwavCambiaEstructura() {
            return false;
        }
    }
}
