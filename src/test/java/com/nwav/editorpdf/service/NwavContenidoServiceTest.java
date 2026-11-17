package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.model.NwavColorRgb;
import com.nwav.editorpdf.model.NwavEstiloTexto;
import com.nwav.editorpdf.model.NwavFuente;
import com.nwav.editorpdf.model.NwavPunto;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.model.NwavTipoForma;
import com.nwav.editorpdf.model.NwavTipoMarca;
import com.nwav.editorpdf.service.comando.NwavComando;
import com.nwav.editorpdf.service.comando.NwavGestorComandos;
import com.nwav.editorpdf.soporte.NwavLectorPdf;
import com.nwav.editorpdf.soporte.NwavPdfDePrueba;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** Edicion de contenido y anotaciones, verificada leyendo y renderizando el PDF. */
class NwavContenidoServiceTest {

    private static final NwavEstiloTexto NWAV_ESTILO =
            new NwavEstiloTexto(NwavFuente.NWAV_HELVETICA, 18, false, false, NwavColorRgb.NWAV_NEGRO);

    private final NwavPdfDocumentoService nwavDocumentos = new NwavPdfDocumentoService();
    private final NwavContenidoService nwavContenido = new NwavContenidoService();
    private final NwavPdfRenderService nwavRender = new NwavPdfRenderService();

    @TempDir
    Path nwavCarpeta;

    private NwavDocumentoPdf nwavDocumento;

    @AfterEach
    void nwavCerrar() {
        if (nwavDocumento != null) {
            nwavDocumento.close();
        }
    }

    private NwavDocumentoPdf nwavAbrirCon(int nwavPaginas) throws Exception {
        nwavDocumento = nwavDocumentos.nwavAbrir(NwavPdfDePrueba.nwavCrear(nwavCarpeta, "base.pdf", nwavPaginas), null);
        return nwavDocumento;
    }

    private NwavDocumentoPdf nwavAbrirPaginaEnBlanco(int nwavRotacion) throws Exception {
        Path nwavRuta = nwavCarpeta.resolve("blanca-" + nwavRotacion + ".pdf");
        try (PDDocument nwavPd = new PDDocument()) {
            PDPage nwavPagina = new PDPage(PDRectangle.A4);
            nwavPagina.setRotation(nwavRotacion);
            nwavPd.addPage(nwavPagina);
            nwavPd.save(nwavRuta.toFile());
        }
        nwavDocumento = nwavDocumentos.nwavAbrir(nwavRuta, null);
        return nwavDocumento;
    }

    private void nwavEjecutar(NwavComando nwavComando) {
        nwavDocumento.getNwavGestor().nwavEjecutar(nwavComando);
    }

    private List<PDAnnotation> nwavAnotaciones(int nwavPagina) throws Exception {
        return nwavDocumento.nwavPdDocument().getPage(nwavPagina).getAnnotations();
    }

    private static boolean nwavOscuro(BufferedImage nwavImagen, int nwavX, int nwavY) {
        Color nwavColor = new Color(nwavImagen.getRGB(nwavX, nwavY));
        return nwavColor.getRed() + nwavColor.getGreen() + nwavColor.getBlue() < 150;
    }

    // ------------------------------------------------------------- geometria

    @ParameterizedTest
    @ValueSource(ints = {0, 90, 180, 270})
    void nwavLaCoberturaApareceDondeSeVeEnCualquierRotacion(int nwavRotacion) throws Exception {
        nwavAbrirPaginaEnBlanco(nwavRotacion);
        // Esquina superior izquierda tal como se ve en pantalla.
        nwavEjecutar(nwavContenido.nwavComandoCubrir(nwavDocumento, 0, new NwavRectangulo(0, 0, 100, 60),
                NwavColorRgb.NWAV_NEGRO));

        BufferedImage nwavImagen = nwavRender.nwavRenderizarPagina(nwavDocumento, 0, 1f);
        assertThat(nwavOscuro(nwavImagen, 20, 20)).as("arriba a la izquierda").isTrue();
        assertThat(nwavOscuro(nwavImagen, 90, 50)).as("dentro del rectangulo").isTrue();
        assertThat(nwavOscuro(nwavImagen, 110, 20)).as("a la derecha del rectangulo").isFalse();
        assertThat(nwavOscuro(nwavImagen, 20, 70)).as("debajo del rectangulo").isFalse();
        assertThat(nwavOscuro(nwavImagen, nwavImagen.getWidth() - 10, nwavImagen.getHeight() - 10)).isFalse();
    }

    // ----------------------------------------------------------------- texto

    @Test
    void nwavElTextoQuedaEnLaPaginaYSePuedeDeshacer() throws Exception {
        nwavAbrirCon(1);
        NwavGestorComandos nwavGestor = nwavDocumento.getNwavGestor();
        nwavEjecutar(nwavContenido.nwavComandoTexto(nwavDocumento, 0, new NwavRectangulo(72, 300, 0, 0),
                "Hola ñandú\n¿Qué tal? €", NWAV_ESTILO));

        assertThat(NwavLectorPdf.nwavTextos(nwavDocumento.nwavPdDocument()).get(0))
                .contains("Holañandú").contains("¿Quétal?€");

        nwavGestor.nwavDeshacer();
        assertThat(NwavLectorPdf.nwavTextos(nwavDocumento.nwavPdDocument()).get(0)).isEqualTo("Pagina1");
        nwavGestor.nwavRehacer();
        assertThat(NwavLectorPdf.nwavTextos(nwavDocumento.nwavPdDocument()).get(0)).contains("Holañandú");
    }

    @Test
    void nwavEditarInvalidaLaCacheDeLaPagina() throws Exception {
        nwavAbrirCon(1);
        BufferedImage nwavAntes = nwavRender.nwavRenderizarPagina(nwavDocumento, 0, 1f);
        nwavEjecutar(nwavContenido.nwavComandoCubrir(nwavDocumento, 0, new NwavRectangulo(200, 400, 80, 80),
                NwavColorRgb.NWAV_NEGRO));
        BufferedImage nwavDespues = nwavRender.nwavRenderizarPagina(nwavDocumento, 0, 1f);

        assertThat(nwavDespues).isNotSameAs(nwavAntes);
        assertThat(nwavOscuro(nwavDespues, 240, 440)).isTrue();
        nwavDocumento.getNwavGestor().nwavDeshacer();
        assertThat(nwavOscuro(nwavRender.nwavRenderizarPagina(nwavDocumento, 0, 1f), 240, 440)).isFalse();
    }

    @Test
    void nwavUnCaracterSinSoporteDaUnErrorClaro() throws Exception {
        nwavAbrirCon(1);
        assertThatThrownBy(() -> nwavContenido.nwavComandoTexto(nwavDocumento, 0, new NwavRectangulo(10, 10, 0, 0),
                "Hola 漢字", NWAV_ESTILO))
                .isInstanceOf(NwavEditorException.class)
                .hasMessageContaining("«漢»");
        assertThatThrownBy(() -> nwavContenido.nwavComandoTexto(nwavDocumento, 0, new NwavRectangulo(10, 10, 0, 0),
                "   ", NWAV_ESTILO)).hasMessageContaining("Escribe");
    }

    @Test
    void nwavElTextoSeAjustaAlAnchoDelCuadro() {
        var nwavFuente = NwavContenidoService.nwavFuente(NWAV_ESTILO);
        List<String> nwavLineas = NwavContenidoService.nwavPartirLineas(
                "uno dos tres cuatro cinco seis siete ocho\ncorto", nwavFuente, 12, 80);

        assertThat(nwavLineas.size()).isGreaterThan(3);
        assertThat(nwavLineas.get(nwavLineas.size() - 1)).isEqualTo("corto");
        assertThat(String.join(" ", nwavLineas.subList(0, nwavLineas.size() - 1)))
                .isEqualTo("uno dos tres cuatro cinco seis siete ocho");
    }

    // ---------------------------------------------------------- imagen y cubrir

    @Test
    void nwavLaImagenSeDibujaEnLaZonaElegida() throws Exception {
        nwavAbrirCon(1);
        Path nwavPng = NwavLectorPdf.nwavCrearPng(nwavCarpeta, "azul.png", 200, 100);
        nwavEjecutar(nwavContenido.nwavComandoImagen(nwavDocumento, 0, nwavPng, new NwavRectangulo(300, 500, 120, 60)));

        BufferedImage nwavImagen = nwavRender.nwavRenderizarPagina(nwavDocumento, 0, 1f);
        Color nwavCentro = new Color(nwavImagen.getRGB(360, 530));
        assertThat(nwavCentro.getBlue()).isGreaterThan(150);
        assertThat(nwavCentro.getRed()).isLessThan(100);
        assertThat(new Color(nwavImagen.getRGB(290, 530))).isEqualTo(Color.WHITE);
    }

    @Test
    void nwavCubrirEsSoloVisualElTextoSigueAhi() throws Exception {
        nwavAbrirCon(1);
        nwavEjecutar(nwavContenido.nwavComandoCubrir(nwavDocumento, 0, new NwavRectangulo(60, 40, 300, 60),
                NwavColorRgb.NWAV_BLANCO));

        assertThat(NwavLectorPdf.nwavTextos(nwavDocumento.nwavPdDocument()).get(0)).isEqualTo("Pagina1");
    }

    // ----------------------------------------------------------- marcas de texto

    @Test
    void nwavDetectaLasLineasDeTextoDeUnaZona() throws Exception {
        nwavAbrirCon(1);
        // "Pagina 1" esta en x=72, linea base a 842-760 = ~82 pt desde arriba, cuerpo 28.
        List<NwavRectangulo> nwavLineas = nwavContenido.nwavLineasDeTexto(nwavDocumento, 0,
                new NwavRectangulo(40, 40, 400, 70));

        assertThat(nwavLineas).hasSize(1);
        NwavRectangulo nwavLinea = nwavLineas.get(0);
        assertThat(nwavLinea.nwavX()).isCloseTo(72, within(3.0));
        assertThat(nwavLinea.nwavAncho()).isBetween(80.0, 160.0);
        assertThat(nwavLinea.nwavY()).isBetween(50.0, 70.0);
        assertThat(nwavLinea.nwavAbajo()).isBetween(82.0, 95.0);

        assertThat(nwavContenido.nwavLineasDeTexto(nwavDocumento, 0, new NwavRectangulo(40, 400, 300, 100))).isEmpty();
    }

    @Test
    void nwavElResaltadoSeAjustaAlTexto() throws Exception {
        nwavAbrirCon(1);
        nwavEjecutar(nwavContenido.nwavComandoMarca(nwavDocumento, 0, NwavTipoMarca.NWAV_RESALTADO,
                new NwavRectangulo(40, 40, 400, 70), NwavColorRgb.NWAV_AMARILLO));

        List<PDAnnotation> nwavLista = nwavAnotaciones(0);
        assertThat(nwavLista).singleElement().extracting(PDAnnotation::getSubtype).isEqualTo("Highlight");
        float[] nwavCuadros = ((PDAnnotationTextMarkup) nwavLista.get(0)).getQuadPoints();
        assertThat(nwavCuadros).hasSize(8);
        assertThat(nwavCuadros[0]).isCloseTo(72f, within(3f)); // ajustado al texto, no a x=40
        assertThat(nwavLista.get(0).getAppearance()).isNotNull();
    }

    @Test
    void nwavSinTextoLaMarcaUsaLaZonaDibujada() throws Exception {
        nwavAbrirCon(1);
        nwavEjecutar(nwavContenido.nwavComandoMarca(nwavDocumento, 0, NwavTipoMarca.NWAV_TACHADO,
                new NwavRectangulo(100, 500, 200, 20), NwavColorRgb.NWAV_ROJO));

        float[] nwavCuadros = ((PDAnnotationTextMarkup) nwavAnotaciones(0).get(0)).getQuadPoints();
        assertThat(nwavCuadros[0]).isEqualTo(100f);
        assertThat(nwavCuadros[1]).isCloseTo(841.89f - 500f, within(0.1f));
        assertThat(nwavAnotaciones(0).get(0).getSubtype()).isEqualTo("StrikeOut");
    }

    // ----------------------------------------------------- notas, dibujo y formas

    @Test
    void nwavNotasDibujoYFormasSonAnotacionesConApariencia() throws Exception {
        nwavAbrirCon(1);
        nwavEjecutar(nwavContenido.nwavComandoNota(nwavDocumento, 0, new NwavPunto(400, 300), "Revisar esta cifra",
                NwavColorRgb.NWAV_AMARILLO));
        nwavEjecutar(nwavContenido.nwavComandoDibujo(nwavDocumento, 0,
                List.of(new NwavPunto(100, 600), new NwavPunto(150, 620), new NwavPunto(200, 610)),
                NwavColorRgb.NWAV_ROJO, 2));
        nwavEjecutar(nwavContenido.nwavComandoForma(nwavDocumento, 0, NwavTipoForma.NWAV_RECTANGULO,
                new NwavPunto(100, 100), new NwavPunto(200, 180), NwavColorRgb.NWAV_ROJO, 2));
        nwavEjecutar(nwavContenido.nwavComandoForma(nwavDocumento, 0, NwavTipoForma.NWAV_ELIPSE,
                new NwavPunto(250, 100), new NwavPunto(350, 180), NwavColorRgb.NWAV_ROJO, 2));
        nwavEjecutar(nwavContenido.nwavComandoForma(nwavDocumento, 0, NwavTipoForma.NWAV_FLECHA,
                new NwavPunto(100, 700), new NwavPunto(300, 750), NwavColorRgb.NWAV_ROJO, 3));

        List<PDAnnotation> nwavLista = nwavAnotaciones(0);
        assertThat(nwavLista).extracting(PDAnnotation::getSubtype)
                .containsExactly("Text", "Ink", "Square", "Circle", "Line");
        assertThat(nwavLista).allSatisfy(nwavAnotacion ->
                assertThat(nwavAnotacion.getNormalAppearanceStream()).isNotNull());
        assertThat(nwavLista.get(0).getContents()).isEqualTo("Revisar esta cifra");

        // Se dibujan: el rectangulo deja pixeles rojos en su borde.
        BufferedImage nwavImagen = nwavRender.nwavRenderizarPagina(nwavDocumento, 0, 1f);
        Color nwavBorde = new Color(nwavImagen.getRGB(150, 100));
        assertThat(nwavBorde.getRed()).isGreaterThan(150);
        assertThat(nwavBorde.getGreen()).isLessThan(120);
    }

    @Test
    void nwavDeshacerYRehacerAnotacionesYGuardarlas() throws Exception {
        nwavAbrirCon(2);
        NwavGestorComandos nwavGestor = nwavDocumento.getNwavGestor();
        nwavEjecutar(nwavContenido.nwavComandoMarca(nwavDocumento, 1, NwavTipoMarca.NWAV_SUBRAYADO,
                new NwavRectangulo(40, 40, 400, 70), NwavColorRgb.NWAV_ROJO));
        nwavEjecutar(nwavContenido.nwavComandoNota(nwavDocumento, 1, new NwavPunto(50, 50), "otra",
                NwavColorRgb.NWAV_AMARILLO));
        assertThat(nwavAnotaciones(1)).hasSize(2);

        nwavGestor.nwavDeshacer();
        assertThat(nwavAnotaciones(1)).hasSize(1);
        nwavGestor.nwavDeshacer();
        assertThat(nwavAnotaciones(1)).isEmpty();
        nwavGestor.nwavRehacer();
        nwavGestor.nwavRehacer();
        assertThat(nwavAnotaciones(1)).extracting(PDAnnotation::getSubtype).containsExactly("Underline", "Text");

        Path nwavGuardado = nwavCarpeta.resolve("con-anotaciones.pdf");
        synchronized (nwavDocumento) {
            nwavDocumento.nwavPdDocument().save(nwavGuardado.toFile());
        }
        try (PDDocument nwavReabierto = Loader.loadPDF(nwavGuardado.toFile())) {
            assertThat(nwavReabierto.getPage(1).getAnnotations()).hasSize(2);
            assertThat(nwavReabierto.getPage(0).getAnnotations()).isEmpty();
        }
    }

    @Test
    void nwavElComandoDeContenidoNoEsEstructural() throws Exception {
        nwavAbrirCon(1);
        NwavComando nwavComando = nwavContenido.nwavComandoCubrir(nwavDocumento, 0, new NwavRectangulo(1, 1, 5, 5),
                NwavColorRgb.NWAV_BLANCO);
        nwavEjecutar(nwavComando);
        assertThat(nwavComando.nwavCambiaEstructura()).isFalse();
        assertThat(nwavComando.nwavDescripcion()).contains("página 1");
    }

    @Test
    void nwavZonasYTrazosDemasiadoPequenosSeRechazan() throws Exception {
        nwavAbrirCon(1);
        assertThatThrownBy(() -> nwavContenido.nwavComandoCubrir(nwavDocumento, 0, new NwavRectangulo(1, 1, 0.2, 9),
                NwavColorRgb.NWAV_BLANCO)).isInstanceOf(NwavEditorException.class);
        assertThatThrownBy(() -> nwavContenido.nwavComandoDibujo(nwavDocumento, 0, List.of(new NwavPunto(1, 1)),
                NwavColorRgb.NWAV_ROJO, 1)).isInstanceOf(NwavEditorException.class);
    }
}
