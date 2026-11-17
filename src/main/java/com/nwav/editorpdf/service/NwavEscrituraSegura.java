package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavArchivoEnUsoException;
import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.exception.NwavSinPermisoException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class NwavEscrituraSegura {
    @FunctionalInterface
    public interface NwavEscritor {
        void nwavEscribir(Path nwavTemporal) throws IOException;
    }

    private NwavEscrituraSegura() {
    }

    public static Path nwavRutaSalida(Path nwavOriginal, Path nwavCarpeta, String nwavSufijo, String nwavExtension) {
        Path nwavDirectorio = nwavCarpeta != null ? nwavCarpeta : nwavOriginal.toAbsolutePath().getParent();
        String nwavBase = nwavSinExtension(nwavOriginal.getFileName().toString());
        String nwavNombre = nwavBase + (nwavSufijo == null || nwavSufijo.isBlank() ? "" : "_" + nwavSufijo)
                + "." + nwavExtension;
        return nwavSinColision(nwavDirectorio.resolve(nwavNombre), nwavOriginal);
    }

    public static Path nwavSinColision(Path nwavRuta, Path nwavEvitar) {
        Path nwavNormalizada = nwavRuta.toAbsolutePath().normalize();
        Path nwavProhibida = nwavEvitar == null ? null : nwavEvitar.toAbsolutePath().normalize();
        if (!Files.exists(nwavNormalizada) && !nwavNormalizada.equals(nwavProhibida)) {
            return nwavNormalizada;
        }
        String nwavNombre = nwavNormalizada.getFileName().toString();
        int nwavPunto = nwavNombre.lastIndexOf('.');
        String nwavBase = nwavPunto > 0 ? nwavNombre.substring(0, nwavPunto) : nwavNombre;
        String nwavExtension = nwavPunto > 0 ? nwavNombre.substring(nwavPunto) : "";
        for (int nwavNumero = 2; ; nwavNumero++) {
            Path nwavAlternativa = nwavNormalizada.resolveSibling(nwavBase + " (" + nwavNumero + ")" + nwavExtension);
            if (!Files.exists(nwavAlternativa) && !nwavAlternativa.equals(nwavProhibida)) {
                return nwavAlternativa;
            }
        }
    }

    public static String nwavSinExtension(String nwavNombre) {
        int nwavPunto = nwavNombre.lastIndexOf('.');
        return nwavPunto > 0 ? nwavNombre.substring(0, nwavPunto) : nwavNombre;
    }

    public static String nwavExtension(Path nwavRuta) {
        String nwavNombre = nwavRuta.getFileName().toString();
        int nwavPunto = nwavNombre.lastIndexOf('.');
        return nwavPunto > 0 ? nwavNombre.substring(nwavPunto + 1).toLowerCase(Locale.ROOT) : "";
    }

    public static Path nwavEscribir(Path nwavDestino, NwavEscritor nwavEscritor) {
        Path nwavFinal = nwavDestino.toAbsolutePath().normalize();
        Path nwavCarpeta = nwavFinal.getParent();
        String nwavNombre = String.valueOf(nwavFinal.getFileName());
        if (nwavCarpeta == null || !Files.isDirectory(nwavCarpeta)) {
            throw new NwavEditorException("La carpeta de destino de «" + nwavNombre + "» no existe.");
        }
        if (!Files.isWritable(nwavCarpeta)) {
            throw new NwavSinPermisoException(nwavCarpeta.toString());
        }
        Path nwavTemporal = null;
        try {
            nwavTemporal = Files.createTempFile(nwavCarpeta, ".nwav-guardando-", ".tmp");
            nwavEscritor.nwavEscribir(nwavTemporal);
            nwavMover(nwavTemporal, nwavFinal);
            nwavTemporal = null;
            return nwavFinal;
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, nwavNombre);
        } finally {
            if (nwavTemporal != null) {
                try {
                    Files.deleteIfExists(nwavTemporal);
                } catch (IOException nwavIgnorado) {
                }
            }
        }
    }

    public static Path nwavGuardarPdf(PDDocument nwavPd, Path nwavDestino) {
        return nwavEscribir(nwavDestino, nwavTemporal -> nwavPd.save(nwavTemporal.toFile(),
                CompressParameters.DEFAULT_COMPRESSION));
    }

    public static void nwavMantenerProteccion(PDDocument nwavPd, String nwavContrasenaUsuario) {
        if (!nwavPd.isEncrypted()) {
            return;
        }
        AccessPermission nwavActuales = nwavPd.getCurrentAccessPermission();
        String nwavUsuario = nwavContrasenaUsuario == null ? "" : nwavContrasenaUsuario;
        String nwavPropietario = nwavActuales.isOwnerPermission() && !nwavUsuario.isEmpty()
                ? nwavUsuario : java.util.UUID.randomUUID().toString();
        StandardProtectionPolicy nwavPolitica = new StandardProtectionPolicy(nwavPropietario, nwavUsuario,
                new AccessPermission(nwavActuales.getPermissionBytes()));
        int nwavBits = nwavPd.getEncryption() == null ? 128 : nwavPd.getEncryption().getLength();
        nwavPolitica.setEncryptionKeyLength(nwavBits >= 256 ? 256 : 128);
        try {
            nwavPd.protect(nwavPolitica);
        } catch (IOException nwavError) {
            throw new NwavEditorException("No se pudo mantener la protección del documento.", nwavError);
        }
    }

    private static void nwavMover(Path nwavTemporal, Path nwavFinal) throws IOException {
        try {
            Files.move(nwavTemporal, nwavFinal, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException nwavError) {
            Files.move(nwavTemporal, nwavFinal, StandardCopyOption.REPLACE_EXISTING);
        } catch (FileSystemException nwavError) {
            if (NwavTraductorErrores.nwavEsArchivoEnUso(nwavError.getReason())) {
                throw new NwavArchivoEnUsoException(String.valueOf(nwavFinal.getFileName()), nwavError);
            }
            throw nwavError;
        }
    }
}
