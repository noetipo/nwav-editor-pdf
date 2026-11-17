package com.nwav.editorpdf.service;

import com.nwav.editorpdf.soporte.NwavPdfConImagenes;
import com.nwav.editorpdf.soporte.NwavPdfDePrueba;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Fase D: convertir desde PDF (imagen, Markdown, Office y PDF/A). */
class NwavConversionDesdePdfTest {

    @TempDir
    Path nwavCarpeta;

    private final NwavPdfDocumentoService nwavDocumentos = new NwavPdfDocumentoService();
    private final NwavConversionDesdePdfService nwavServicio = new NwavConversionDesdePdfService(nwavDocumentos);
    private final NwavPdfAService nwavPdfA = new NwavPdfAService(nwavDocumentos);

    /** PDF con estructura: titulos, parrafo, vinetas, lista numerada, tabla, enlace e imagen. */
    private Path nwavPdfEstructurado(boolean nwavFuenteIncrustada) throws Exception {
        Path nwavRuta = nwavCarpeta.resolve(nwavFuenteIncrustada ? "incrustada.pdf" : "estructurado.pdf");
        try (PDDocument nwavPd = new PDDocument()) {
            PDFont nwavNormal;
            PDFont nwavNegrita;
            if (nwavFuenteIncrustada) {
                try (InputStream nwavTtf = PDDocument.class.getResourceAsStream(
                        "/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")) {
                    nwavNormal = PDType0Font.load(nwavPd, nwavTtf);
                }
                nwavNegrita = nwavNormal;
            } else {
                nwavNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                nwavNegrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            }
            PDPage nwavPagina = new PDPage(PDRectangle.A4);
            nwavPd.addPage(nwavPagina);
            float nwavAlto = PDRectangle.A4.getHeight();
            try (PDPageContentStream nwavFlujo = new PDPageContentStream(nwavPd, nwavPagina)) {
                nwavTexto(nwavFlujo, nwavNegrita, 22, 72, nwavAlto - 80, "Informe trimestral");
                nwavTexto(nwavFlujo, nwavNormal, 11, 72, nwavAlto - 115, "Este documento resume la evolución del negocio");
                nwavTexto(nwavFlujo, nwavNormal, 11, 72, nwavAlto - 129, "durante el trimestre y sus principales cifras.");
                nwavTexto(nwavFlujo, nwavNegrita, 16, 72, nwavAlto - 165, "Resultados");
                nwavTexto(nwavFlujo, nwavNormal, 11, 72, nwavAlto - 190, "• Las ventas crecieron");
                nwavTexto(nwavFlujo, nwavNormal, 11, 72, nwavAlto - 204, "• Los costes bajaron");
                nwavTexto(nwavFlujo, nwavNormal, 11, 72, nwavAlto - 230, "1. Primer objetivo");
                nwavTexto(nwavFlujo, nwavNormal, 11, 72, nwavAlto - 244, "2. Segundo objetivo");
                float nwavY = nwavAlto - 280;
                String[][] nwavTabla = {{"Producto", "Unidades", "Importe"}, {"Mesa", "12", "1.234,50"},
                        {"Silla", "40", "980,00"}};
                for (String[] nwavFila : nwavTabla) {
                    nwavTexto(nwavFlujo, nwavNormal, 11, 72, nwavY, nwavFila[0]);
                    nwavTexto(nwavFlujo, nwavNormal, 11, 250, nwavY, nwavFila[1]);
                    nwavTexto(nwavFlujo, nwavNormal, 11, 400, nwavY, nwavFila[2]);
                    nwavY -= 16;
                }
                nwavTexto(nwavFlujo, nwavNormal, 11, 72, nwavAlto - 360, "Más información: visita la web oficial");
                BufferedImage nwavImagen = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
                Graphics2D nwavG = nwavImagen.createGraphics();
                nwavG.setColor(Color.GREEN);
                nwavG.fillRect(0, 0, 200, 100);
                nwavG.dispose();
                nwavFlujo.drawImage(LosslessFactory.createFromImage(nwavPd, nwavImagen), 72, nwavAlto - 500, 200, 100);
            }
            // enlace sobre "visita la web"
            float nwavInicio = 72 + nwavNormal.getStringWidth("Más información: ") / 1000 * 11;
            float nwavAncho = nwavNormal.getStringWidth("visita la web") / 1000 * 11;
            PDAnnotationLink nwavLink = new PDAnnotationLink();
            nwavLink.setRectangle(new PDRectangle(nwavInicio, nwavAlto - 363, nwavAncho, 13));
            PDActionURI nwavUri = new PDActionURI();
            nwavUri.setURI("https://ejemplo.org/datos");
            nwavLink.setAction(nwavUri);
            nwavPagina.getAnnotations().add(nwavLink);

            PDPage nwavSegunda = new PDPage(PDRectangle.A4);
            nwavPd.addPage(nwavSegunda);
            try (PDPageContentStream nwavFlujo = new PDPageContentStream(nwavPd, nwavSegunda)) {
                nwavTexto(nwavFlujo, nwavNormal, 11, 72, nwavAlto - 80, "Segunda página con un párrafo final.");
            }
            nwavPd.getDocumentInformation().setTitle("Informe");
            nwavPd.save(nwavRuta.toFile());
        }
        return nwavRuta;
    }

    private static void nwavTexto(PDPageContentStream nwavFlujo, PDFont nwavFuente, float nwavTamano, float nwavX, float nwavY,
                                  String nwavTexto) throws Exception {
        nwavFlujo.beginText();
        nwavFlujo.setFont(nwavFuente, nwavTamano);
        nwavFlujo.newLineAtOffset(nwavX, nwavY);
        nwavFlujo.showText(nwavTexto);
        nwavFlujo.endText();
    }

    @Test
    void nwavPaginasAJpgYPng() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "tres.pdf", 3);
        List<Path> nwavJpg = nwavServicio.nwavAImagenes(nwavPdf, null, "jpg", 72, 0.8f, "1, 3", null,
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);
        assertThat(nwavJpg).extracting(nwavRuta -> nwavRuta.getFileName().toString())
                .containsExactly("tres_p01.jpg", "tres_p03.jpg");
        BufferedImage nwavLeida = ImageIO.read(nwavJpg.get(0).toFile());
        assertThat(nwavLeida.getWidth()).isEqualTo(595); // A4 a 72 ppp

        List<Path> nwavPng = nwavServicio.nwavAImagenes(nwavPdf, null, "png", 150, 1f, "", null,
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);
        assertThat(nwavPng).hasSize(3);
        assertThat(ImageIO.read(nwavPng.get(1).toFile()).getWidth()).isEqualTo(1240);
    }

    @Test
    void nwavExtraerImagenesSinRepetirYConJpegOriginal() throws Exception {
        BufferedImage nwavFoto = NwavPdfConImagenes.nwavFoto(300, 200, false);
        Path nwavJpeg = nwavCarpeta.resolve("foto.jpg");
        ImageIO.write(nwavFoto, "jpg", nwavJpeg.toFile());
        Path nwavPdf = nwavCarpeta.resolve("fotos.pdf");
        try (PDDocument nwavPd = new PDDocument()) {
            org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject nwavJpg =
                    org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory.createFromStream(nwavPd, Files.newInputStream(nwavJpeg));
            for (int nwavIndice = 0; nwavIndice < 2; nwavIndice++) {
                PDPage nwavPagina = new PDPage(PDRectangle.A4);
                nwavPd.addPage(nwavPagina);
                try (PDPageContentStream nwavFlujo = new PDPageContentStream(nwavPd, nwavPagina)) {
                    nwavFlujo.drawImage(nwavJpg, 50, 400, 300, 200); // mismo objeto en las dos paginas
                    // mismo PNG pero como objetos distintos en cada pagina
                    nwavFlujo.drawImage(LosslessFactory.createFromImage(nwavPd, nwavFoto), 50, 100, 150, 100);
                    nwavFlujo.drawImage(LosslessFactory.createFromImage(nwavPd, new BufferedImage(10, 10,
                            BufferedImage.TYPE_INT_RGB)), 400, 100, 10, 10); // diminuta: se omite
                }
            }
            nwavPd.save(nwavPdf.toFile());
        }
        List<Path> nwavImagenes = nwavServicio.nwavExtraerImagenes(nwavPdf, null, 32, null, NwavAvance.NWAV_NINGUNO,
                NwavCancelacion.NWAV_NUNCA);

        assertThat(nwavImagenes).extracting(nwavRuta -> nwavRuta.getFileName().toString())
                .containsExactly("fotos_p1_img1.jpg", "fotos_p1_img2.png");
        assertThat(Files.readAllBytes(nwavImagenes.get(0))).isEqualTo(Files.readAllBytes(nwavJpeg)); // sin recomprimir
        assertThat(ImageIO.read(nwavImagenes.get(1).toFile()).getWidth()).isEqualTo(300);

        Path nwavSinImagenes = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "texto.pdf", 1);
        assertThatThrownBy(() -> nwavServicio.nwavExtraerImagenes(nwavSinImagenes, null, 1, null, NwavAvance.NWAV_NINGUNO,
                NwavCancelacion.NWAV_NUNCA)).hasMessageContaining("no tiene imágenes");
    }

    @Test
    void nwavMarkdownConservaTitulosListasTablasYEnlaces() throws Exception {
        Path nwavPdf = nwavPdfEstructurado(false);
        Path nwavMd = nwavServicio.nwavAMarkdown(nwavPdf, null, nwavCarpeta.resolve("salida.md"), NwavCancelacion.NWAV_NUNCA);
        String nwavTexto = Files.readString(nwavMd, StandardCharsets.UTF_8);

        assertThat(nwavTexto).contains("# Informe trimestral\n")
                .contains("## Resultados\n")
                .contains("Este documento resume la evolución del negocio durante el trimestre y sus principales cifras.")
                .contains("- Las ventas crecieron\n- Los costes bajaron\n")
                .contains("1. Primer objetivo\n2. Segundo objetivo\n")
                .contains("| Producto | Unidades | Importe |\n| --- | --- | --- |\n| Mesa | 12 | 1.234,50 |")
                .contains("[visita la web](https://ejemplo.org/datos)")
                .contains("Segunda página con un párrafo final.");
    }

    @Test
    void nwavTextoPlanoYAvisoSinTexto() throws Exception {
        Path nwavTxt = nwavServicio.nwavATexto(nwavPdfEstructurado(false), null, nwavCarpeta.resolve("t.txt"));
        assertThat(Files.readString(nwavTxt)).contains("Informe trimestral").contains("\f");

        Path nwavEscaneo = NwavPdfConImagenes.nwavCrearEscaneado(nwavCarpeta, "escaneo.pdf", "HOLA");
        assertThatThrownBy(() -> nwavServicio.nwavATexto(nwavEscaneo, null, nwavCarpeta.resolve("e.txt")))
                .hasMessageContaining("OCR");
    }

    @Test
    void nwavWordConTitulosTablaEnlaceEImagen() throws Exception {
        Path nwavDocx = nwavServicio.nwavAWord(nwavPdfEstructurado(false), null, true, nwavCarpeta.resolve("s.docx"),
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);
        try (XWPFDocument nwavWord = new XWPFDocument(Files.newInputStream(nwavDocx))) {
            List<String> nwavParrafos = nwavWord.getParagraphs().stream().map(XWPFParagraph::getText).toList();
            assertThat(nwavParrafos).contains("Informe trimestral", "Resultados");
            assertThat(nwavParrafos).anyMatch(nwavP -> nwavP.contains("Las ventas crecieron"));
            XWPFParagraph nwavTitulo = nwavWord.getParagraphs().stream()
                    .filter(nwavP -> nwavP.getText().equals("Informe trimestral")).findFirst().orElseThrow();
            assertThat(nwavTitulo.getRuns().get(0).isBold()).isTrue();
            assertThat(nwavWord.getTables()).hasSize(1);
            assertThat(nwavWord.getTables().get(0).getRow(1).getCell(2).getText()).isEqualTo("1.234,50");
            assertThat(nwavWord.getHyperlinks()).extracting(nwavH -> nwavH.getURL()).contains("https://ejemplo.org/datos");
            assertThat(nwavWord.getAllPictures()).hasSize(1);
        }
    }

    @Test
    void nwavExcelConNumerosReales() throws Exception {
        Path nwavXlsx = nwavServicio.nwavAExcel(nwavPdfEstructurado(false), null, false, true, nwavCarpeta.resolve("s.xlsx"),
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);
        try (XSSFWorkbook nwavLibro = new XSSFWorkbook(Files.newInputStream(nwavXlsx))) {
            assertThat(nwavLibro.getNumberOfSheets()).isEqualTo(1); // la pagina 2 no tiene tablas
            XSSFSheet nwavHoja = nwavLibro.getSheetAt(0);
            assertThat(nwavHoja.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Producto");
            assertThat(nwavHoja.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(12);
            assertThat(nwavHoja.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(1234.5);
            assertThat(nwavHoja.getRow(2).getCell(2).getNumericCellValue()).isEqualTo(980);
        }
        assertThat(NwavConversionDesdePdfService.nwavNumero("1,234.56")).isEqualByComparingTo(new BigDecimal("1234.56"));
        assertThat(NwavConversionDesdePdfService.nwavNumero("(15,5 %)")).isEqualByComparingTo(new BigDecimal("-15.5"));
        assertThat(NwavConversionDesdePdfService.nwavNumero("1.234.567")).isEqualByComparingTo(new BigDecimal("1234567"));
        assertThat(NwavConversionDesdePdfService.nwavNumero("ABC-12")).isNull();
    }

    @Test
    void nwavPowerPointEditableYComoImagen() throws Exception {
        Path nwavPdf = nwavPdfEstructurado(false);
        Path nwavEditable = nwavServicio.nwavAPowerPoint(nwavPdf, null, true, nwavCarpeta.resolve("e.pptx"),
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);
        try (XMLSlideShow nwavPpt = new XMLSlideShow(Files.newInputStream(nwavEditable))) {
            assertThat(nwavPpt.getSlides()).hasSize(2);
            assertThat(nwavPpt.getPageSize().getWidth()).isEqualTo(595);
            List<String> nwavTextos = nwavPpt.getSlides().get(0).getShapes().stream()
                    .filter(XSLFTextShape.class::isInstance).map(nwavS -> ((XSLFTextShape) nwavS).getText()).toList();
            assertThat(nwavTextos).contains("Informe trimestral", "Resultados");
            assertThat(nwavPpt.getSlides().get(0).getShapes()).anyMatch(XSLFPictureShape.class::isInstance);
        }
        Path nwavImagen = nwavServicio.nwavAPowerPoint(nwavPdf, null, false, nwavCarpeta.resolve("i.pptx"),
                NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);
        try (XMLSlideShow nwavPpt = new XMLSlideShow(Files.newInputStream(nwavImagen))) {
            assertThat(nwavPpt.getSlides().get(0).getShapes()).hasSize(1);
        }
    }

    @Test
    void nwavPdfAConservandoElTextoCuandoLasFuentesEstanIncrustadas() throws Exception {
        Path nwavPdf = nwavPdfEstructurado(true);
        NwavPdfAService.NwavResultadoPdfA nwavResultado = nwavPdfA.nwavConvertir(nwavPdf, null, false,
                nwavCarpeta.resolve("a.pdf"), NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);

        assertThat(nwavResultado.nwavMotivos()).isEmpty();
        assertThat(nwavResultado.nwavValido()).isTrue();
        assertThat(nwavResultado.nwavRasterizado()).isFalse();
        assertThat(nwavPdfA.nwavValidar(nwavResultado.nwavDestino())).isEmpty();
        try (PDDocument nwavPd = Loader.loadPDF(nwavResultado.nwavDestino().toFile())) {
            assertThat(new PDFTextStripper().getText(nwavPd)).contains("Informe trimestral");
        }
        assertThat(nwavPdfA.nwavValidar(nwavPdf)).isNotEmpty(); // el original no era PDF/A
    }

    @Test
    void nwavPdfAExplicaLosMotivosYPuedeRasterizar() throws Exception {
        Path nwavPdf = nwavPdfEstructurado(false); // Helvetica sin incrustar
        NwavPdfAService.NwavResultadoPdfA nwavSoloInforme = nwavPdfA.nwavConvertir(nwavPdf, null, false,
                nwavCarpeta.resolve("no.pdf"), NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);
        assertThat(nwavSoloInforme.nwavValido()).isFalse();
        assertThat(nwavSoloInforme.nwavDestino()).isNull();
        assertThat(nwavCarpeta.resolve("no.pdf")).doesNotExist();
        assertThat(nwavSoloInforme.nwavMotivos()).anyMatch(nwavM -> nwavM.contains("fuentes"));

        NwavPdfAService.NwavResultadoPdfA nwavRaster = nwavPdfA.nwavConvertir(nwavPdf, null, true,
                nwavCarpeta.resolve("si.pdf"), NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);
        assertThat(nwavRaster.nwavValido()).isTrue();
        assertThat(nwavRaster.nwavRasterizado()).isTrue();
        assertThat(nwavPdfA.nwavValidar(nwavRaster.nwavDestino())).isEmpty();
        try (PDDocument nwavPd = Loader.loadPDF(nwavRaster.nwavDestino().toFile())) {
            assertThat(nwavPd.getNumberOfPages()).isEqualTo(2);
        }
    }
}
