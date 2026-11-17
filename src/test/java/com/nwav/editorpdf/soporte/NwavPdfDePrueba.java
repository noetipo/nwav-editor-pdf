package com.nwav.editorpdf.soporte;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Path;

/** Genera PDF pequenos para las pruebas, sin depender de archivos externos. */
public final class NwavPdfDePrueba {

    public static final String NWAV_CLAVE_PROPIETARIO = "propietario-nwav";

    private NwavPdfDePrueba() {
    }

    /** PDF A4 con {@code nwavPaginas} paginas, cada una con su numero escrito. */
    public static Path nwavCrear(Path nwavCarpeta, String nwavNombre, int nwavPaginas) throws IOException {
        return nwavGuardar(nwavCarpeta.resolve(nwavNombre), nwavPaginas, null, 0);
    }

    /** Igual, pero la segunda pagina (si existe) va girada {@code nwavRotacion} grados. */
    public static Path nwavCrearConRotacion(Path nwavCarpeta, String nwavNombre, int nwavPaginas,
                                           int nwavRotacion) throws IOException {
        return nwavGuardar(nwavCarpeta.resolve(nwavNombre), nwavPaginas, null, nwavRotacion);
    }

    /** PDF cifrado con contrasena de usuario. */
    public static Path nwavCrearCifrado(Path nwavCarpeta, String nwavNombre, int nwavPaginas,
                                       String nwavClaveUsuario) throws IOException {
        return nwavGuardar(nwavCarpeta.resolve(nwavNombre), nwavPaginas, nwavClaveUsuario, 0);
    }

    private static Path nwavGuardar(Path nwavDestino, int nwavPaginas, String nwavClaveUsuario,
                                    int nwavRotacionSegunda) throws IOException {
        try (PDDocument nwavDocumento = new PDDocument()) {
            PDType1Font nwavFuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            for (int nwavIndice = 0; nwavIndice < nwavPaginas; nwavIndice++) {
                PDPage nwavPagina = new PDPage(PDRectangle.A4);
                if (nwavIndice == 1 && nwavRotacionSegunda != 0) {
                    nwavPagina.setRotation(nwavRotacionSegunda);
                }
                nwavDocumento.addPage(nwavPagina);
                try (PDPageContentStream nwavContenido = new PDPageContentStream(nwavDocumento, nwavPagina)) {
                    nwavContenido.beginText();
                    nwavContenido.setFont(nwavFuente, 28);
                    nwavContenido.newLineAtOffset(72, 760);
                    nwavContenido.showText("Pagina " + (nwavIndice + 1));
                    nwavContenido.endText();
                }
            }
            if (nwavClaveUsuario != null) {
                StandardProtectionPolicy nwavPolitica = new StandardProtectionPolicy(
                        NWAV_CLAVE_PROPIETARIO, nwavClaveUsuario, new AccessPermission());
                nwavPolitica.setEncryptionKeyLength(128);
                nwavDocumento.protect(nwavPolitica);
            }
            nwavDocumento.save(nwavDestino.toFile());
        }
        return nwavDestino;
    }
}
