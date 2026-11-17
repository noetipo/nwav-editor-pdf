package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.model.NwavOpcionesProteccion;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.service.NwavFirmaDigitalService.NwavFirmaVerificada;
import com.nwav.editorpdf.service.NwavFirmaDigitalService.NwavOpcionesFirma;
import com.nwav.editorpdf.service.NwavRedaccionService.NwavPatron;
import com.nwav.editorpdf.service.NwavRedaccionService.NwavZona;
import com.nwav.editorpdf.soporte.NwavPdfDePrueba;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class NwavSeguridadAvanzadaTest {
    @TempDir
    Path nwavCarpeta;

    private final NwavPdfDocumentoService nwavDocumentos = new NwavPdfDocumentoService();
    private final NwavFirmaDigitalService nwavFirmas = new NwavFirmaDigitalService(nwavDocumentos);
    private final NwavRedaccionService nwavRedaccion = new NwavRedaccionService(nwavDocumentos);
    private final NwavProteccionLoteService nwavProteccion = new NwavProteccionLoteService(nwavDocumentos);

    private Path nwavCertificado() {
        return nwavFirmas.nwavCrearCertificado("Ana Pérez", "Ejemplo S.L.", "ana@ejemplo.org", "secreta1".toCharArray(), 2,
                nwavCarpeta.resolve("ana.p12"));
    }

    @Test
    void nwavFirmarYVerificarInvisibleYVisible() throws Exception {
        Path nwavP12 = nwavCertificado();
        assertThat(nwavFirmas.nwavInfoCertificado(nwavP12, "secreta1".toCharArray()).nwavTitular()).isEqualTo("Ana Pérez");
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "contrato.pdf", 2);

        Path nwavInvisible = nwavFirmas.nwavFirmar(nwavPdf, null, nwavP12, "secreta1".toCharArray(),
                new NwavOpcionesFirma("Conforme", "Madrid", null, 0, null, null), nwavCarpeta.resolve("firmado.pdf"));
        List<NwavFirmaVerificada> nwavVerificadas = nwavFirmas.nwavVerificar(nwavInvisible, null);
        assertThat(nwavVerificadas).hasSize(1);
        NwavFirmaVerificada nwavFirma = nwavVerificadas.get(0);
        assertThat(nwavFirma.nwavIntegra()).isTrue();
        assertThat(nwavFirma.nwavCubreTodo()).isTrue();
        assertThat(nwavFirma.nwavCertificadoVigente()).isTrue();
        assertThat(nwavFirma.nwavAutofirmado()).isTrue();
        assertThat(nwavFirma.nwavFirmante()).isEqualTo("Ana Pérez");
        assertThat(nwavFirma.nwavMotivo()).isEqualTo("Conforme");

        Path nwavGirado = NwavPdfDePrueba.nwavCrearConRotacion(nwavCarpeta, "girado.pdf", 2, 90);
        byte[] nwavRubrica = new NwavFirmaService(null).nwavFirmaEscrita("Ana", "Serif", null);
        Path nwavVisible = nwavFirmas.nwavFirmar(nwavGirado, null, nwavP12, "secreta1".toCharArray(),
                new NwavOpcionesFirma(null, null, null, 1, new NwavRectangulo(300, 400, 220, 70), nwavRubrica),
                nwavCarpeta.resolve("visible.pdf"));
        assertThat(nwavFirmas.nwavVerificar(nwavVisible, null)).singleElement()
                .satisfies(nwavF -> assertThat(nwavF.nwavValida()).isTrue());
        try (PDDocument nwavPd = Loader.loadPDF(nwavVisible.toFile())) {
            assertThat(nwavPd.getPage(1).getAnnotations()).hasSize(1);
            assertThat(nwavPd.getPage(1).getAnnotations().get(0).getRectangle().getWidth()).isCloseTo(70f, within(0.5f));
        }
    }

    @Test
    void nwavLaVerificacionDetectaCambiosPosteriores() throws Exception {
        Path nwavP12 = nwavCertificado();
        Path nwavFirmado = nwavFirmas.nwavFirmar(NwavPdfDePrueba.nwavCrear(nwavCarpeta, "a.pdf", 1), null, nwavP12,
                "secreta1".toCharArray(), new NwavOpcionesFirma(null, null, null, 0, null, null), nwavCarpeta.resolve("f.pdf"));
        byte[] nwavBytes = Files.readAllBytes(nwavFirmado);

        byte[] nwavAlterado = nwavBytes.clone();
        String nwavTexto = new String(nwavAlterado, StandardCharsets.ISO_8859_1);
        int nwavPos = nwavTexto.indexOf("/MediaBox");
        while (!Character.isDigit(nwavTexto.charAt(nwavPos)) || nwavTexto.charAt(nwavPos) == '0') {
            nwavPos++;
        }
        nwavAlterado[nwavPos] = (byte) (nwavAlterado[nwavPos] == '9' ? '8' : nwavAlterado[nwavPos] + 1);
        Path nwavManipulado = nwavCarpeta.resolve("manipulado.pdf");
        Files.write(nwavManipulado, nwavAlterado);
        assertThat(nwavFirmas.nwavVerificar(nwavManipulado, null)).singleElement()
                .satisfies(nwavF -> assertThat(nwavF.nwavIntegra()).isFalse());

        Path nwavAmpliado = nwavCarpeta.resolve("ampliado.pdf");
        try (PDDocument nwavPd = Loader.loadPDF(nwavFirmado.toFile());
             java.io.OutputStream nwavSalida = Files.newOutputStream(nwavAmpliado)) {
            nwavPd.getDocumentInformation().setTitle("Cambiado");
            nwavPd.getDocumentInformation().getCOSObject().setNeedToBeUpdated(true);
            nwavPd.saveIncremental(nwavSalida);
        }
        assertThat(nwavFirmas.nwavVerificar(nwavAmpliado, null)).singleElement().satisfies(nwavF -> {
            assertThat(nwavF.nwavIntegra()).isTrue();
            assertThat(nwavF.nwavCubreTodo()).isFalse();
        });
    }

    @Test
    void nwavErroresClarosDelCertificado() {
        Path nwavP12 = nwavCertificado();
        assertThatThrownBy(() -> nwavFirmas.nwavInfoCertificado(nwavP12, "mala".toCharArray()))
                .isInstanceOf(NwavEditorException.class).hasMessageContaining("contraseña del certificado");
        assertThatThrownBy(() -> nwavFirmas.nwavCrearCertificado("X", null, null, "123".toCharArray(), 1,
                nwavCarpeta.resolve("x.p12"))).hasMessageContaining("al menos 6");
    }

    private Path nwavPdfConSecretos() throws Exception {
        Path nwavRuta = nwavCarpeta.resolve("secretos.pdf");
        try (PDDocument nwavPd = new PDDocument()) {
            PDPage nwavPagina = new PDPage(PDRectangle.A4);
            nwavPd.addPage(nwavPagina);
            PDType1Font nwavHelvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            BufferedImage nwavFoto = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
            Graphics2D nwavG = nwavFoto.createGraphics();
            nwavG.setColor(Color.YELLOW);
            nwavG.fillRect(0, 0, 200, 100);
            nwavG.dispose();
            PDImageXObject nwavImagen = LosslessFactory.createFromImage(nwavPd, nwavFoto);

            PDFormXObject nwavFormulario = new PDFormXObject(nwavPd);
            nwavFormulario.setBBox(new PDRectangle(0, 0, 300, 40));
            nwavFormulario.setResources(new PDResources());
            try (org.apache.pdfbox.pdmodel.PDFormContentStream nwavFlujoForm = new org.apache.pdfbox.pdmodel.PDFormContentStream(nwavFormulario)) {
                nwavFlujoForm.beginText();
                nwavFlujoForm.setFont(nwavHelvetica, 12);
                nwavFlujoForm.newLineAtOffset(5, 15);
                nwavFlujoForm.showText("CLAVE9 visible fuera");
                nwavFlujoForm.endText();
            }
            try (PDPageContentStream nwavFlujo = new PDPageContentStream(nwavPd, nwavPagina)) {
                nwavFlujo.drawImage(nwavImagen, 72, 560, 200, 100);
                nwavFlujo.beginText();
                nwavFlujo.setFont(nwavHelvetica, 12);
                nwavFlujo.newLineAtOffset(72, 700);
                nwavFlujo.showText("Cuenta: SECRETO 123 y texto público");
                nwavFlujo.newLineAtOffset(0, -20);
                nwavFlujo.showText("Contacto: ana@ejemplo.org, DNI 12345678Z");
                nwavFlujo.endText();
                nwavFlujo.saveGraphicsState();
                nwavFlujo.transform(org.apache.pdfbox.util.Matrix.getTranslateInstance(72, 400));
                nwavFlujo.drawForm(nwavFormulario);
                nwavFlujo.restoreGraphicsState();
            }
            nwavPd.getDocumentInformation().setAuthor("Autor confidencial");
            nwavPd.save(nwavRuta.toFile());
        }
        return nwavRuta;
    }

    private static String nwavTodoLoQueHayEnElArchivo(Path nwavPdf) throws IOException {
        StringBuilder nwavTodo = new StringBuilder();
        try (PDDocument nwavPd = Loader.loadPDF(nwavPdf.toFile())) {
            for (COSObject nwavObjeto : nwavPd.getDocument().getXrefTable().keySet().stream()
                    .map(nwavClave -> nwavPd.getDocument().getObjectFromPool(nwavClave)).toList()) {
                COSBase nwavValor = nwavObjeto.getObject();
                if (nwavValor instanceof COSStream nwavFlujo) {
                    try (InputStream nwavEntrada = nwavFlujo.createInputStream()) {
                        nwavTodo.append(new String(nwavEntrada.readAllBytes(), StandardCharsets.ISO_8859_1));
                    } catch (IOException nwavError) {
                    }
                } else if (nwavValor != null) {
                    nwavTodo.append(nwavValor);
                }
            }
        }
        return nwavTodo.append(new String(Files.readAllBytes(nwavPdf), StandardCharsets.ISO_8859_1)).toString();
    }

    private static boolean nwavAparece(String nwavTodo, String nwavDato) {
        StringBuilder nwavHex = new StringBuilder();
        for (byte nwavByte : nwavDato.getBytes(StandardCharsets.ISO_8859_1)) {
            nwavHex.append(String.format("%02X", nwavByte));
        }
        return nwavTodo.contains(nwavDato) || nwavTodo.toUpperCase(java.util.Locale.ROOT).contains(nwavHex);
    }

    private static float nwavXDe(Path nwavPdf, String nwavPalabra) throws IOException {
        List<Float> nwavPosiciones = new ArrayList<>();
        PDFTextStripper nwavLector = new PDFTextStripper() {
            @Override
            protected void writeString(String nwavTexto, List<TextPosition> nwavLetras) {
                int nwavIndice = nwavTexto.indexOf(nwavPalabra);
                if (nwavIndice >= 0 && nwavPosiciones.isEmpty()) {
                    nwavPosiciones.add(nwavLetras.get(nwavIndice).getXDirAdj());
                }
            }
        };
        try (PDDocument nwavPd = Loader.loadPDF(nwavPdf.toFile())) {
            nwavLector.writeText(nwavPd, new StringWriter());
        }
        return nwavPosiciones.isEmpty() ? -1 : nwavPosiciones.get(0);
    }

    @Test
    void nwavLaRedaccionEliminaDeVerdadElTextoYLosPixeles() throws Exception {
        Path nwavOriginal = nwavPdfConSecretos();
        float nwavXPublicoAntes = nwavXDe(nwavOriginal, "público");
        String nwavAntes = nwavTodoLoQueHayEnElArchivo(nwavOriginal);
        assertThat(nwavAparece(nwavAntes, "SECRETO")).isTrue();
        assertThat(nwavAparece(nwavAntes, "CLAVE9")).isTrue();

        Path nwavRedactado;
        try (PDDocument nwavPd = Loader.loadPDF(nwavOriginal.toFile())) {
            List<NwavZona> nwavZonas = new ArrayList<>(nwavRedaccion.nwavBuscar(nwavPd, List.of("secreto 123"), Set.of(),
                    NwavCancelacion.NWAV_NUNCA));
            assertThat(nwavZonas).hasSize(1);
            nwavZonas.add(new NwavZona(0, new NwavRectangulo(100, 200, 80, 60)));
            nwavZonas.add(new NwavZona(0, new NwavRectangulo(72, 420, 50, 20)));
            NwavRedaccionService.nwavAplicar(nwavPd, nwavZonas, NwavCancelacion.NWAV_NUNCA);
            nwavRedactado = NwavEscrituraSegura.nwavGuardarPdf(nwavPd, nwavCarpeta.resolve("redactado.pdf"));
        }

        String nwavExtraido;
        try (PDDocument nwavPd = Loader.loadPDF(nwavRedactado.toFile())) {
            nwavExtraido = new PDFTextStripper().getText(nwavPd);
        }
        assertThat(nwavExtraido).doesNotContain("SECRETO").doesNotContain("123 y").doesNotContain("CLAVE9")
                .contains("texto público").contains("visible fuera").contains("Contacto");
        String nwavTodo = nwavTodoLoQueHayEnElArchivo(nwavRedactado);
        assertThat(nwavAparece(nwavTodo, "SECRETO")).isFalse();
        assertThat(nwavAparece(nwavTodo, "CLAVE9")).isFalse();
        assertThat(nwavXDe(nwavRedactado, "público")).isCloseTo(nwavXPublicoAntes, within(0.5f));

        try (PDDocument nwavPd = Loader.loadPDF(nwavRedactado.toFile())) {
            BufferedImage nwavRender = new PDFRenderer(nwavPd).renderImage(0, 1f);
            assertThat(new Color(nwavRender.getRGB(140, 230)).getRed()).isLessThan(30);
            assertThat(new Color(nwavRender.getRGB(250, 260)).getBlue()).isLessThan(60);
            assertThat(new Color(nwavRender.getRGB(250, 260)).getRed()).isGreaterThan(200);
            PDResources nwavRecursos = nwavPd.getPage(0).getResources();
            for (COSName nwavNombre : nwavRecursos.getXObjectNames()) {
                if (nwavRecursos.getXObject(nwavNombre) instanceof PDImageXObject nwavImagen) {
                    BufferedImage nwavPixeles = nwavImagen.getImage();
                    assertThat(new Color(nwavPixeles.getRGB(68, 40)).getRed()).as("pixel redactado").isLessThan(10);
                }
            }
        }
    }

    @Test
    void nwavPatronesDeDatosPersonalesYLimpiezaDeMetadatos() throws Exception {
        Path nwavOriginal = nwavPdfConSecretos();
        try (PDDocument nwavPd = Loader.loadPDF(nwavOriginal.toFile())) {
            PDEmbeddedFile nwavAdjunto = new PDEmbeddedFile(nwavPd, new ByteArrayInputStream("oculto".getBytes()));
            PDComplexFileSpecification nwavEspec = new PDComplexFileSpecification();
            nwavEspec.setFile("nota.txt");
            nwavEspec.setEmbeddedFile(nwavAdjunto);
            PDEmbeddedFilesNameTreeNode nwavArbol = new PDEmbeddedFilesNameTreeNode();
            nwavArbol.setNames(Map.of("nota.txt", nwavEspec));
            PDDocumentNameDictionary nwavNombres = new PDDocumentNameDictionary(nwavPd.getDocumentCatalog());
            nwavNombres.setEmbeddedFiles(nwavArbol);
            nwavPd.getDocumentCatalog().setNames(nwavNombres);
            nwavPd.save(nwavOriginal.toFile());
        }
        NwavRedaccionService.NwavResultadoRedaccion nwavResultado = nwavRedaccion.nwavRedactarBusqueda(nwavOriginal, null,
                List.of(), Set.of(NwavPatron.NWAV_CORREOS, NwavPatron.NWAV_DNI), true, nwavCarpeta.resolve("limpio.pdf"),
                NwavCancelacion.NWAV_NUNCA);
        assertThat(nwavResultado.nwavZonas()).isEqualTo(2);
        try (PDDocument nwavPd = Loader.loadPDF(nwavResultado.nwavDestino().toFile())) {
            String nwavTexto = new PDFTextStripper().getText(nwavPd);
            assertThat(nwavTexto).doesNotContain("ana@ejemplo.org").doesNotContain("12345678Z").contains("Contacto:");
            assertThat(nwavPd.getDocumentInformation().getAuthor()).isNull();
            assertThat(nwavPd.getDocumentCatalog().getNames() == null
                    || nwavPd.getDocumentCatalog().getNames().getEmbeddedFiles() == null).isTrue();
        }
        String nwavLimpio = nwavTodoLoQueHayEnElArchivo(nwavResultado.nwavDestino());
        assertThat(nwavAparece(nwavLimpio, "Autor confidencial")).isFalse();
        assertThat(nwavAparece(nwavLimpio, "oculto")).isFalse();
        assertThat(nwavAparece(nwavLimpio, "ana@ejemplo.org")).isFalse();

        assertThatThrownBy(() -> nwavRedaccion.nwavRedactarBusqueda(nwavOriginal, null, List.of("NO-EXISTE"), Set.of(),
                false, nwavCarpeta.resolve("nada.pdf"), NwavCancelacion.NWAV_NUNCA)).hasMessageContaining("ninguna");
    }

    @Test
    void nwavRedactarEnElEditorSePuedeDeshacer() throws Exception {
        Path nwavOriginal = nwavPdfConSecretos();
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavOriginal, null)) {
            List<NwavZona> nwavZonas;
            synchronized (nwavDocumento) {
                nwavZonas = nwavRedaccion.nwavBuscar(nwavDocumento.nwavPdDocument(), List.of("SECRETO"), Set.of(),
                        NwavCancelacion.NWAV_NUNCA);
            }
            nwavDocumento.getNwavGestor().nwavEjecutar(nwavRedaccion.nwavComandoRedactar(nwavDocumento, nwavZonas));
            synchronized (nwavDocumento) {
                assertThat(new PDFTextStripper().getText(nwavDocumento.nwavPdDocument())).doesNotContain("SECRETO");
            }
            nwavDocumento.getNwavGestor().nwavDeshacer();
            synchronized (nwavDocumento) {
                assertThat(new PDFTextStripper().getText(nwavDocumento.nwavPdDocument())).contains("SECRETO");
            }
        }
    }

    @Test
    void nwavProtegerYDesbloquearPorLotes() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "p.pdf", 1);
        Path nwavProtegido = nwavProteccion.nwavProteger(nwavOriginal, null,
                new NwavOpcionesProteccion("abrir1", "dueno1", true, false, false, false, false),
                nwavCarpeta.resolve("protegido.pdf"));
        try (PDDocument nwavPd = Loader.loadPDF(nwavProtegido.toFile(), "abrir1")) {
            assertThat(nwavPd.isEncrypted()).isTrue();
            assertThat(nwavPd.getCurrentAccessPermission().canExtractContent()).isFalse();
        }
        assertThatThrownBy(() -> nwavProteccion.nwavDesbloquear(nwavProtegido, "abrir1", nwavCarpeta.resolve("d.pdf")))
                .hasMessageContaining("propietario");
        Path nwavLibre = nwavProteccion.nwavDesbloquear(nwavProtegido, "dueno1", nwavCarpeta.resolve("libre.pdf"));
        try (PDDocument nwavPd = Loader.loadPDF(nwavLibre.toFile())) {
            assertThat(nwavPd.isEncrypted()).isFalse();
        }
        assertThatThrownBy(() -> nwavProteccion.nwavDesbloquear(nwavLibre, null, nwavCarpeta.resolve("otra.pdf")))
                .hasMessageContaining("no tiene contraseña");
    }

    @Test
    void nwavFirmaEscritaGeneraUnaImagenConTrazo() throws Exception {
        NwavFirmaService nwavServicio = new NwavFirmaService(null);
        assertThat(NwavFirmaService.nwavFuentesManuscritas()).contains("Serif");
        byte[] nwavPng = nwavServicio.nwavFirmaEscrita("Ana Pérez", NwavFirmaService.nwavFuentesManuscritas().get(0), null);
        BufferedImage nwavImagen = ImageIO.read(new ByteArrayInputStream(nwavPng));
        assertThat(nwavImagen.getWidth()).isGreaterThan(nwavImagen.getHeight());
        boolean nwavHayTinta = false;
        boolean nwavHayTransparente = false;
        for (int nwavX = 0; nwavX < nwavImagen.getWidth(); nwavX += 2) {
            for (int nwavY = 0; nwavY < nwavImagen.getHeight(); nwavY += 2) {
                int nwavAlfa = nwavImagen.getRGB(nwavX, nwavY) >>> 24;
                nwavHayTinta |= nwavAlfa > 200;
                nwavHayTransparente |= nwavAlfa == 0;
            }
        }
        assertThat(nwavHayTinta).isTrue();
        assertThat(nwavHayTransparente).isTrue();
        assertThatThrownBy(() -> nwavServicio.nwavFirmaEscrita("  ", "Serif", null)).hasMessageContaining("Escribe");
        assertThat(Calendar.getInstance()).isNotNull();
    }
}
