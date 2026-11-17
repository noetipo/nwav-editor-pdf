package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavColorRgb;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.service.comando.NwavComandoDuplicar;
import com.nwav.editorpdf.service.comando.NwavComandoEliminar;
import com.nwav.editorpdf.service.comando.NwavComandoMover;
import com.nwav.editorpdf.service.comando.NwavComandoRotar;
import com.nwav.editorpdf.service.comando.NwavGestorComandos;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regresion del aviso "No se pudo dibujar la página": renderiza TODAS las paginas (y sus
 * miniaturas) de un PDF grande y variado, tambien despues de editarlo y desde dos hilos a
 * la vez, como hace el visor. Ninguna pagina debe fallar.
 */
class NwavRenderizadoCompletoTest {

    private static final int NWAV_PAGINAS = 120;

    private final NwavPdfDocumentoService nwavDocumentos = new NwavPdfDocumentoService();
    private final NwavPaginaService nwavPaginas = new NwavPaginaService(nwavDocumentos);
    private final NwavContenidoService nwavContenido = new NwavContenidoService();
    private final NwavPdfRenderService nwavRender = new NwavPdfRenderService();

    @TempDir
    Path nwavCarpeta;

    /** Mezcla A4 vertical, apaisado, carta, paginas giradas y una CropBox desplazada. */
    private Path nwavCrearPdfVariado() throws Exception {
        Path nwavRuta = nwavCarpeta.resolve("variado.pdf");
        try (PDDocument nwavPd = new PDDocument()) {
            PDType1Font nwavFuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int nwavIndice = 0; nwavIndice < NWAV_PAGINAS; nwavIndice++) {
                PDRectangle nwavTamano = switch (nwavIndice % 4) {
                    case 1 -> new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
                    case 2 -> PDRectangle.LETTER;
                    default -> PDRectangle.A4;
                };
                PDPage nwavPagina = new PDPage(nwavTamano);
                nwavPagina.setRotation((nwavIndice % 5) * 90 % 360);
                if (nwavIndice % 7 == 0) {
                    nwavPagina.setCropBox(new PDRectangle(30, 40, nwavTamano.getWidth() - 60, nwavTamano.getHeight() - 80));
                }
                nwavPd.addPage(nwavPagina);
                try (PDPageContentStream nwavFlujo = new PDPageContentStream(nwavPd, nwavPagina)) {
                    nwavFlujo.beginText();
                    nwavFlujo.setFont(nwavFuente, 14);
                    nwavFlujo.newLineAtOffset(60, 200);
                    nwavFlujo.showText("Pagina " + (nwavIndice + 1));
                    nwavFlujo.endText();
                }
            }
            nwavPd.save(nwavRuta.toFile());
        }
        return nwavRuta;
    }

    /** Renderiza todo y comprueba que cada imagen tiene la orientacion esperada. */
    private void nwavRenderizarTodo(NwavDocumentoPdf nwavDocumento) {
        for (int nwavIndice = 0; nwavIndice < nwavDocumento.getNwavNumeroPaginas(); nwavIndice++) {
            BufferedImage nwavPagina = nwavRender.nwavRenderizarPagina(nwavDocumento, nwavIndice, 0.5f);
            BufferedImage nwavMiniatura = nwavRender.nwavRenderizarMiniatura(nwavDocumento, nwavIndice, 116);
            NwavDocumentoPdf.NwavTamanoPagina nwavTamano = nwavDocumento.nwavTamano(nwavIndice);
            assertThat(nwavPagina.getWidth() > nwavPagina.getHeight())
                    .as("orientacion de la pagina %d", nwavIndice + 1)
                    .isEqualTo(nwavTamano.nwavAncho() > nwavTamano.nwavAlto());
            assertThat(nwavMiniatura.getWidth()).isBetween(115, 117);
        }
    }

    @Test
    void nwavTodasLasPaginasSeDibujanAntesYDespuesDeEditar() throws Exception {
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavCrearPdfVariado(), null)) {
            nwavRenderizarTodo(nwavDocumento);

            NwavGestorComandos nwavGestor = nwavDocumento.getNwavGestor();
            nwavGestor.nwavEjecutar(new NwavComandoRotar(nwavPaginas, nwavDocumento, List.of(0, 5, 9), 90));
            nwavGestor.nwavEjecutar(new NwavComandoMover(nwavPaginas, nwavDocumento, List.of(10, 11, 12), 100));
            nwavGestor.nwavEjecutar(new NwavComandoDuplicar(nwavPaginas, nwavDocumento, List.of(3, 50)));
            nwavGestor.nwavEjecutar(new NwavComandoEliminar(nwavPaginas, nwavDocumento, List.of(1, 2)));
            nwavGestor.nwavEjecutar(nwavContenido.nwavComandoCubrir(nwavDocumento, 7,
                    new NwavRectangulo(10, 10, 50, 50), NwavColorRgb.NWAV_NEGRO));
            assertThat(nwavDocumento.getNwavNumeroPaginas()).isEqualTo(NWAV_PAGINAS);
            nwavRenderizarTodo(nwavDocumento);

            while (nwavGestor.nwavDeshacer().isPresent()) {
                // deshacer todo
            }
            nwavRenderizarTodo(nwavDocumento);
        }
    }

    @Test
    void nwavDosHilosDibujandoALaVezNoFallan() throws Exception {
        ExecutorService nwavHilos = Executors.newFixedThreadPool(2);
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavCrearPdfVariado(), null)) {
            // Como el visor: un hilo para paginas y otro para miniaturas.
            Future<?> nwavPaginasFuturo = nwavHilos.submit(() -> {
                for (int nwavIndice = 0; nwavIndice < NWAV_PAGINAS; nwavIndice++) {
                    nwavRender.nwavRenderizarPagina(nwavDocumento, nwavIndice, 0.4f);
                }
            });
            Future<?> nwavMiniaturasFuturo = nwavHilos.submit(() -> {
                for (int nwavIndice = NWAV_PAGINAS - 1; nwavIndice >= 0; nwavIndice--) {
                    nwavRender.nwavRenderizarMiniatura(nwavDocumento, nwavIndice, 116);
                }
            });
            nwavPaginasFuturo.get();
            nwavMiniaturasFuturo.get();
        } finally {
            nwavHilos.shutdownNow();
        }
    }
}
