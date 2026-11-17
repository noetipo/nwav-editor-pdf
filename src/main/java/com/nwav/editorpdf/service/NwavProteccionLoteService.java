package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.model.NwavOpcionesProteccion;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class NwavProteccionLoteService {
    private final NwavPdfDocumentoService nwavDocumentos;

    public Path nwavProteger(Path nwavEntrada, String nwavContrasenaActual, NwavOpcionesProteccion nwavOpciones,
                             Path nwavDestino) {
        if (nwavOpciones.nwavUsuario() == null || nwavOpciones.nwavUsuario().length() < NwavSeguridadService.NWAV_LARGO_MINIMO) {
            throw new NwavEditorException("La contraseña debe tener al menos " + NwavSeguridadService.NWAV_LARGO_MINIMO
                    + " caracteres.");
        }
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasenaActual)) {
            nwavExigirPropietario(nwavPd);
            nwavPd.setAllSecurityToBeRemoved(true);
            nwavPd.protect(NwavSeguridadService.nwavPolitica(nwavOpciones));
            return NwavEscrituraSegura.nwavGuardarPdf(nwavPd, nwavDestino);
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    public Path nwavDesbloquear(Path nwavEntrada, String nwavContrasena, Path nwavDestino) {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            if (!nwavPd.isEncrypted()) {
                throw new NwavEditorException("«" + nwavEntrada.getFileName() + "» no tiene contraseña.");
            }
            nwavExigirPropietario(nwavPd);
            nwavPd.setAllSecurityToBeRemoved(true);
            return NwavEscrituraSegura.nwavGuardarPdf(nwavPd, nwavDestino);
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    private static void nwavExigirPropietario(PDDocument nwavPd) {
        if (nwavPd.isEncrypted() && !nwavPd.getCurrentAccessPermission().isOwnerPermission()) {
            throw new NwavEditorException("Esa contraseña solo permite abrir el documento. Para cambiar o quitar la "
                    + "protección hace falta la contraseña de propietario (la de permisos).");
        }
    }
}
