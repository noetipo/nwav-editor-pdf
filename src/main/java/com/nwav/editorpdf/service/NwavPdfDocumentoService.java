package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavArchivoEnUsoException;
import com.nwav.editorpdf.exception.NwavArchivoNoEncontradoException;
import com.nwav.editorpdf.exception.NwavContrasenaIncorrectaException;
import com.nwav.editorpdf.exception.NwavContrasenaRequeridaException;
import com.nwav.editorpdf.exception.NwavPdfCorruptoException;
import com.nwav.editorpdf.exception.NwavSinPermisoException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Service
@Slf4j
public class NwavPdfDocumentoService {
    public NwavDocumentoPdf nwavAbrir(Path nwavRuta, String nwavContrasena) {
        Path nwavAbsoluta = nwavRuta.toAbsolutePath().normalize();
        String nwavNombre = String.valueOf(nwavAbsoluta.getFileName());
        nwavValidarLectura(nwavAbsoluta, nwavNombre);

        PDDocument nwavDocumento = nwavCargar(nwavAbsoluta, nwavNombre, nwavContrasena);
        try {
            if (nwavDocumento.getNumberOfPages() == 0) {
                throw new NwavPdfCorruptoException(nwavNombre);
            }
            NwavDocumentoPdf nwavResultado = new NwavDocumentoPdf(nwavAbsoluta, nwavDocumento,
                    nwavContrasena == null || nwavContrasena.isEmpty() ? null : nwavContrasena);
            nwavLog.info("Abierto {} ({} paginas{})", nwavAbsoluta, nwavResultado.getNwavNumeroPaginas(),
                    nwavResultado.isNwavCifrado() ? ", cifrado" : "");
            return nwavResultado;
        } catch (RuntimeException nwavError) {
            nwavCerrarSilencioso(nwavDocumento);
            throw nwavError instanceof NwavPdfCorruptoException
                    ? nwavError
                    : new NwavPdfCorruptoException(nwavNombre, nwavError);
        }
    }

    public PDDocument nwavCargarDocumento(Path nwavRuta, String nwavContrasena) {
        Path nwavAbsoluta = nwavRuta.toAbsolutePath().normalize();
        String nwavNombre = String.valueOf(nwavAbsoluta.getFileName());
        nwavValidarLectura(nwavAbsoluta, nwavNombre);
        PDDocument nwavDocumento = nwavCargar(nwavAbsoluta, nwavNombre, nwavContrasena);
        if (nwavDocumento.getNumberOfPages() == 0) {
            nwavCerrarSilencioso(nwavDocumento);
            throw new NwavPdfCorruptoException(nwavNombre);
        }
        return nwavDocumento;
    }

    public boolean nwavRequiereContrasena(Path nwavRuta) {
        try (NwavDocumentoPdf nwavDocumento = nwavAbrir(nwavRuta, null)) {
            return false;
        } catch (NwavContrasenaRequeridaException nwavError) {
            return true;
        }
    }

    private static void nwavValidarLectura(Path nwavRuta, String nwavNombre) {
        if (!Files.exists(nwavRuta)) {
            throw new NwavArchivoNoEncontradoException(nwavNombre);
        }
        if (!Files.isRegularFile(nwavRuta)) {
            throw new NwavPdfCorruptoException(nwavNombre);
        }
        if (!Files.isReadable(nwavRuta)) {
            throw new NwavSinPermisoException(nwavNombre);
        }
    }

    private static PDDocument nwavCargar(Path nwavRuta, String nwavNombre, String nwavContrasena) {
        boolean nwavSinContrasena = nwavContrasena == null || nwavContrasena.isEmpty();
        try {
            return Loader.loadPDF(nwavRuta.toFile(), nwavSinContrasena ? "" : nwavContrasena);
        } catch (InvalidPasswordException nwavError) {
            throw nwavSinContrasena
                    ? new NwavContrasenaRequeridaException(nwavNombre, nwavError)
                    : new NwavContrasenaIncorrectaException(nwavNombre, nwavError);
        } catch (AccessDeniedException nwavError) {
            throw new NwavSinPermisoException(nwavNombre, nwavError);
        } catch (FileNotFoundException nwavError) {
            String nwavMotivo = String.valueOf(nwavError.getMessage()).toLowerCase(Locale.ROOT);
            if (NwavTraductorErrores.nwavEsArchivoEnUso(nwavMotivo)) {
                throw new NwavArchivoEnUsoException(nwavNombre, nwavError);
            }
            if (nwavMotivo.contains("denied") || nwavMotivo.contains("denegado")) {
                throw new NwavSinPermisoException(nwavNombre, nwavError);
            }
            throw new NwavArchivoNoEncontradoException(nwavNombre, nwavError);
        } catch (IOException | RuntimeException nwavError) {
            nwavLog.debug("No se pudo leer {}", nwavRuta, nwavError);
            throw new NwavPdfCorruptoException(nwavNombre, nwavError);
        }
    }

    private static void nwavCerrarSilencioso(PDDocument nwavDocumento) {
        try {
            nwavDocumento.close();
        } catch (IOException nwavError) {
            nwavLog.debug("Error al cerrar documento descartado", nwavError);
        }
    }
}
