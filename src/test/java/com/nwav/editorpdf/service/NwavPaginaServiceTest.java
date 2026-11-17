package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.service.comando.NwavComandoDuplicar;
import com.nwav.editorpdf.service.comando.NwavComandoEliminar;
import com.nwav.editorpdf.service.comando.NwavComandoInsertarImagenes;
import com.nwav.editorpdf.service.comando.NwavComandoInsertarPdf;
import com.nwav.editorpdf.service.comando.NwavComandoMover;
import com.nwav.editorpdf.service.comando.NwavComandoRotar;
import com.nwav.editorpdf.service.comando.NwavGestorComandos;
import com.nwav.editorpdf.soporte.NwavLectorPdf;
import com.nwav.editorpdf.soporte.NwavPdfDePrueba;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Operaciones de pagina en memoria y su deshacer/rehacer, sobre PDF generados aqui. */
class NwavPaginaServiceTest {

    private final NwavPdfDocumentoService nwavDocumentos = new NwavPdfDocumentoService();
    private final NwavPaginaService nwavPaginas = new NwavPaginaService(nwavDocumentos);
    private final NwavPdfRenderService nwavRender = new NwavPdfRenderService();

    @TempDir
    Path nwavCarpeta;

    private NwavDocumentoPdf nwavDocumento;
    private NwavGestorComandos nwavGestor;

    @BeforeEach
    void nwavAbrirCinco() throws Exception {
        nwavDocumento = nwavDocumentos.nwavAbrir(NwavPdfDePrueba.nwavCrear(nwavCarpeta, "cinco.pdf", 5), null);
        nwavGestor = nwavDocumento.getNwavGestor();
    }

    @AfterEach
    void nwavCerrar() {
        nwavDocumento.close();
    }

    private List<String> nwavTextos() throws Exception {
        return NwavLectorPdf.nwavTextos(nwavDocumento.nwavPdDocument());
    }

    private static List<String> nwavPaginasNumero(int... nwavNumeros) {
        return java.util.Arrays.stream(nwavNumeros).mapToObj(nwavNumero -> "Pagina" + nwavNumero).toList();
    }

    @Test
    void nwavRotarSumaGradosYSeDeshace() {
        nwavGestor.nwavEjecutar(new NwavComandoRotar(nwavPaginas, nwavDocumento, List.of(0, 2), 90));

        assertThat(nwavDocumento.nwavPdDocument().getPage(0).getRotation()).isEqualTo(90);
        assertThat(nwavDocumento.nwavPdDocument().getPage(1).getRotation()).isZero();
        assertThat(nwavDocumento.nwavTamano(0).nwavAncho()).isGreaterThan(nwavDocumento.nwavTamano(0).nwavAlto());

        nwavGestor.nwavEjecutar(new NwavComandoRotar(nwavPaginas, nwavDocumento, List.of(0), -90));
        nwavGestor.nwavEjecutar(new NwavComandoRotar(nwavPaginas, nwavDocumento, List.of(0), -90));
        assertThat(nwavDocumento.nwavPdDocument().getPage(0).getRotation()).isEqualTo(270);

        nwavGestor.nwavDeshacer();
        nwavGestor.nwavDeshacer();
        nwavGestor.nwavDeshacer();
        assertThat(nwavDocumento.nwavPdDocument().getPage(0).getRotation()).isZero();
        assertThat(nwavDocumento.nwavPdDocument().getPage(2).getRotation()).isZero();
    }

    @Test
    void nwavEliminarYDeshacerRestauraElOrden() throws Exception {
        nwavGestor.nwavEjecutar(new NwavComandoEliminar(nwavPaginas, nwavDocumento, List.of(1, 3)));
        assertThat(nwavTextos()).isEqualTo(nwavPaginasNumero(1, 3, 5));
        assertThat(nwavDocumento.getNwavNumeroPaginas()).isEqualTo(3);

        nwavGestor.nwavDeshacer();
        assertThat(nwavTextos()).isEqualTo(nwavPaginasNumero(1, 2, 3, 4, 5));

        nwavGestor.nwavRehacer();
        assertThat(nwavTextos()).isEqualTo(nwavPaginasNumero(1, 3, 5));
    }

    @Test
    void nwavNoSePuedenEliminarTodasLasPaginas() throws Exception {
        assertThatThrownBy(() -> nwavGestor.nwavEjecutar(
                new NwavComandoEliminar(nwavPaginas, nwavDocumento, List.of(0, 1, 2, 3, 4))))
                .isInstanceOf(NwavEditorException.class)
                .hasMessageContaining("al menos una");
        assertThat(nwavTextos()).hasSize(5);
        assertThat(nwavGestor.nwavPuedeDeshacer()).isFalse();
    }

    @Test
    void nwavDuplicarInsertaLaCopiaDetrasDelOriginal() throws Exception {
        NwavComandoDuplicar nwavComando = new NwavComandoDuplicar(nwavPaginas, nwavDocumento, List.of(1, 4));
        nwavGestor.nwavEjecutar(nwavComando);

        assertThat(nwavTextos()).isEqualTo(nwavPaginasNumero(1, 2, 2, 3, 4, 5, 5));
        assertThat(nwavComando.nwavPaginaDestacada()).isEqualTo(2);

        // La copia es independiente: rotarla no rota el original.
        nwavGestor.nwavEjecutar(new NwavComandoRotar(nwavPaginas, nwavDocumento, List.of(2), 90));
        assertThat(nwavDocumento.nwavPdDocument().getPage(1).getRotation()).isZero();

        nwavGestor.nwavDeshacer();
        nwavGestor.nwavDeshacer();
        assertThat(nwavTextos()).isEqualTo(nwavPaginasNumero(1, 2, 3, 4, 5));
    }

    @Test
    void nwavMoverReordenaComoBloque() throws Exception {
        NwavComandoMover nwavComando = new NwavComandoMover(nwavPaginas, nwavDocumento, List.of(0, 1), 4);
        nwavGestor.nwavEjecutar(nwavComando);

        assertThat(nwavTextos()).isEqualTo(nwavPaginasNumero(3, 4, 1, 2, 5));
        assertThat(nwavComando.nwavPaginaDestacada()).isEqualTo(2);

        nwavGestor.nwavEjecutar(new NwavComandoMover(nwavPaginas, nwavDocumento, List.of(4), 0));
        assertThat(nwavTextos()).isEqualTo(nwavPaginasNumero(5, 3, 4, 1, 2));

        nwavGestor.nwavDeshacer();
        nwavGestor.nwavDeshacer();
        assertThat(nwavTextos()).isEqualTo(nwavPaginasNumero(1, 2, 3, 4, 5));
    }

    @Test
    void nwavMoverASuMismaPosicionNoSeApila() {
        boolean nwavApilado = nwavGestor.nwavEjecutar(
                new NwavComandoMover(nwavPaginas, nwavDocumento, List.of(2), 2));

        assertThat(nwavApilado).isFalse();
        assertThat(nwavGestor.nwavPuedeDeshacer()).isFalse();
        assertThat(nwavDocumento.nwavHayCambiosSinGuardar()).isFalse();
    }

    @Test
    void nwavInsertarPaginasDeOtroPdf() throws Exception {
        Path nwavOtro = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "otro.pdf", 3);
        // Las paginas del otro PDF dicen tambien "Pagina N": se insertan la 2 y la 3 tras la 1.
        nwavGestor.nwavEjecutar(new NwavComandoInsertarPdf(nwavPaginas, nwavDocumento, nwavOtro, null, "2-3", 1));

        assertThat(nwavTextos()).isEqualTo(nwavPaginasNumero(1, 2, 3, 2, 3, 4, 5));

        nwavGestor.nwavDeshacer();
        assertThat(nwavTextos()).isEqualTo(nwavPaginasNumero(1, 2, 3, 4, 5));
        nwavGestor.nwavRehacer();
        assertThat(nwavTextos()).hasSize(7);
    }

    @Test
    void nwavInsertarDesdeUnPdfProtegidoPideLaContrasena() throws Exception {
        Path nwavProtegido = NwavPdfDePrueba.nwavCrearCifrado(nwavCarpeta, "prot.pdf", 2, "clave");

        assertThatThrownBy(() -> nwavGestor.nwavEjecutar(
                new NwavComandoInsertarPdf(nwavPaginas, nwavDocumento, nwavProtegido, null, null, 0)))
                .isInstanceOf(com.nwav.editorpdf.exception.NwavContrasenaRequeridaException.class);

        nwavGestor.nwavEjecutar(new NwavComandoInsertarPdf(nwavPaginas, nwavDocumento, nwavProtegido, "clave", null, 5));
        assertThat(nwavTextos()).hasSize(7);
    }

    @Test
    void nwavInsertarImagenesCreaPaginasA4SegunOrientacion() throws Exception {
        Path nwavHorizontal = NwavLectorPdf.nwavCrearPng(nwavCarpeta, "ancha.png", 800, 400);
        Path nwavVertical = NwavLectorPdf.nwavCrearPng(nwavCarpeta, "alta.png", 300, 600);

        nwavGestor.nwavEjecutar(new NwavComandoInsertarImagenes(nwavPaginas, nwavDocumento,
                List.of(nwavHorizontal, nwavVertical), 0));

        assertThat(nwavDocumento.getNwavNumeroPaginas()).isEqualTo(7);
        assertThat(nwavDocumento.nwavTamano(0).nwavAncho()).isGreaterThan(nwavDocumento.nwavTamano(0).nwavAlto());
        assertThat(nwavDocumento.nwavTamano(1).nwavAncho()).isLessThan(nwavDocumento.nwavTamano(1).nwavAlto());
        assertThat(nwavTextos().subList(2, 7)).isEqualTo(nwavPaginasNumero(1, 2, 3, 4, 5));
    }

    @Test
    void nwavUnaImagenInvalidaNoCambiaElDocumento() throws Exception {
        Path nwavFalsa = Files.writeString(nwavCarpeta.resolve("falsa.png"), "no es una imagen");

        assertThatThrownBy(() -> nwavGestor.nwavEjecutar(new NwavComandoInsertarImagenes(nwavPaginas, nwavDocumento,
                List.of(nwavFalsa), 0)))
                .isInstanceOf(NwavEditorException.class)
                .hasMessageContaining("falsa.png");
        assertThat(nwavTextos()).isEqualTo(nwavPaginasNumero(1, 2, 3, 4, 5));
    }

    @Test
    void nwavElRenderizadoSigueLaNuevaEstructura() {
        nwavGestor.nwavEjecutar(new NwavComandoRotar(nwavPaginas, nwavDocumento, List.of(0), 90));
        var nwavImagen = nwavRender.nwavRenderizarPagina(nwavDocumento, 0, 0.5f);

        assertThat(nwavImagen.getWidth()).isGreaterThan(nwavImagen.getHeight());
    }

    @Test
    void nwavElGestorSabeSiHayCambiosSinGuardar() {
        assertThat(nwavDocumento.nwavHayCambiosSinGuardar()).isFalse();
        nwavGestor.nwavEjecutar(new NwavComandoRotar(nwavPaginas, nwavDocumento, List.of(0), 90));
        assertThat(nwavDocumento.nwavHayCambiosSinGuardar()).isTrue();
        nwavGestor.nwavDeshacer();
        assertThat(nwavDocumento.nwavHayCambiosSinGuardar()).isFalse();
    }
}
