package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavArchivoNoEncontradoException;
import com.nwav.editorpdf.exception.NwavContrasenaIncorrectaException;
import com.nwav.editorpdf.exception.NwavContrasenaRequeridaException;
import com.nwav.editorpdf.exception.NwavPdfCorruptoException;
import com.nwav.editorpdf.soporte.NwavPdfDePrueba;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NwavPdfDocumentoServiceTest {
    private final NwavPdfDocumentoService nwavServicio = new NwavPdfDocumentoService();

    @TempDir
    Path nwavCarpeta;

    @Test
    void nwavAbreUnPdfYCuentaSusPaginas() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "tres.pdf", 3);

        try (NwavDocumentoPdf nwavDocumento = nwavServicio.nwavAbrir(nwavPdf, null)) {
            assertThat(nwavDocumento.getNwavNumeroPaginas()).isEqualTo(3);
            assertThat(nwavDocumento.getNwavNombre()).isEqualTo("tres.pdf");
            assertThat(nwavDocumento.isNwavCifrado()).isFalse();
            NwavDocumentoPdf.NwavTamanoPagina nwavA4 = nwavDocumento.nwavTamano(0);
            assertThat(nwavA4.nwavAncho()).isCloseTo(595.27f, org.assertj.core.data.Offset.offset(0.1f));
            assertThat(nwavA4.nwavAlto()).isCloseTo(841.89f, org.assertj.core.data.Offset.offset(0.1f));
        }
    }

    @Test
    void nwavLaPaginaGiradaIntercambiaAnchoYAlto() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrearConRotacion(nwavCarpeta, "girado.pdf", 2, 90);

        try (NwavDocumentoPdf nwavDocumento = nwavServicio.nwavAbrir(nwavPdf, null)) {
            NwavDocumentoPdf.NwavTamanoPagina nwavGirada = nwavDocumento.nwavTamano(1);
            assertThat(nwavGirada.nwavAncho()).isGreaterThan(nwavGirada.nwavAlto());
        }
    }

    @Test
    void nwavPdfCifradoSinContrasenaPideContrasena() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrearCifrado(nwavCarpeta, "cifrado.pdf", 1, "secreta");

        assertThatThrownBy(() -> nwavServicio.nwavAbrir(nwavPdf, null))
                .isInstanceOf(NwavContrasenaRequeridaException.class)
                .hasMessageContaining("cifrado.pdf");
        assertThat(nwavServicio.nwavRequiereContrasena(nwavPdf)).isTrue();
    }

    @Test
    void nwavContrasenaIncorrectaSeDistingueDeLaFaltante() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrearCifrado(nwavCarpeta, "cifrado.pdf", 1, "secreta");

        assertThatThrownBy(() -> nwavServicio.nwavAbrir(nwavPdf, "otra"))
                .isInstanceOf(NwavContrasenaIncorrectaException.class)
                .hasMessageContaining("no es correcta");
    }

    @Test
    void nwavContrasenaCorrectaAbreElDocumento() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrearCifrado(nwavCarpeta, "cifrado.pdf", 2, "secreta");

        try (NwavDocumentoPdf nwavDocumento = nwavServicio.nwavAbrir(nwavPdf, "secreta")) {
            assertThat(nwavDocumento.getNwavNumeroPaginas()).isEqualTo(2);
            assertThat(nwavDocumento.isNwavCifrado()).isTrue();
        }
    }

    @Test
    void nwavPdfSinContrasenaNoLaRequiere() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "libre.pdf", 1);
        assertThat(nwavServicio.nwavRequiereContrasena(nwavPdf)).isFalse();
    }

    @Test
    void nwavArchivoCorruptoDaUnErrorClaro() throws Exception {
        Path nwavBasura = nwavCarpeta.resolve("roto.pdf");
        Files.write(nwavBasura, "esto no es un pdf %%EOF".getBytes());

        assertThatThrownBy(() -> nwavServicio.nwavAbrir(nwavBasura, null))
                .isInstanceOf(NwavPdfCorruptoException.class)
                .hasMessageContaining("no es un PDF válido");
    }

    @Test
    void nwavArchivoVacioSeConsideraCorrupto() throws Exception {
        Path nwavVacio = Files.createFile(nwavCarpeta.resolve("vacio.pdf"));

        assertThatThrownBy(() -> nwavServicio.nwavAbrir(nwavVacio, null))
                .isInstanceOf(NwavPdfCorruptoException.class);
    }

    @Test
    void nwavPdfTruncadoSeConsideraCorrupto() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "completo.pdf", 2);
        byte[] nwavBytes = Files.readAllBytes(nwavPdf);
        Path nwavTruncado = nwavCarpeta.resolve("truncado.pdf");
        Files.write(nwavTruncado, java.util.Arrays.copyOf(nwavBytes, 40));

        assertThatThrownBy(() -> nwavServicio.nwavAbrir(nwavTruncado, null))
                .isInstanceOf(NwavPdfCorruptoException.class);
    }

    @Test
    void nwavArchivoInexistenteSeInforma() {
        Path nwavFalta = nwavCarpeta.resolve("no-existe.pdf");

        assertThatThrownBy(() -> nwavServicio.nwavAbrir(nwavFalta, null))
                .isInstanceOf(NwavArchivoNoEncontradoException.class)
                .hasMessageContaining("no-existe.pdf");
    }

    @Test
    void nwavCerrarEsIdempotenteYMarcaElDocumento() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "uno.pdf", 1);
        NwavDocumentoPdf nwavDocumento = nwavServicio.nwavAbrir(nwavPdf, null);

        nwavDocumento.close();
        nwavDocumento.close();

        assertThat(nwavDocumento.isNwavCerrado()).isTrue();
    }
}
