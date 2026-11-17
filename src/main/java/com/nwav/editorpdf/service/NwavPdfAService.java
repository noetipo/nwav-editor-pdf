package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.preflight.ValidationResult;
import org.apache.pdfbox.preflight.parser.PreflightParser;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.AdobePDFSchema;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.xml.XmpSerializer;
import org.springframework.stereotype.Service;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NwavPdfAService {
    private final NwavPdfDocumentoService nwavDocumentos;

    public record NwavResultadoPdfA(Path nwavDestino, boolean nwavValido, boolean nwavRasterizado, List<String> nwavMotivos) {
        public String nwavResumen() {
            if (nwavValido && !nwavRasterizado) {
                return "PDF/A-1b válido (texto conservado)";
            }
            if (nwavValido) {
                return "PDF/A-1b válido, con las páginas como imagen (el original no cumplía: "
                        + String.join("; ", nwavMotivos) + ")";
            }
            return "No cumple PDF/A: " + String.join("; ", nwavMotivos);
        }
    }

    public NwavResultadoPdfA nwavConvertir(Path nwavEntrada, String nwavContrasena, boolean nwavRasterizarSiFalla,
                                           Path nwavDestino, NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) {
        Path nwavTemporal = null;
        try {
            nwavTemporal = Files.createTempFile(nwavDestino.toAbsolutePath().getParent(), ".nwav-pdfa", ".pdf");
            nwavAvance.nwavInformar(0, 3, "Preparando el documento…");
            try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
                nwavPd.setAllSecurityToBeRemoved(true);
                nwavLimpiar(nwavPd);
                nwavMarcarPdfA(nwavPd);
                nwavPd.save(nwavTemporal.toFile(), CompressParameters.NO_COMPRESSION);
            }
            nwavCancelacion.nwavVerificar();
            nwavAvance.nwavInformar(1, 3, "Validando PDF/A…");
            List<String> nwavMotivos = nwavValidar(nwavTemporal);
            if (nwavMotivos.isEmpty()) {
                Path nwavConservado = nwavTemporal;
                Path nwavFinal = NwavEscrituraSegura.nwavEscribir(nwavDestino, nwavSalida -> Files.copy(nwavConservado,
                        nwavSalida, java.nio.file.StandardCopyOption.REPLACE_EXISTING));
                return new NwavResultadoPdfA(nwavFinal, true, false, List.of());
            }
            if (!nwavRasterizarSiFalla) {
                return new NwavResultadoPdfA(null, false, false, nwavMotivos);
            }
            nwavAvance.nwavInformar(2, 3, "Rehaciendo las páginas como imagen…");
            nwavRasterizar(nwavEntrada, nwavContrasena, nwavTemporal, nwavCancelacion);
            List<String> nwavMotivosRaster = nwavValidar(nwavTemporal);
            if (!nwavMotivosRaster.isEmpty()) {
                return new NwavResultadoPdfA(null, false, true, nwavMotivosRaster);
            }
            Path nwavOrigen = nwavTemporal;
            Path nwavFinal = NwavEscrituraSegura.nwavEscribir(nwavDestino, nwavSalida -> Files.copy(nwavOrigen, nwavSalida,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING));
            return new NwavResultadoPdfA(nwavFinal, true, true, nwavMotivos);
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        } finally {
            if (nwavTemporal != null) {
                try {
                    Files.deleteIfExists(nwavTemporal);
                } catch (IOException nwavError) {
                }
            }
        }
    }

    public List<String> nwavValidar(Path nwavPdf) throws IOException {
        ValidationResult nwavResultado;
        try {
            nwavResultado = PreflightParser.validate(nwavPdf.toFile());
        } catch (IOException | RuntimeException nwavError) {
            return List.of("El archivo no se pudo analizar como PDF/A (" + nwavError.getMessage() + ")");
        }
        if (nwavResultado.isValid()) {
            return List.of();
        }
        Set<String> nwavMotivos = new LinkedHashSet<>();
        for (ValidationResult.ValidationError nwavError : nwavResultado.getErrorsList()) {
            nwavMotivos.add(nwavMotivo(nwavError.getErrorCode()));
        }
        return new ArrayList<>(nwavMotivos);
    }

    static String nwavMotivo(String nwavCodigo) {
        String nwavGrupo = nwavCodigo == null || nwavCodigo.isEmpty() ? "" : nwavCodigo.substring(0, 1);
        return switch (nwavGrupo) {
            case "1" -> "estructura del archivo no permitida (" + nwavCodigo + ")";
            case "2" -> "gráficos con colores sin perfil o transparencias (" + nwavCodigo + ")";
            case "3" -> "hay fuentes sin incrustar o incompletas (" + nwavCodigo + ")";
            case "4" -> "usa transparencias (" + nwavCodigo + ")";
            case "5" -> "anotaciones no permitidas (" + nwavCodigo + ")";
            case "6" -> "acciones o JavaScript (" + nwavCodigo + ")";
            case "7" -> "metadatos incorrectos (" + nwavCodigo + ")";
            default -> "otro problema (" + nwavCodigo + ")";
        };
    }

    private static void nwavLimpiar(PDDocument nwavPd) throws IOException {
        PDDocumentCatalog nwavCatalogo = nwavPd.getDocumentCatalog();
        PDDocumentNameDictionary nwavNombres = nwavCatalogo.getNames();
        if (nwavNombres != null) {
            nwavNombres.getCOSObject().removeItem(COSName.JAVA_SCRIPT);
            nwavNombres.getCOSObject().removeItem(COSName.EMBEDDED_FILES);
        }
        nwavCatalogo.getCOSObject().removeItem(COSName.AA);
        nwavCatalogo.getCOSObject().removeItem(COSName.OPEN_ACTION);
        nwavCatalogo.getCOSObject().removeItem(COSName.getPDFName("OCProperties"));
        for (PDPage nwavPagina : nwavPd.getPages()) {
            nwavPagina.getCOSObject().removeItem(COSName.AA);
            List<PDAnnotation> nwavValidas = new ArrayList<>();
            for (PDAnnotation nwavAnotacion : nwavPagina.getAnnotations()) {
                String nwavTipo = nwavAnotacion.getSubtype();
                if ("FileAttachment".equals(nwavTipo) || "Sound".equals(nwavTipo) || "Movie".equals(nwavTipo)) {
                    continue;
                }
                nwavAnotacion.setPrinted(true);
                nwavAnotacion.setHidden(false);
                nwavAnotacion.setInvisible(false);
                nwavAnotacion.setNoView(false);
                nwavAnotacion.getCOSObject().removeItem(COSName.CA);
                nwavAnotacion.getCOSObject().removeItem(COSName.A);
                nwavValidas.add(nwavAnotacion);
            }
            nwavPagina.setAnnotations(nwavValidas);
        }
        if (nwavCatalogo.getAcroForm() != null) {
            nwavCatalogo.getAcroForm().setNeedAppearances(false);
            nwavCatalogo.getAcroForm().getCOSObject().removeItem(COSName.getPDFName("XFA"));
        }
    }

    private static void nwavMarcarPdfA(PDDocument nwavPd) throws IOException {
        nwavPd.getDocument().setVersion(1.4f);
        PDDocumentInformation nwavInfo = nwavPd.getDocumentInformation();
        Calendar nwavAhora = Calendar.getInstance();
        nwavInfo.setProducer("NWAV Editor PDF");
        nwavInfo.setModificationDate(nwavAhora);
        if (nwavInfo.getCreationDate() == null) {
            nwavInfo.setCreationDate(nwavAhora);
        }
        COSDictionary nwavDicc = nwavInfo.getCOSObject();
        for (COSName nwavClave : List.copyOf(nwavDicc.keySet())) {
            if (!List.of(COSName.TITLE, COSName.AUTHOR, COSName.SUBJECT, COSName.KEYWORDS, COSName.CREATOR,
                    COSName.PRODUCER, COSName.CREATION_DATE, COSName.MOD_DATE).contains(nwavClave)) {
                nwavDicc.removeItem(nwavClave);
            }
        }

        XMPMetadata nwavXmp = XMPMetadata.createXMPMetadata();
        try {
            DublinCoreSchema nwavDc = nwavXmp.createAndAddDublinCoreSchema();
            if (nwavInfo.getTitle() != null) {
                nwavDc.setTitle(nwavInfo.getTitle());
            }
            if (nwavInfo.getAuthor() != null) {
                nwavDc.addCreator(nwavInfo.getAuthor());
            }
            if (nwavInfo.getSubject() != null) {
                nwavDc.addDescription(null, nwavInfo.getSubject());
            }
            AdobePDFSchema nwavAdobe = nwavXmp.createAndAddAdobePDFSchema();
            nwavAdobe.setProducer(nwavInfo.getProducer());
            if (nwavInfo.getKeywords() != null) {
                nwavAdobe.setKeywords(nwavInfo.getKeywords());
            }
            XMPBasicSchema nwavBasico = nwavXmp.createAndAddXMPBasicSchema();
            nwavBasico.setCreateDate(nwavInfo.getCreationDate());
            nwavBasico.setModifyDate(nwavInfo.getModificationDate());
            if (nwavInfo.getCreator() != null) {
                nwavBasico.setCreatorTool(nwavInfo.getCreator());
            }
            PDFAIdentificationSchema nwavId = nwavXmp.createAndAddPDFAIdentificationSchema();
            nwavId.setPart(1);
            nwavId.setConformance("B");
            ByteArrayOutputStream nwavBytes = new ByteArrayOutputStream();
            new XmpSerializer().serialize(nwavXmp, nwavBytes, true);
            PDMetadata nwavMetadatos = new PDMetadata(nwavPd);
            nwavMetadatos.importXMPMetadata(nwavBytes.toByteArray());
            nwavPd.getDocumentCatalog().setMetadata(nwavMetadatos);
        } catch (org.apache.xmpbox.type.BadFieldValueException | javax.xml.transform.TransformerException nwavError) {
            throw new IOException("No se pudieron crear los metadatos XMP", nwavError);
        }

        PDDocumentCatalog nwavCatalogo = nwavPd.getDocumentCatalog();
        nwavCatalogo.getCOSObject().removeItem(COSName.OUTPUT_INTENTS);
        PDOutputIntent nwavIntencion = new PDOutputIntent(nwavPd,
                new ByteArrayInputStream(ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData()));
        nwavIntencion.setInfo("sRGB IEC61966-2.1");
        nwavIntencion.setOutputCondition("sRGB IEC61966-2.1");
        nwavIntencion.setOutputConditionIdentifier("sRGB IEC61966-2.1");
        nwavIntencion.setRegistryName("http://www.color.org");
        nwavCatalogo.addOutputIntent(nwavIntencion);
    }

    private void nwavRasterizar(Path nwavEntrada, String nwavContrasena, Path nwavSalida, NwavCancelacion nwavCancelacion)
            throws IOException {
        try (PDDocument nwavOrigen = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena);
             PDDocument nwavNuevo = new PDDocument()) {
            PDFRenderer nwavRender = new PDFRenderer(nwavOrigen);
            for (int nwavIndice = 0; nwavIndice < nwavOrigen.getNumberOfPages(); nwavIndice++) {
                nwavCancelacion.nwavVerificar();
                NwavGeometriaPagina nwavGeometria = NwavGeometriaPagina.nwavDe(nwavOrigen.getPage(nwavIndice));
                BufferedImage nwavImagen = nwavRender.renderImageWithDPI(nwavIndice, 200, ImageType.RGB);
                PDPage nwavPagina = new PDPage(new PDRectangle(nwavGeometria.nwavAnchoVisual(), nwavGeometria.nwavAltoVisual()));
                nwavNuevo.addPage(nwavPagina);
                PDImageXObject nwavXObject = JPEGFactory.createFromImage(nwavNuevo, nwavImagen, 0.85f);
                try (PDPageContentStream nwavFlujo = new PDPageContentStream(nwavNuevo, nwavPagina)) {
                    nwavFlujo.drawImage(nwavXObject, 0, 0, nwavGeometria.nwavAnchoVisual(), nwavGeometria.nwavAltoVisual());
                }
            }
            PDDocumentInformation nwavInfoOrigen = nwavOrigen.getDocumentInformation();
            nwavNuevo.getDocumentInformation().setTitle(nwavInfoOrigen.getTitle());
            nwavNuevo.getDocumentInformation().setAuthor(nwavInfoOrigen.getAuthor());
            nwavNuevo.getDocumentInformation().setSubject(nwavInfoOrigen.getSubject());
            nwavMarcarPdfA(nwavNuevo);
            nwavNuevo.save(nwavSalida.toFile(), CompressParameters.NO_COMPRESSION);
        }
    }

    public boolean nwavEsPdfA(Path nwavPdf) {
        try {
            return nwavValidar(nwavPdf).isEmpty();
        } catch (IOException nwavError) {
            throw new NwavEditorException("No se pudo validar «" + nwavPdf.getFileName() + "».", nwavError);
        }
    }
}
