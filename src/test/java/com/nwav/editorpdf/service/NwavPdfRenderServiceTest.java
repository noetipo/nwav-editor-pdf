package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.soporte.NwavPdfDePrueba;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NwavPdfRenderServiceTest {
    private final NwavPdfDocumentoService nwavDocumentos = new NwavPdfDocumentoService();
    private final NwavPdfRenderService nwavRender = new NwavPdfRenderService();

    @TempDir
    Path nwavCarpeta;

    @Test
    void nwavRenderizaConElTamanoSegunLaEscala() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "a4.pdf", 2);
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavPdf, null)) {
            BufferedImage nwavImagen = nwavRender.nwavRenderizarPagina(nwavDocumento, 0, 1.0f);

            assertThat(nwavImagen.getWidth()).isBetween(594, 596);
            assertThat(nwavImagen.getHeight()).isBetween(841, 843);
            assertThat(nwavTienePixelesOscuros(nwavImagen)).isTrue();
        }
    }

    @Test
    void nwavLaSegundaPeticionSaleDeLaCache() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "cache.pdf", 1);
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavPdf, null)) {
            BufferedImage nwavPrimera = nwavRender.nwavRenderizarPagina(nwavDocumento, 0, 0.5f);
            BufferedImage nwavSegunda = nwavRender.nwavRenderizarPagina(nwavDocumento, 0, 0.5f);

            assertThat(nwavSegunda).isSameAs(nwavPrimera);
            assertThat(nwavDocumento.nwavCachePaginas().nwavCantidad()).isEqualTo(1);
        }
    }

    @Test
    void nwavLaMiniaturaTieneElAnchoPedido() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "mini.pdf", 3);
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavPdf, null)) {
            BufferedImage nwavMiniatura = nwavRender.nwavRenderizarMiniatura(nwavDocumento, 2, 120);

            assertThat(nwavMiniatura.getWidth()).isBetween(119, 121);
            assertThat(nwavDocumento.nwavCacheMiniaturas().nwavCantidad()).isEqualTo(1);
            assertThat(nwavDocumento.nwavCachePaginas().nwavCantidad()).isZero();
        }
    }

    @Test
    void nwavCerrarElDocumentoLiberaLaCache() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "liberar.pdf", 2);
        NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavPdf, null);
        nwavRender.nwavRenderizarPagina(nwavDocumento, 0, 0.5f);
        nwavRender.nwavRenderizarMiniatura(nwavDocumento, 1, 80);

        nwavDocumento.close();

        assertThat(nwavDocumento.nwavCachePaginas().nwavCantidad()).isZero();
        assertThat(nwavDocumento.nwavCacheMiniaturas().nwavCantidad()).isZero();
        assertThatThrownBy(() -> nwavRender.nwavRenderizarPagina(nwavDocumento, 1, 0.5f))
                .isInstanceOf(NwavEditorException.class)
                .hasMessageContaining("cerrado");
    }

    @Test
    void nwavPaginaFueraDeRangoSeRechaza() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "rango.pdf", 1);
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavPdf, null)) {
            assertThatThrownBy(() -> nwavRender.nwavRenderizarPagina(nwavDocumento, 5, 1f))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void nwavLaEscalaSeLimitaParaNoAgotarMemoria() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "grande.pdf", 1);
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavPdf, null)) {
            float nwavSegura = nwavRender.nwavEscalaSegura(nwavDocumento, 0, 40f);
            NwavDocumentoPdf.NwavTamanoPagina nwavTamano = nwavDocumento.nwavTamano(0);
            double nwavPixeles = nwavTamano.nwavAncho() * nwavTamano.nwavAlto() * nwavSegura * nwavSegura;

            assertThat(nwavSegura).isLessThan(40f);
            assertThat(nwavPixeles).isLessThanOrEqualTo(NwavPdfRenderService.NWAV_MAXIMO_PIXELES * 1.001);
        }
    }

    private static boolean nwavTienePixelesOscuros(BufferedImage nwavImagen) {
        for (int nwavY = 0; nwavY < nwavImagen.getHeight(); nwavY += 2) {
            for (int nwavX = 0; nwavX < nwavImagen.getWidth(); nwavX += 2) {
                if ((nwavImagen.getRGB(nwavX, nwavY) & 0xFF) < 100) {
                    return true;
                }
            }
        }
        return false;
    }
}
