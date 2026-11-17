package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavContrasenaRequeridaException;
import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.soporte.NwavLectorPdf;
import com.nwav.editorpdf.soporte.NwavPdfConImagenes;
import com.nwav.editorpdf.soporte.NwavPdfDePrueba;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** Fase B: herramientas de organizar por archivo. */
class NwavOrganizarTest {

    private final NwavPdfDocumentoService nwavDocumentos = new NwavPdfDocumentoService();
    private final NwavOrganizarService nwavOrganizar = new NwavOrganizarService(nwavDocumentos);

    @TempDir
    Path nwavCarpeta;

    private static List<String> nwavPaginas(int... nwavNumeros) {
        return java.util.Arrays.stream(nwavNumeros).mapToObj(nwavNumero -> "Pagina" + nwavNumero).toList();
    }

    @Test
    void nwavUnirIncluyendoUnPdfProtegido() throws Exception {
        Path nwavA = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "a.pdf", 2);
        Path nwavB = NwavPdfDePrueba.nwavCrearCifrado(nwavCarpeta, "b.pdf", 1, "clave");

        assertThatThrownBy(() -> nwavOrganizar.nwavUnir(List.of(nwavA, nwavB), Map.of(), nwavCarpeta.resolve("u.pdf"),
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA)).isInstanceOf(NwavContrasenaRequeridaException.class);

        Path nwavUnido = nwavOrganizar.nwavUnir(List.of(nwavB, nwavA), Map.of(nwavB, "clave"), nwavCarpeta.resolve("u.pdf"),
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);
        assertThat(NwavLectorPdf.nwavTextos(nwavUnido)).isEqualTo(nwavPaginas(1, 1, 2));
    }

    @Test
    void nwavDividirPorRangosYCadaN() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "siete.pdf", 7);

        List<Path> nwavRangos = nwavOrganizar.nwavDividirPorRangos(nwavOriginal, null, "1-2, 3, 4-", null,
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);
        assertThat(nwavRangos).extracting(nwavRuta -> nwavRuta.getFileName().toString())
                .containsExactly("siete_parte01.pdf", "siete_parte02.pdf", "siete_parte03.pdf");
        assertThat(NwavLectorPdf.nwavTextos(nwavRangos.get(2))).isEqualTo(nwavPaginas(4, 5, 6, 7));

        List<Path> nwavCada = nwavOrganizar.nwavDividirCada(nwavOriginal, null, 3, nwavCarpeta.resolve("."),
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);
        assertThat(nwavCada).hasSize(3);
        assertThat(NwavLectorPdf.nwavTextos(nwavCada.get(2))).isEqualTo(nwavPaginas(7));
    }

    @Test
    void nwavDividirPorTamanoRespetaElLimite() throws Exception {
        Path nwavPesado = NwavPdfConImagenes.nwavCrearPesado(nwavCarpeta, "pesado.pdf");
        long nwavLimite = Files.size(nwavPesado) * 6 / 10; // mas de media pagina, menos que el total

        List<Path> nwavPartes = nwavOrganizar.nwavDividirPorTamano(nwavPesado, null, nwavLimite, null,
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);

        assertThat(nwavPartes).hasSize(2);
        int nwavPaginasTotales = 0;
        for (Path nwavParte : nwavPartes) {
            assertThat(Files.size(nwavParte)).isLessThanOrEqualTo((long) (nwavLimite * 1.05));
            try (PDDocument nwavPd = Loader.loadPDF(nwavParte.toFile())) {
                nwavPaginasTotales += nwavPd.getNumberOfPages();
            }
        }
        assertThat(nwavPaginasTotales).isEqualTo(2);
    }

    @Test
    void nwavEliminarYExtraer() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "cinco.pdf", 5);

        Path nwavSin = nwavOrganizar.nwavEliminarPaginas(nwavOriginal, null, "2, 4-", nwavCarpeta.resolve("sin.pdf"));
        assertThat(NwavLectorPdf.nwavTextos(nwavSin)).isEqualTo(nwavPaginas(1, 3));
        assertThatThrownBy(() -> nwavOrganizar.nwavEliminarPaginas(nwavOriginal, null, "1-", nwavCarpeta.resolve("x.pdf")))
                .isInstanceOf(NwavEditorException.class);

        List<Path> nwavSueltas = nwavOrganizar.nwavExtraerPaginas(nwavOriginal, null, "2, 5", true, null,
                NwavCancelacion.NWAV_NUNCA);
        assertThat(nwavSueltas).hasSize(2);
        assertThat(NwavLectorPdf.nwavTextos(nwavSueltas.get(1))).isEqualTo(nwavPaginas(5));
        List<Path> nwavJuntas = nwavOrganizar.nwavExtraerPaginas(nwavOriginal, null, "4, 1", false, null,
                NwavCancelacion.NWAV_NUNCA);
        assertThat(NwavLectorPdf.nwavTextos(nwavJuntas.get(0))).isEqualTo(nwavPaginas(4, 1));
    }

    @Test
    void nwavOrdenarInvertirYPersonalizado() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "cuatro.pdf", 4);

        assertThat(NwavLectorPdf.nwavTextos(nwavOrganizar.nwavOrdenar(nwavOriginal, null, null, true,
                nwavCarpeta.resolve("inv.pdf")))).isEqualTo(nwavPaginas(4, 3, 2, 1));
        assertThat(NwavLectorPdf.nwavTextos(nwavOrganizar.nwavOrdenar(nwavOriginal, null, "3, 1, 4-", false,
                nwavCarpeta.resolve("per.pdf")))).isEqualTo(nwavPaginas(3, 1, 4));
    }

    @Test
    void nwavRotarPaginasConcretas() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "rot.pdf", 3);
        Path nwavRotado = nwavOrganizar.nwavRotar(nwavOriginal, null, 270, "2-", nwavCarpeta.resolve("r.pdf"));

        try (PDDocument nwavPd = Loader.loadPDF(nwavRotado.toFile())) {
            assertThat(nwavPd.getPage(0).getRotation()).isZero();
            assertThat(nwavPd.getPage(1).getRotation()).isEqualTo(270);
            assertThat(nwavPd.getPage(2).getRotation()).isEqualTo(270);
        }
    }

    @Test
    void nwavRecortarMargenesRespetaLaRotacion() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrearConRotacion(nwavCarpeta, "girado.pdf", 2, 90);
        Path nwavRecortado = nwavOrganizar.nwavRecortarMargenes(nwavOriginal, null, 20, 0, 0, 10, "",
                nwavCarpeta.resolve("rec.pdf"));

        try (PDDocument nwavPd = Loader.loadPDF(nwavRecortado.toFile())) {
            double nwavMm = NwavOrganizarService.NWAV_PUNTOS_POR_MM;
            // Pagina 1 (sin giro): ancho y alto visibles reducidos por los margenes.
            NwavDocumentoPdf.NwavTamanoPagina nwavPrimera = NwavDocumentoPdf.nwavCalcularTamano(nwavPd.getPage(0));
            assertThat((double) nwavPrimera.nwavAncho()).isCloseTo(595.27 - 10 * nwavMm, within(0.5));
            assertThat((double) nwavPrimera.nwavAlto()).isCloseTo(841.89 - 20 * nwavMm, within(0.5));
            // Pagina 2 (girada 90): se ve apaisada; el margen "superior" recorta su alto visible.
            NwavDocumentoPdf.NwavTamanoPagina nwavSegunda = NwavDocumentoPdf.nwavCalcularTamano(nwavPd.getPage(1));
            assertThat((double) nwavSegunda.nwavAncho()).isCloseTo(841.89 - 10 * nwavMm, within(0.5));
            assertThat((double) nwavSegunda.nwavAlto()).isCloseTo(595.27 - 20 * nwavMm, within(0.5));
        }
        assertThat(NwavLectorPdf.nwavTextos(nwavRecortado).get(0)).isEqualTo("Pagina1");
    }

    @Test
    void nwavRecortarConElRatonSePuedeDeshacer() throws Exception {
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(NwavPdfDePrueba.nwavCrear(nwavCarpeta, "m.pdf", 2),
                null)) {
            NwavPaginaService nwavPaginasServicio = new NwavPaginaService(nwavDocumentos);
            nwavDocumento.getNwavGestor().nwavEjecutar(nwavPaginasServicio.nwavComandoRecortar(nwavDocumento, List.of(0, 1),
                    new NwavRectangulo(50, 100, 300, 400)));
            assertThat(nwavDocumento.nwavTamano(0).nwavAncho()).isCloseTo(300f, within(0.5f));
            assertThat(nwavDocumento.nwavTamano(1).nwavAlto()).isCloseTo(400f, within(0.5f));

            nwavDocumento.getNwavGestor().nwavDeshacer();
            assertThat(nwavDocumento.nwavTamano(0).nwavAncho()).isCloseTo(595.27f, within(0.5f));
        }
    }
}
