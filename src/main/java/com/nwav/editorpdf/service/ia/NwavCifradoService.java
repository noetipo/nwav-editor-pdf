package com.nwav.editorpdf.service.ia;

import com.nwav.editorpdf.exception.NwavEditorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra secretos (la clave de la API de IA) con AES-256-GCM. La clave de cifrado es aleatoria,
 * vive en un archivo aparte de la base de datos ({@code nwav_clave_local.bin}, solo legible
 * por el usuario), de modo que copiar la base de datos no revela el secreto.
 */
@Service
public class NwavCifradoService {

    private static final String NWAV_PREFIJO = "nwav1:";
    private static final int NWAV_BYTES_IV = 12;

    private final Path nwavArchivoClave;
    private SecretKeySpec nwavClave;

    public NwavCifradoService(@Value("${nwav.datos.carpeta}") String nwavCarpetaDatos) {
        this.nwavArchivoClave = Path.of(nwavCarpetaDatos).resolve("nwav_clave_local.bin");
    }

    public String nwavCifrar(String nwavTexto) {
        try {
            byte[] nwavIv = new byte[NWAV_BYTES_IV];
            new SecureRandom().nextBytes(nwavIv);
            Cipher nwavCifra = Cipher.getInstance("AES/GCM/NoPadding");
            nwavCifra.init(Cipher.ENCRYPT_MODE, nwavClave(), new GCMParameterSpec(128, nwavIv));
            byte[] nwavCifrado = nwavCifra.doFinal(nwavTexto.getBytes(StandardCharsets.UTF_8));
            byte[] nwavTodo = new byte[nwavIv.length + nwavCifrado.length];
            System.arraycopy(nwavIv, 0, nwavTodo, 0, nwavIv.length);
            System.arraycopy(nwavCifrado, 0, nwavTodo, nwavIv.length, nwavCifrado.length);
            return NWAV_PREFIJO + Base64.getEncoder().encodeToString(nwavTodo);
        } catch (GeneralSecurityException nwavError) {
            throw new NwavEditorException("No se pudo cifrar la clave.", nwavError);
        }
    }

    public String nwavDescifrar(String nwavGuardado) {
        if (nwavGuardado == null || !nwavGuardado.startsWith(NWAV_PREFIJO)) {
            throw new NwavEditorException("La clave guardada no tiene un formato válido. Vuelve a escribirla en Preferencias.");
        }
        try {
            byte[] nwavTodo = Base64.getDecoder().decode(nwavGuardado.substring(NWAV_PREFIJO.length()));
            Cipher nwavCifra = Cipher.getInstance("AES/GCM/NoPadding");
            nwavCifra.init(Cipher.DECRYPT_MODE, nwavClave(), new GCMParameterSpec(128, nwavTodo, 0, NWAV_BYTES_IV));
            return new String(nwavCifra.doFinal(nwavTodo, NWAV_BYTES_IV, nwavTodo.length - NWAV_BYTES_IV),
                    StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException nwavError) {
            throw new NwavEditorException("No se pudo descifrar la clave guardada (¿se borró el archivo de clave local?). "
                    + "Vuelve a escribirla en Preferencias.", nwavError);
        }
    }

    private synchronized SecretKeySpec nwavClave() {
        if (nwavClave != null) {
            return nwavClave;
        }
        try {
            byte[] nwavBytes;
            if (Files.isRegularFile(nwavArchivoClave)) {
                nwavBytes = Files.readAllBytes(nwavArchivoClave);
            } else {
                nwavBytes = new byte[32];
                new SecureRandom().nextBytes(nwavBytes);
                Files.createDirectories(nwavArchivoClave.getParent());
                Files.write(nwavArchivoClave, nwavBytes);
                try {
                    Files.setPosixFilePermissions(nwavArchivoClave, PosixFilePermissions.fromString("rw-------"));
                } catch (UnsupportedOperationException nwavError) {
                    // Windows: queda en la carpeta del usuario
                }
            }
            if (nwavBytes.length != 32) {
                throw new NwavEditorException("El archivo de clave local está dañado: " + nwavArchivoClave);
            }
            nwavClave = new SecretKeySpec(nwavBytes, "AES");
            return nwavClave;
        } catch (IOException nwavError) {
            throw new NwavEditorException("No se pudo leer ni crear la clave local de cifrado.", nwavError);
        }
    }
}
