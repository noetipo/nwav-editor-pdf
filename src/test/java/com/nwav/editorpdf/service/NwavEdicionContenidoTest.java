package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavColorRgb;
import com.nwav.editorpdf.model.NwavEstiloTexto;
import com.nwav.editorpdf.model.NwavFuente;
import com.nwav.editorpdf.model.NwavHistorialOperacion;
import com.nwav.editorpdf.model.NwavPunto;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.model.NwavTipoForma;
import com.nwav.editorpdf.model.NwavTipoMarca;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.repository.NwavHistorialOperacionRepository;
import com.nwav.editorpdf.service.NwavEdicionService.NwavResultadoEdicion;
import com.nwav.editorpdf.soporte.NwavLectorPdf;
import com.nwav.editorpdf.soporte.NwavPdfDePrueba;
import com.nwav.editorpdf.soporte.NwavPruebaConBaseDatos;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Ediciones de contenido a traves del servicio de edicion: historial, deshacer y guardado. */
class NwavEdicionContenidoTest extends NwavPruebaConBaseDatos {

    @Autowired private NwavEdicionService nwavEdicion;
    @Autowired private NwavArchivoPdfService nwavArchivos;
    @Autowired private NwavPdfDocumentoService nwavDocumentos;
    @Autowired private NwavHistorialOperacionRepository nwavHistorial;

    @TempDir
    Path nwavCarpeta;

    @BeforeEach
    void nwavLimpiar() {
        nwavHistorial.deleteAll();
    }

    @Test
    void nwavCadaEdicionDeContenidoQuedaEnElHistorialYNoEsEstructural() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "contenido.pdf", 2);
        Path nwavPng = NwavLectorPdf.nwavCrearPng(nwavCarpeta, "logo.png", 60, 30);
        Path nwavCopia = nwavCarpeta.resolve("contenido-editado.pdf");
        NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavOriginal, null);

        NwavResultadoEdicion nwavTexto = nwavEdicion.nwavAgregarTexto(nwavDocumento, 1, new NwavRectangulo(72, 200, 300, 0),
                "Aprobado", new NwavEstiloTexto(NwavFuente.NWAV_TIMES, 20, true, false, NwavColorRgb.NWAV_ROJO));
        nwavEdicion.nwavInsertarImagen(nwavDocumento, 1, nwavPng, new NwavRectangulo(400, 50, 60, 30));
        nwavEdicion.nwavCubrir(nwavDocumento, 0, new NwavRectangulo(10, 700, 100, 40), NwavColorRgb.NWAV_BLANCO);
        nwavEdicion.nwavMarcar(nwavDocumento, 0, NwavTipoMarca.NWAV_RESALTADO, new NwavRectangulo(40, 40, 400, 70),
                NwavColorRgb.NWAV_AMARILLO);
        nwavEdicion.nwavAgregarNota(nwavDocumento, 0, new NwavPunto(500, 100), "Ver anexo", NwavColorRgb.NWAV_AMARILLO);
        nwavEdicion.nwavDibujar(nwavDocumento, 0, List.of(new NwavPunto(10, 10), new NwavPunto(40, 30)),
                NwavColorRgb.NWAV_ROJO, 2);
        nwavEdicion.nwavAgregarForma(nwavDocumento, 0, NwavTipoForma.NWAV_ELIPSE, new NwavPunto(100, 300),
                new NwavPunto(200, 360), NwavColorRgb.NWAV_ROJO, 2);

        assertThat(nwavTexto.nwavEstructural()).isFalse();
        assertThat(nwavTexto.nwavPaginaDestacada()).isEqualTo(1);
        assertThat(nwavTexto.nwavDescripcion()).isEqualTo("Agregar texto en la página 2");
        assertThat(nwavHistorial.findAllByOrderByNwavFechaDescNwavIdDesc())
                .extracting(NwavHistorialOperacion::getNwavTipo)
                .containsExactly(NwavTipoOperacion.NWAV_FORMA, NwavTipoOperacion.NWAV_DIBUJO, NwavTipoOperacion.NWAV_NOTA,
                        NwavTipoOperacion.NWAV_RESALTAR, NwavTipoOperacion.NWAV_CUBRIR, NwavTipoOperacion.NWAV_IMAGEN,
                        NwavTipoOperacion.NWAV_TEXTO);

        // Deshacer la forma y guardar: el resultado conserva el resto.
        assertThat(nwavEdicion.nwavDeshacer(nwavDocumento)).get()
                .extracting(NwavResultadoEdicion::nwavEstructural).isEqualTo(false);
        try (NwavDocumentoPdf nwavGuardado = nwavArchivos.nwavGuardarComo(nwavDocumento, nwavCopia)) {
            assertThat(nwavGuardado.nwavHayCambiosSinGuardar()).isFalse();
        }
        try (PDDocument nwavReabierto = Loader.loadPDF(nwavCopia.toFile())) {
            assertThat(nwavReabierto.getPage(0).getAnnotations()).extracting(nwavAnotacion -> nwavAnotacion.getSubtype())
                    .containsExactly("Highlight", "Text", "Ink");
            assertThat(NwavLectorPdf.nwavTextos(nwavReabierto).get(1)).contains("Aprobado");
        }
    }
}
