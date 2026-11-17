package com.nwav.editorpdf.service.ia;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.repository.NwavConfiguracionRepository;
import com.nwav.editorpdf.service.NwavAvance;
import com.nwav.editorpdf.service.NwavCancelacion;
import com.nwav.editorpdf.service.NwavConversionAPdfService;
import com.nwav.editorpdf.service.NwavConversionDesdePdfService;
import com.nwav.editorpdf.service.NwavPdfDocumentoService;
import com.nwav.editorpdf.service.herramienta.NwavDependenciasService;
import com.nwav.editorpdf.soporte.NwavPdfConImagenes;
import com.nwav.editorpdf.soporte.NwavPdfDePrueba;
import com.nwav.editorpdf.soporte.NwavPruebaConBaseDatos;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Fase G: IA opcional (sin llamadas reales: el cliente se simula). */
class NwavIaTest extends NwavPruebaConBaseDatos {

    @Autowired private NwavConfiguracionIaService nwavConfiguracionIa;
    @Autowired private NwavCifradoService nwavCifrado;
    @Autowired private NwavConfiguracionRepository nwavRepositorio;
    @Autowired private NwavClienteClaude nwavClaude;

    @TempDir
    Path nwavCarpeta;

    @AfterEach
    void nwavLimpiar() {
        nwavConfiguracionIa.nwavBorrarClave();
        nwavConfiguracionIa.nwavGuardar(false, null, "");
    }

    /** Cliente falso que registra lo que se le envia. */
    private static final class NwavClienteFalso implements NwavClienteIa {
        final List<String> nwavSistemas = new ArrayList<>();
        final List<String> nwavTextos = new ArrayList<>();
        final List<byte[]> nwavPdfs = new ArrayList<>();
        private final java.util.function.UnaryOperator<String> nwavRespuesta;

        NwavClienteFalso(java.util.function.UnaryOperator<String> nwavRespuesta) {
            this.nwavRespuesta = nwavRespuesta;
        }

        @Override
        public String nwavGenerar(String nwavSistema, byte[] nwavPdf, String nwavTexto, NwavCancelacion nwavCancelacion) {
            nwavSistemas.add(nwavSistema);
            nwavPdfs.add(nwavPdf);
            nwavTextos.add(nwavTexto);
            return nwavRespuesta.apply(nwavTexto);
        }
    }

    private NwavIaService nwavServicio(NwavClienteIa nwavCliente) {
        NwavPdfDocumentoService nwavDocumentos = new NwavPdfDocumentoService();
        return new NwavIaService(nwavCliente, nwavDocumentos, new NwavConversionDesdePdfService(nwavDocumentos),
                new NwavConversionAPdfService(Mockito.mock(NwavDependenciasService.class)));
    }

    // ============================================================== clave y estado

    @Test
    void nwavLaClaveSeGuardaCifradaYLaIaEstaDesactivadaPorDefecto() throws Exception {
        assertThat(nwavConfiguracionIa.nwavActiva()).isFalse();
        assertThat(nwavConfiguracionIa.nwavEstado().nwavDisponible()).isFalse();
        assertThat(nwavConfiguracionIa.nwavEstado().nwavInstrucciones()).contains("Anthropic");

        nwavConfiguracionIa.nwavGuardar(true, "sk-ant-secreta-123", "");
        assertThat(nwavConfiguracionIa.nwavEstado().nwavDisponible()).isTrue();
        assertThat(nwavConfiguracionIa.nwavModelo()).isEqualTo("claude-opus-5-5");
        assertThat(nwavConfiguracionIa.nwavClaveApi()).isEqualTo("sk-ant-secreta-123");
        // en la base de datos no aparece en claro
        assertThat(nwavRepositorio.findAll()).noneMatch(nwavFila -> String.valueOf(nwavFila.getNwavValor())
                .contains("sk-ant-secreta"));

        // guardar sin clave nueva conserva la anterior
        nwavConfiguracionIa.nwavGuardar(true, "", "claude-sonnet-5-5");
        assertThat(nwavConfiguracionIa.nwavClaveApi()).isEqualTo("sk-ant-secreta-123");
        assertThat(nwavConfiguracionIa.nwavModelo()).isEqualTo("claude-sonnet-5-5");

        nwavConfiguracionIa.nwavBorrarClave();
        assertThat(nwavConfiguracionIa.nwavTieneClave()).isFalse();
        assertThat(nwavConfiguracionIa.nwavActiva()).isFalse();
    }

    @Test
    void nwavElCifradoUsaVectoresDistintosYDetectaManipulacion() {
        String nwavA = nwavCifrado.nwavCifrar("hola");
        String nwavB = nwavCifrado.nwavCifrar("hola");
        assertThat(nwavA).isNotEqualTo(nwavB).doesNotContain("hola");
        assertThat(nwavCifrado.nwavDescifrar(nwavA)).isEqualTo("hola");
        char[] nwavLetras = nwavA.toCharArray();
        nwavLetras[nwavLetras.length - 3] = nwavLetras[nwavLetras.length - 3] == 'A' ? 'B' : 'A';
        assertThatThrownBy(() -> nwavCifrado.nwavDescifrar(new String(nwavLetras))).isInstanceOf(NwavEditorException.class);
        assertThatThrownBy(() -> nwavCifrado.nwavDescifrar("texto plano")).hasMessageContaining("Preferencias");
    }

    @Test
    void nwavElArchivoDeClaveLocalSoloLoLeeElUsuario() throws Exception {
        NwavCifradoService nwavPropio = new NwavCifradoService(nwavCarpeta.toString());
        nwavPropio.nwavCifrar("x");
        Path nwavArchivo = nwavCarpeta.resolve("nwav_clave_local.bin");
        assertThat(nwavArchivo).exists();
        assertThat(Files.size(nwavArchivo)).isEqualTo(32);
        if (nwavCarpeta.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            assertThat(Files.getPosixFilePermissions(nwavArchivo))
                    .containsExactlyInAnyOrder(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        }
        // otra clave local no puede descifrar lo de esta
        assertThatThrownBy(() -> new NwavCifradoService(nwavCarpeta.resolve("otra").toString())
                .nwavDescifrar(nwavPropio.nwavCifrar("y"))).isInstanceOf(NwavEditorException.class);
    }

    @Test
    void nwavElClienteRealNoLlamaSiLaIaEstaDesactivada() {
        assertThatThrownBy(() -> nwavClaude.nwavGenerar("s", null, "t", NwavCancelacion.NWAV_NUNCA))
                .isInstanceOf(NwavEditorException.class).hasMessageContaining("Preferencias");
    }

    // ================================================================ resumir

    @Test
    void nwavResumirEnviaElPdfYGuardaConAvisoDeIa() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "informe.pdf", 3);
        NwavClienteFalso nwavCliente = new NwavClienteFalso(nwavTexto -> "## Puntos clave\n\n- Uno\n- Dos");
        Path nwavMd = nwavServicio(nwavCliente).nwavResumir(nwavPdf, null, "francés", NwavIaService.NwavLongitud.NWAV_BREVE,
                false, nwavCarpeta.resolve("resumen.md"), NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);

        assertThat(nwavCliente.nwavPdfs).singleElement().satisfies(nwavBytes ->
                assertThat(nwavBytes).isEqualTo(Files.readAllBytes(nwavPdf)));
        assertThat(nwavCliente.nwavSistemas.get(0)).contains("francés").contains("no inventes");
        assertThat(nwavCliente.nwavTextos.get(0)).contains("párrafo breve");
        String nwavContenido = Files.readString(nwavMd, StandardCharsets.UTF_8);
        assertThat(nwavContenido).startsWith("# Resumen de «informe.pdf»").contains("Generado con IA")
                .contains("- Uno");
    }

    @Test
    void nwavResumirUnPdfProtegidoEnviaUnaCopiaSinContrasena() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrearCifrado(nwavCarpeta, "secreto.pdf", 1, "clave");
        NwavClienteFalso nwavCliente = new NwavClienteFalso(nwavTexto -> "Resumen");
        nwavServicio(nwavCliente).nwavResumir(nwavPdf, "clave", "español", NwavIaService.NwavLongitud.NWAV_NORMAL, true,
                nwavCarpeta.resolve("r.pdf"), NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);
        try (PDDocument nwavEnviado = Loader.loadPDF(nwavCliente.nwavPdfs.get(0))) {
            assertThat(nwavEnviado.isEncrypted()).isFalse();
        }
        try (PDDocument nwavResultado = Loader.loadPDF(nwavCarpeta.resolve("r.pdf").toFile())) {
            assertThat(new PDFTextStripper().getText(nwavResultado)).contains("Resumen de").contains("Generado con IA");
        }
    }

    // =============================================================== traducir

    @Test
    void nwavTraducirPorPartesEnOrdenYGuardarComoPdf() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "doc.pdf", 4);
        NwavClienteFalso nwavCliente = new NwavClienteFalso(nwavTexto -> nwavTexto.replace("Pagina", "Page"));
        Path nwavResultado = nwavServicio(nwavCliente).nwavTraducir(nwavPdf, null, "inglés", true,
                nwavCarpeta.resolve("doc_en.pdf"), NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);

        assertThat(nwavCliente.nwavSistemas.get(0)).contains("inglés").contains("Markdown");
        assertThat(nwavCliente.nwavPdfs).containsOnlyNulls(); // se traduce el texto, no se adjunta el PDF
        try (PDDocument nwavPd = Loader.loadPDF(nwavResultado.toFile())) {
            String nwavTexto = new PDFTextStripper().getText(nwavPd).replaceAll("\\s+", "");
            assertThat(nwavTexto).contains("Page1").contains("Page4").doesNotContain("<texto>");
            assertThat(nwavTexto.indexOf("Page1")).isLessThan(nwavTexto.indexOf("Page4"));
        }
    }

    @Test
    void nwavLosTrozosRespetanElLimiteYElOrden() {
        List<String> nwavPaginas = List.of("A".repeat(50), "B".repeat(50), "", "C".repeat(30) + "\n\n" + "D".repeat(30),
                "E".repeat(130));
        List<String> nwavTrozos = NwavIaService.nwavTrozos(nwavPaginas, 60);
        assertThat(nwavTrozos).allSatisfy(nwavT -> assertThat(nwavT.length()).isLessThanOrEqualTo(64));
        assertThat(String.join("", nwavTrozos).replaceAll("\\s", ""))
                .isEqualTo("A".repeat(50) + "B".repeat(50) + "C".repeat(30) + "D".repeat(30) + "E".repeat(130));
    }

    @Test
    void nwavUnEscaneoSinTextoPideOcrSinLlamarALaIa() throws Exception {
        Path nwavEscaneo = NwavPdfConImagenes.nwavCrearEscaneado(nwavCarpeta, "e.pdf", "HOLA");
        NwavClienteFalso nwavCliente = new NwavClienteFalso(nwavTexto -> "x");
        assertThatThrownBy(() -> nwavServicio(nwavCliente).nwavTraducir(nwavEscaneo, null, "inglés", false,
                nwavCarpeta.resolve("t.md"), NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA)).hasMessageContaining("OCR");
        assertThat(nwavCliente.nwavTextos).isEmpty();
    }

    @Test
    void nwavMarkdownAHtml() {
        String nwavHtml = NwavMarkdownHtml.nwavConvertir("""
                # Título <x>
                Párrafo con **negrita**, *cursiva* y [enlace](https://ejemplo.org).
                continúa aquí

                - uno
                - dos
                1. primero

                | A | B |
                | --- | --- |
                | 1 | 2 \\| 3 |
                """, "T");
        assertThat(nwavHtml).contains("<h1>Título &lt;x&gt;</h1>")
                .contains("<p>Párrafo con <strong>negrita</strong>, <em>cursiva</em> y <a href=\"https://ejemplo.org\">"
                        + "enlace</a>. continúa aquí</p>")
                .contains("<ul><li>uno</li><li>dos</li></ul><ol><li>primero</li></ol>")
                .contains("<th>A</th><th>B</th>").contains("<td>2 | 3</td>");
        assertThat(Set.of(nwavHtml.split("<table>")).size()).isEqualTo(2);
    }
}
