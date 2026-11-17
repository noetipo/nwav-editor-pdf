package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.comando.NwavComando;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import org.apache.pdfbox.contentstream.operator.text.BeginText;
import org.apache.pdfbox.contentstream.operator.text.EndText;
import org.apache.pdfbox.contentstream.operator.text.MoveText;
import org.apache.pdfbox.contentstream.operator.text.MoveTextSetLeading;
import org.apache.pdfbox.contentstream.operator.text.NextLine;
import org.apache.pdfbox.contentstream.operator.text.SetCharSpacing;
import org.apache.pdfbox.contentstream.operator.text.SetFontAndSize;
import org.apache.pdfbox.contentstream.operator.text.SetTextHorizontalScaling;
import org.apache.pdfbox.contentstream.operator.text.SetTextLeading;
import org.apache.pdfbox.contentstream.operator.text.SetTextRenderingMode;
import org.apache.pdfbox.contentstream.operator.text.SetTextRise;
import org.apache.pdfbox.contentstream.operator.text.SetWordSpacing;
import org.apache.pdfbox.contentstream.operator.text.ShowText;
import org.apache.pdfbox.contentstream.operator.text.ShowTextAdjusted;
import org.apache.pdfbox.contentstream.operator.text.ShowTextLine;
import org.apache.pdfbox.contentstream.operator.text.ShowTextLineAndSpace;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;
import org.springframework.stereotype.Service;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NwavRedaccionService {
    private final NwavPdfDocumentoService nwavDocumentos;

    public record NwavZona(int nwavPagina, NwavRectangulo nwavRectangulo) {
    }

    public enum NwavPatron {
        NWAV_CORREOS("Correos electrónicos", "[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+"),
        NWAV_TELEFONOS("Teléfonos", "(?<![\\w])(?:\\+\\d{1,3}[\\s.-]?)?(?:\\(?\\d{2,4}\\)?[\\s.-]?){2,4}\\d{2,4}(?![\\w])"),
        NWAV_DNI("DNI / NIE", "\\b[XYZxyz]?\\d{7,8}[-\\s]?[A-Za-z]\\b"),
        NWAV_IBAN("Cuentas IBAN", "\\b[A-Z]{2}\\d{2}(?:[\\s]?[A-Z0-9]{4}){3,7}(?:[\\s]?[A-Z0-9]{1,4})?\\b"),
        NWAV_TARJETAS("Tarjetas bancarias", "\\b(?:\\d[ -]?){13,18}\\d\\b");

        private final String nwavDescripcion;
        private final Pattern nwavExpresion;

        NwavPatron(String nwavDescripcion, String nwavExpresion) {
            this.nwavDescripcion = nwavDescripcion;
            this.nwavExpresion = Pattern.compile(nwavExpresion);
        }

        public String getNwavDescripcion() {
            return nwavDescripcion;
        }

        Pattern nwavExpresion() {
            return nwavExpresion;
        }
    }

    public record NwavResultadoRedaccion(Path nwavDestino, int nwavZonas, int nwavPaginas) {
    }

    public NwavResultadoRedaccion nwavRedactarBusqueda(Path nwavEntrada, String nwavContrasena, List<String> nwavTerminos,
                                                       Set<NwavPatron> nwavPatrones, boolean nwavLimpiar, Path nwavDestino,
                                                       NwavCancelacion nwavCancelacion) {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            List<NwavZona> nwavZonas = nwavBuscar(nwavPd, nwavTerminos, nwavPatrones, nwavCancelacion);
            if (nwavZonas.isEmpty() && !nwavLimpiar) {
                throw new NwavEditorException("No se encontró ninguna coincidencia que redactar.");
            }
            nwavAplicar(nwavPd, nwavZonas, nwavCancelacion);
            if (nwavLimpiar) {
                nwavLimpiarDatosOcultos(nwavPd);
            }
            NwavEscrituraSegura.nwavMantenerProteccion(nwavPd, nwavContrasena);
            Path nwavGuardado = NwavEscrituraSegura.nwavGuardarPdf(nwavPd, nwavDestino);
            return new NwavResultadoRedaccion(nwavGuardado, nwavZonas.size(),
                    (int) nwavZonas.stream().mapToInt(NwavZona::nwavPagina).distinct().count());
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    public Path nwavLimpiarArchivo(Path nwavEntrada, String nwavContrasena, Path nwavDestino) {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            nwavLimpiarDatosOcultos(nwavPd);
            NwavEscrituraSegura.nwavMantenerProteccion(nwavPd, nwavContrasena);
            return NwavEscrituraSegura.nwavGuardarPdf(nwavPd, nwavDestino);
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    public NwavComando nwavComandoRedactar(NwavDocumentoPdf nwavDocumento, List<NwavZona> nwavZonas) {
        if (nwavZonas == null || nwavZonas.isEmpty()) {
            throw new NwavEditorException("Marca al menos una zona.");
        }
        List<NwavZona> nwavCopia = List.copyOf(nwavZonas);
        return new NwavComando() {
            private final Map<PDPage, COSBase[]> nwavAntes = new LinkedHashMap<>();
            private COSArray nwavCamposAntes;

            @Override
            public void nwavEjecutar() {
                synchronized (nwavDocumento) {
                    nwavDocumento.nwavVerificarAbierto();
                    PDDocument nwavPd = nwavDocumento.nwavPdDocument();
                    nwavAntes.clear();
                    for (int nwavIndice : nwavCopia.stream().map(NwavZona::nwavPagina).distinct().toList()) {
                        PDPage nwavPagina = nwavPd.getPage(nwavIndice);
                        COSDictionary nwavDicc = nwavPagina.getCOSObject();
                        nwavAntes.put(nwavPagina, new COSBase[]{nwavDicc.getItem(COSName.CONTENTS),
                                nwavDicc.getItem(COSName.RESOURCES), nwavDicc.getItem(COSName.ANNOTS)});
                    }
                    PDAcroForm nwavFormulario = nwavPd.getDocumentCatalog().getAcroForm();
                    nwavCamposAntes = nwavFormulario == null ? null
                            : nwavCopiarArray(nwavFormulario.getCOSObject().getCOSArray(COSName.FIELDS));
                    try {
                        nwavAplicar(nwavPd, nwavCopia, NwavCancelacion.NWAV_NUNCA);
                    } catch (IOException nwavError) {
                        nwavDeshacer();
                        throw new NwavEditorException("No se pudo redactar: " + nwavError.getMessage(), nwavError);
                    }
                    nwavAntes.keySet().forEach(nwavP -> nwavDocumento.nwavMarcarModificada(nwavP.getCOSObject()));
                }
            }

            @Override
            public void nwavDeshacer() {
                synchronized (nwavDocumento) {
                    nwavAntes.forEach((nwavPagina, nwavItems) -> {
                        COSDictionary nwavDicc = nwavPagina.getCOSObject();
                        nwavPoner(nwavDicc, COSName.CONTENTS, nwavItems[0]);
                        nwavPoner(nwavDicc, COSName.RESOURCES, nwavItems[1]);
                        nwavPoner(nwavDicc, COSName.ANNOTS, nwavItems[2]);
                        nwavDocumento.nwavMarcarModificada(nwavDicc);
                    });
                    PDAcroForm nwavFormulario = nwavDocumento.nwavPdDocument().getDocumentCatalog().getAcroForm();
                    if (nwavFormulario != null && nwavCamposAntes != null) {
                        nwavFormulario.getCOSObject().setItem(COSName.FIELDS, nwavCamposAntes);
                    }
                }
            }

            @Override
            public NwavTipoOperacion nwavTipo() {
                return NwavTipoOperacion.NWAV_REDACTAR;
            }

            @Override
            public String nwavDescripcion() {
                return nwavCopia.size() == 1 ? "Redactar una zona" : "Redactar " + nwavCopia.size() + " zonas";
            }

            @Override
            public int nwavPaginaDestacada() {
                return nwavCopia.get(0).nwavPagina();
            }

            @Override
            public boolean nwavCambiaEstructura() {
                return false;
            }
        };
    }

    private static void nwavPoner(COSDictionary nwavDicc, COSName nwavClave, COSBase nwavValor) {
        if (nwavValor == null) {
            nwavDicc.removeItem(nwavClave);
        } else {
            nwavDicc.setItem(nwavClave, nwavValor);
        }
    }

    private static COSArray nwavCopiarArray(COSArray nwavOrigen) {
        if (nwavOrigen == null) {
            return null;
        }
        COSArray nwavCopia = new COSArray();
        for (int nwavIndice = 0; nwavIndice < nwavOrigen.size(); nwavIndice++) {
            nwavCopia.add(nwavOrigen.get(nwavIndice));
        }
        return nwavCopia;
    }

    public List<NwavZona> nwavBuscar(PDDocument nwavPd, List<String> nwavTerminos, Set<NwavPatron> nwavPatrones,
                                     NwavCancelacion nwavCancelacion) throws IOException {
        List<Pattern> nwavExpresiones = new ArrayList<>();
        if (nwavTerminos != null) {
            for (String nwavTermino : nwavTerminos) {
                if (nwavTermino != null && !nwavTermino.isBlank()) {
                    nwavExpresiones.add(Pattern.compile(Pattern.quote(nwavTermino.strip()),
                            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
                }
            }
        }
        if (nwavPatrones != null) {
            nwavPatrones.forEach(nwavP -> nwavExpresiones.add(nwavP.nwavExpresion()));
        }
        if (nwavExpresiones.isEmpty()) {
            return List.of();
        }
        NwavLector nwavLector = new NwavLector();
        List<NwavZona> nwavZonas = new ArrayList<>();
        for (int nwavIndice = 0; nwavIndice < nwavPd.getNumberOfPages(); nwavIndice++) {
            nwavCancelacion.nwavVerificar();
            nwavLector.nwavLeer(nwavPd, nwavIndice);
            String nwavTexto = nwavLector.nwavTexto.toString();
            for (Pattern nwavExpresion : nwavExpresiones) {
                Matcher nwavCoincidencia = nwavExpresion.matcher(nwavTexto);
                while (nwavCoincidencia.find()) {
                    for (NwavRectangulo nwavZona : nwavLector.nwavZonas(nwavCoincidencia.start(), nwavCoincidencia.end())) {
                        nwavZonas.add(new NwavZona(nwavIndice, nwavZona));
                    }
                }
            }
        }
        return nwavZonas;
    }

    private static final class NwavLector extends PDFTextStripper {
        private final StringBuilder nwavTexto = new StringBuilder();
        private final List<TextPosition> nwavPosiciones = new ArrayList<>();

        NwavLector() {
            setSortByPosition(true);
        }

        void nwavLeer(PDDocument nwavPd, int nwavIndice) throws IOException {
            nwavTexto.setLength(0);
            nwavPosiciones.clear();
            setStartPage(nwavIndice + 1);
            setEndPage(nwavIndice + 1);
            writeText(nwavPd, new StringWriter());
        }

        @Override
        protected void writeString(String nwavCadena, List<TextPosition> nwavLetras) {
            for (TextPosition nwavLetra : nwavLetras) {
                String nwavUnicode = nwavLetra.getUnicode();
                if (nwavUnicode == null) {
                    continue;
                }
                for (int nwavI = 0; nwavI < nwavUnicode.length(); nwavI++) {
                    nwavTexto.append(nwavUnicode.charAt(nwavI));
                    nwavPosiciones.add(nwavLetra);
                }
            }
        }

        @Override
        protected void writeWordSeparator() {
            nwavTexto.append(' ');
            nwavPosiciones.add(null);
        }

        @Override
        protected void writeLineSeparator() {
            nwavTexto.append('\n');
            nwavPosiciones.add(null);
        }

        List<NwavRectangulo> nwavZonas(int nwavInicio, int nwavFin) {
            Map<Integer, double[]> nwavPorLinea = new LinkedHashMap<>();
            for (int nwavI = nwavInicio; nwavI < nwavFin && nwavI < nwavPosiciones.size(); nwavI++) {
                TextPosition nwavLetra = nwavPosiciones.get(nwavI);
                if (nwavLetra == null) {
                    continue;
                }
                double nwavAlto = Math.max(nwavLetra.getHeightDir(), nwavLetra.getFontSizeInPt() * 0.8);
                double nwavX1 = nwavLetra.getXDirAdj();
                double nwavX2 = nwavX1 + nwavLetra.getWidthDirAdj();
                double nwavY2 = nwavLetra.getYDirAdj() + nwavLetra.getFontSizeInPt() * 0.25;
                double nwavY1 = nwavLetra.getYDirAdj() - nwavAlto;
                nwavPorLinea.merge(Math.round(nwavLetra.getYDirAdj()), new double[]{nwavX1, nwavY1, nwavX2, nwavY2},
                        (nwavA, nwavB) -> new double[]{Math.min(nwavA[0], nwavB[0]), Math.min(nwavA[1], nwavB[1]),
                                Math.max(nwavA[2], nwavB[2]), Math.max(nwavA[3], nwavB[3])});
            }
            return nwavPorLinea.values().stream().map(nwavR -> new NwavRectangulo(nwavR[0] - 1, nwavR[1] - 1,
                    nwavR[2] - nwavR[0] + 2, nwavR[3] - nwavR[1] + 2)).toList();
        }
    }

    static void nwavAplicar(PDDocument nwavPd, List<NwavZona> nwavZonas, NwavCancelacion nwavCancelacion)
            throws IOException {
        Map<Integer, List<NwavRectangulo>> nwavPorPagina = nwavZonas.stream().collect(Collectors.groupingBy(
                NwavZona::nwavPagina, LinkedHashMap::new, Collectors.mapping(NwavZona::nwavRectangulo, Collectors.toList())));
        for (Map.Entry<Integer, List<NwavRectangulo>> nwavEntrada : nwavPorPagina.entrySet()) {
            nwavCancelacion.nwavVerificar();
            if (nwavEntrada.getKey() < 0 || nwavEntrada.getKey() >= nwavPd.getNumberOfPages()) {
                continue;
            }
            nwavRedactarPagina(nwavPd, nwavPd.getPage(nwavEntrada.getKey()), nwavEntrada.getValue());
        }
    }

    private static void nwavRedactarPagina(PDDocument nwavPd, PDPage nwavPagina, List<NwavRectangulo> nwavVisuales)
            throws IOException {
        NwavGeometriaPagina nwavGeometria = NwavGeometriaPagina.nwavDe(nwavPagina);
        List<Rectangle2D> nwavZonas = new ArrayList<>();
        for (NwavRectangulo nwavVisual : nwavVisuales) {
            PDRectangle nwavCaja = nwavGeometria.nwavRectanguloPdf(nwavVisual);
            nwavZonas.add(new Rectangle2D.Float(nwavCaja.getLowerLeftX(), nwavCaja.getLowerLeftY(), nwavCaja.getWidth(),
                    nwavCaja.getHeight()));
        }
        PDResources nwavRecursos = new PDResources(nwavClonarRecursos(nwavPagina.getResources()));
        NwavReescritor nwavReescritor = new NwavReescritor(nwavPd, nwavZonas, nwavRecursos);
        nwavReescritor.processPage(nwavPagina);
        List<Object> nwavTokens = nwavReescritor.nwavSalida.pop();

        PDStream nwavNuevo = new PDStream(nwavPd);
        try (OutputStream nwavSalida = nwavNuevo.createOutputStream(COSName.FLATE_DECODE)) {
            ContentStreamWriter nwavEscritor = new ContentStreamWriter(nwavSalida);
            nwavEscritor.writeTokens(Operator.getOperator("q"));
            nwavEscritor.writeTokens(nwavTokens);
            nwavEscritor.writeTokens(Operator.getOperator("Q"));
            nwavEscritor.writeTokens(COSFloat.ZERO, COSFloat.ZERO, COSFloat.ZERO, Operator.getOperator("rg"));
            for (Rectangle2D nwavZona : nwavZonas) {
                nwavEscritor.writeTokens(new COSFloat((float) nwavZona.getX()), new COSFloat((float) nwavZona.getY()),
                        new COSFloat((float) nwavZona.getWidth()), new COSFloat((float) nwavZona.getHeight()),
                        Operator.getOperator("re"), Operator.getOperator("f"));
            }
        }
        nwavPagina.setContents(nwavNuevo);
        nwavPodarXObjetos(nwavRecursos, nwavTokens);
        nwavPagina.setResources(nwavRecursos);

        List<PDAnnotation> nwavQuedan = new ArrayList<>();
        Set<COSDictionary> nwavWidgetsQuitados = new HashSet<>();
        for (PDAnnotation nwavAnotacion : nwavPagina.getAnnotations()) {
            PDRectangle nwavRect = nwavAnotacion.getRectangle();
            boolean nwavToca = nwavRect != null && nwavZonas.stream().anyMatch(nwavZona -> nwavZona.intersects(
                    nwavRect.getLowerLeftX(), nwavRect.getLowerLeftY(), nwavRect.getWidth(), nwavRect.getHeight()));
            if (nwavToca) {
                if (nwavAnotacion instanceof PDAnnotationWidget) {
                    nwavWidgetsQuitados.add(nwavAnotacion.getCOSObject());
                }
            } else {
                nwavQuedan.add(nwavAnotacion);
            }
        }
        nwavPagina.setAnnotations(nwavQuedan);
        if (!nwavWidgetsQuitados.isEmpty()) {
            nwavQuitarCampos(nwavPd, nwavWidgetsQuitados);
        }
    }

    private static void nwavQuitarCampos(PDDocument nwavPd, Set<COSDictionary> nwavWidgets) {
        PDAcroForm nwavFormulario = nwavPd.getDocumentCatalog().getAcroForm();
        if (nwavFormulario == null) {
            return;
        }
        List<PDField> nwavRaiz = new ArrayList<>(nwavFormulario.getFields());
        nwavRaiz.removeIf(nwavCampo -> nwavFiltrar(nwavCampo, nwavWidgets));
        nwavFormulario.setFields(nwavRaiz);
    }

    private static boolean nwavFiltrar(PDField nwavCampo, Set<COSDictionary> nwavWidgets) {
        if (nwavCampo instanceof PDNonTerminalField nwavPadre) {
            List<PDField> nwavHijos = new ArrayList<>(nwavPadre.getChildren());
            nwavHijos.removeIf(nwavHijo -> nwavFiltrar(nwavHijo, nwavWidgets));
            nwavPadre.setChildren(nwavHijos);
            return nwavHijos.isEmpty();
        }
        return nwavCampo.getWidgets().stream().anyMatch(nwavW -> nwavWidgets.contains(nwavW.getCOSObject()));
    }

    private static void nwavPodarXObjetos(PDResources nwavRecursos, List<Object> nwavTokens) {
        COSDictionary nwavXObjetos = nwavRecursos.getCOSObject().getCOSDictionary(COSName.XOBJECT);
        if (nwavXObjetos == null) {
            return;
        }
        Set<COSName> nwavUsados = new HashSet<>();
        for (int nwavIndice = 1; nwavIndice < nwavTokens.size(); nwavIndice++) {
            if (nwavTokens.get(nwavIndice) instanceof Operator nwavOp && "Do".equals(nwavOp.getName())
                    && nwavTokens.get(nwavIndice - 1) instanceof COSName nwavNombre) {
                nwavUsados.add(nwavNombre);
            }
        }
        for (COSName nwavNombre : List.copyOf(nwavXObjetos.keySet())) {
            if (!nwavUsados.contains(nwavNombre)) {
                nwavXObjetos.removeItem(nwavNombre);
            }
        }
    }

    private static COSDictionary nwavClonarRecursos(PDResources nwavRecursos) {
        COSDictionary nwavCopia = new COSDictionary();
        if (nwavRecursos == null) {
            return nwavCopia;
        }
        COSDictionary nwavOrigen = nwavRecursos.getCOSObject();
        for (COSName nwavClave : nwavOrigen.keySet()) {
            COSBase nwavValor = nwavOrigen.getDictionaryObject(nwavClave);
            if (COSName.XOBJECT.equals(nwavClave) && nwavValor instanceof COSDictionary nwavXObjetos) {
                COSDictionary nwavCopiaX = new COSDictionary();
                for (COSName nwavNombre : nwavXObjetos.keySet()) {
                    nwavCopiaX.setItem(nwavNombre, nwavXObjetos.getItem(nwavNombre));
                }
                nwavCopia.setItem(nwavClave, nwavCopiaX);
            } else {
                nwavCopia.setItem(nwavClave, nwavOrigen.getItem(nwavClave));
            }
        }
        return nwavCopia;
    }

    private static final class NwavReescritor extends PDFStreamEngine {
        private final PDDocument nwavPd;
        private final List<Rectangle2D> nwavZonas;
        private final Deque<List<Object>> nwavSalida = new ArrayDeque<>();
        private final Deque<PDResources> nwavDestinos = new ArrayDeque<>();
        private List<NwavGlifo> nwavGlifos;

        private record NwavGlifo(boolean nwavDentro, float nwavAjuste) {
        }

        NwavReescritor(PDDocument nwavPd, List<Rectangle2D> nwavZonas, PDResources nwavRecursosPagina) {
            this.nwavPd = nwavPd;
            this.nwavZonas = nwavZonas;
            nwavSalida.push(new ArrayList<>());
            nwavDestinos.push(nwavRecursosPagina);
            addOperator(new BeginText(this));
            addOperator(new EndText(this));
            addOperator(new SetFontAndSize(this));
            addOperator(new SetCharSpacing(this));
            addOperator(new SetWordSpacing(this));
            addOperator(new SetTextHorizontalScaling(this));
            addOperator(new SetTextLeading(this));
            addOperator(new SetTextRise(this));
            addOperator(new SetTextRenderingMode(this));
            addOperator(new MoveText(this));
            addOperator(new MoveTextSetLeading(this));
            addOperator(new NextLine(this));
            addOperator(new ShowText(this));
            addOperator(new ShowTextAdjusted(this));
            addOperator(new ShowTextLine(this));
            addOperator(new ShowTextLineAndSpace(this));
            addOperator(new Concatenate(this));
            addOperator(new Save(this));
            addOperator(new Restore(this));
            addOperator(new SetMatrix(this));
            addOperator(new SetGraphicsStateParameters(this));
        }

        private List<Object> nwavActual() {
            return nwavSalida.peek();
        }

        @Override
        protected void processOperator(Operator nwavOperador, List<COSBase> nwavOperandos) throws IOException {
            String nwavNombre = nwavOperador.getName();
            switch (nwavNombre) {
                case "Tj", "TJ", "'", "\"" -> nwavTexto(nwavOperador, nwavOperandos);
                case "Do" -> nwavDibujarObjeto(nwavOperador, nwavOperandos);
                case "BI" -> {
                    if (!nwavToca(nwavCajaUnidad())) {
                        nwavCopiar(nwavOperador, nwavOperandos);
                    }
                }
                default -> {
                    nwavCopiar(nwavOperador, nwavOperandos);
                    super.processOperator(nwavOperador, nwavOperandos);
                }
            }
        }

        private void nwavCopiar(Operator nwavOperador, List<COSBase> nwavOperandos) {
            nwavActual().addAll(nwavOperandos);
            nwavActual().add(nwavOperador);
        }

        private void nwavTexto(Operator nwavOperador, List<COSBase> nwavOperandos) throws IOException {
            PDFont nwavFuente = getGraphicsState().getTextState().getFont();
            float nwavTamano = getGraphicsState().getTextState().getFontSize();
            nwavGlifos = new ArrayList<>();
            super.processOperator(nwavOperador, nwavOperandos);
            List<NwavGlifo> nwavMedidos = nwavGlifos;
            nwavGlifos = null;
            if (nwavFuente == null || nwavMedidos.stream().noneMatch(NwavGlifo::nwavDentro)) {
                nwavCopiar(nwavOperador, nwavOperandos);
                return;
            }
            List<COSBase> nwavElementos = new ArrayList<>();
            String nwavNombre = nwavOperador.getName();
            if ("TJ".equals(nwavNombre)) {
                ((COSArray) nwavOperandos.get(0)).forEach(nwavElementos::add);
            } else {
                nwavElementos.add(nwavOperandos.get(nwavOperandos.size() - 1));
            }
            COSArray nwavNuevo = new COSArray();
            int nwavGlifo = 0;
            for (COSBase nwavElemento : nwavElementos) {
                if (nwavElemento instanceof COSNumber) {
                    nwavNuevo.add(nwavElemento);
                    continue;
                }
                if (!(nwavElemento instanceof COSString nwavCadena)) {
                    continue;
                }
                byte[] nwavBytes = nwavCadena.getBytes();
                ByteArrayInputStream nwavEntrada = new ByteArrayInputStream(nwavBytes);
                java.io.ByteArrayOutputStream nwavConservados = new java.io.ByteArrayOutputStream();
                while (nwavEntrada.available() > 0) {
                    int nwavAntes = nwavEntrada.available();
                    nwavFuente.readCode(nwavEntrada);
                    int nwavLongitud = nwavAntes - nwavEntrada.available();
                    int nwavInicio = nwavBytes.length - nwavAntes;
                    NwavGlifo nwavMedido = nwavGlifo < nwavMedidos.size() ? nwavMedidos.get(nwavGlifo) : null;
                    nwavGlifo++;
                    if (nwavMedido != null && nwavMedido.nwavDentro()) {
                        if (nwavConservados.size() > 0) {
                            nwavNuevo.add(new COSString(nwavConservados.toByteArray()));
                            nwavConservados.reset();
                        }
                        nwavNuevo.add(new COSFloat(nwavMedido.nwavAjuste()));
                    } else {
                        nwavConservados.write(nwavBytes, nwavInicio, nwavLongitud);
                    }
                }
                if (nwavConservados.size() > 0) {
                    nwavNuevo.add(new COSString(nwavConservados.toByteArray()));
                }
            }
            if ("'".equals(nwavNombre)) {
                nwavActual().add(Operator.getOperator("T*"));
            } else if ("\"".equals(nwavNombre)) {
                nwavActual().add(nwavOperandos.get(0));
                nwavActual().add(Operator.getOperator("Tw"));
                nwavActual().add(nwavOperandos.get(1));
                nwavActual().add(Operator.getOperator("Tc"));
                nwavActual().add(Operator.getOperator("T*"));
            }
            if (nwavTamano == 0) {
                return;
            }
            nwavActual().add(nwavNuevo);
            nwavActual().add(Operator.getOperator("TJ"));
        }

        @Override
        protected void showGlyph(Matrix nwavTrm, PDFont nwavFuente, int nwavCodigo, Vector nwavDesplazamiento) {
            if (nwavGlifos == null) {
                return;
            }
            var nwavEstado = getGraphicsState().getTextState();
            float nwavTamano = nwavEstado.getFontSize();
            boolean nwavEspacio = nwavCodigo == 32 && nwavFuente.isStandard14()
                    || nwavCodigo == 32 && !(nwavFuente instanceof org.apache.pdfbox.pdmodel.font.PDType0Font);
            float nwavAvance = nwavDesplazamiento.getX() * nwavTamano + nwavEstado.getCharacterSpacing()
                    + (nwavEspacio ? nwavEstado.getWordSpacing() : 0);
            float nwavAjuste = nwavTamano == 0 ? 0 : -nwavAvance * 1000 / nwavTamano;
            AffineTransform nwavT = nwavTrm.createAffineTransform();
            float nwavAncho = Math.max(nwavDesplazamiento.getX(), 0.2f);
            Rectangle2D nwavCaja = nwavT.createTransformedShape(new Rectangle2D.Float(0, -0.2f, nwavAncho, 1f)).getBounds2D();
            double nwavCx = nwavCaja.getCenterX();
            double nwavCy = nwavCaja.getCenterY();
            boolean nwavDentro = nwavZonas.stream().anyMatch(nwavZona -> nwavZona.contains(nwavCx, nwavCy)
                    || nwavZona.intersects(nwavCaja) && nwavArea(nwavZona.createIntersection(nwavCaja))
                    >= nwavArea(nwavCaja) * 0.3);
            nwavGlifos.add(new NwavGlifo(nwavDentro, nwavAjuste));
        }

        private static double nwavArea(Rectangle2D nwavR) {
            return Math.max(0, nwavR.getWidth()) * Math.max(0, nwavR.getHeight());
        }

        private Rectangle2D nwavCajaUnidad() {
            return getGraphicsState().getCurrentTransformationMatrix().createAffineTransform()
                    .createTransformedShape(new Rectangle2D.Float(0, 0, 1, 1)).getBounds2D();
        }

        private boolean nwavToca(Rectangle2D nwavCaja) {
            return nwavZonas.stream().anyMatch(nwavZona -> nwavZona.intersects(nwavCaja));
        }

        private void nwavDibujarObjeto(Operator nwavOperador, List<COSBase> nwavOperandos) throws IOException {
            if (nwavOperandos.isEmpty() || !(nwavOperandos.get(0) instanceof COSName nwavNombre) || getResources() == null) {
                nwavCopiar(nwavOperador, nwavOperandos);
                return;
            }
            PDXObject nwavObjeto = getResources().getXObject(nwavNombre);
            if (nwavObjeto instanceof PDImageXObject nwavImagen) {
                Rectangle2D nwavCaja = nwavCajaUnidad();
                if (!nwavToca(nwavCaja)) {
                    nwavCopiar(nwavOperador, nwavOperandos);
                    return;
                }
                if (nwavImagen.isStencil()) {
                    return;
                }
                PDImageXObject nwavRedactada = nwavRedactarImagen(nwavImagen);
                COSName nwavNuevoNombre = nwavDestinos.peek().add(nwavRedactada, "Red");
                nwavActual().add(nwavNuevoNombre);
                nwavActual().add(nwavOperador);
                return;
            }
            if (nwavObjeto instanceof PDFormXObject nwavFormulario) {
                PDRectangle nwavBBox = nwavFormulario.getBBox();
                Rectangle2D nwavCaja = nwavBBox == null ? null : nwavFormularioEnPagina(nwavFormulario, nwavBBox);
                if (nwavCaja != null && !nwavToca(nwavCaja)) {
                    nwavCopiar(nwavOperador, nwavOperandos);
                    return;
                }
                PDResources nwavRecursosCopia = new PDResources(nwavClonarRecursos(nwavFormulario.getResources()));
                nwavSalida.push(new ArrayList<>());
                nwavDestinos.push(nwavRecursosCopia);
                try {
                    showForm(nwavFormulario);
                } finally {
                    nwavDestinos.pop();
                }
                List<Object> nwavTokens = nwavSalida.pop();
                PDFormXObject nwavCopia = nwavCopiarFormulario(nwavFormulario, nwavTokens, nwavRecursosCopia);
                COSName nwavNuevoNombre = nwavDestinos.peek().add(nwavCopia, "RedF");
                nwavActual().add(nwavNuevoNombre);
                nwavActual().add(nwavOperador);
                return;
            }
            nwavCopiar(nwavOperador, nwavOperandos);
        }

        private Rectangle2D nwavFormularioEnPagina(PDFormXObject nwavFormulario, PDRectangle nwavBBox) {
            AffineTransform nwavT = getGraphicsState().getCurrentTransformationMatrix().createAffineTransform();
            nwavT.concatenate(nwavFormulario.getMatrix().createAffineTransform());
            return nwavT.createTransformedShape(new Rectangle2D.Float(nwavBBox.getLowerLeftX(), nwavBBox.getLowerLeftY(),
                    nwavBBox.getWidth(), nwavBBox.getHeight())).getBounds2D();
        }

        private PDFormXObject nwavCopiarFormulario(PDFormXObject nwavOriginal, List<Object> nwavTokens,
                                                   PDResources nwavRecursos) throws IOException {
            PDStream nwavFlujo = new PDStream(nwavPd);
            try (OutputStream nwavSalidaFlujo = nwavFlujo.createOutputStream(COSName.FLATE_DECODE)) {
                new ContentStreamWriter(nwavSalidaFlujo).writeTokens(nwavTokens);
            }
            COSStream nwavNuevo = nwavFlujo.getCOSObject();
            COSDictionary nwavDicc = nwavOriginal.getCOSObject();
            for (COSName nwavClave : nwavDicc.keySet()) {
                if (!COSName.LENGTH.equals(nwavClave) && !COSName.FILTER.equals(nwavClave)
                        && !COSName.DECODE_PARMS.equals(nwavClave) && !COSName.RESOURCES.equals(nwavClave)) {
                    nwavNuevo.setItem(nwavClave, nwavDicc.getItem(nwavClave));
                }
            }
            PDFormXObject nwavCopia = new PDFormXObject(nwavNuevo);
            nwavPodarXObjetos(nwavRecursos, nwavTokens);
            nwavCopia.setResources(nwavRecursos);
            return nwavCopia;
        }

        private PDImageXObject nwavRedactarImagen(PDImageXObject nwavImagen) throws IOException {
            BufferedImage nwavPixeles = nwavImagen.getImage();
            BufferedImage nwavCopia = new BufferedImage(nwavPixeles.getWidth(), nwavPixeles.getHeight(),
                    nwavPixeles.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            Graphics2D nwavG = nwavCopia.createGraphics();
            nwavG.drawImage(nwavPixeles, 0, 0, null);
            nwavG.setColor(java.awt.Color.BLACK);
            AffineTransform nwavAPagina = getGraphicsState().getCurrentTransformationMatrix().createAffineTransform();
            AffineTransform nwavAPixeles = new AffineTransform(nwavPixeles.getWidth(), 0, 0, -nwavPixeles.getHeight(), 0,
                    nwavPixeles.getHeight());
            try {
                AffineTransform nwavInversa = nwavAPagina.createInverse();
                for (Rectangle2D nwavZona : nwavZonas) {
                    java.awt.Shape nwavEnUnidad = nwavInversa.createTransformedShape(nwavZona);
                    nwavG.fill(nwavAPixeles.createTransformedShape(nwavEnUnidad));
                }
            } catch (NoninvertibleTransformException nwavError) {
                nwavG.fillRect(0, 0, nwavCopia.getWidth(), nwavCopia.getHeight());
            }
            nwavG.dispose();
            return LosslessFactory.createFromImage(nwavPd, nwavCopia);
        }
    }

    public static void nwavLimpiarDatosOcultos(PDDocument nwavPd) {
        PDDocumentInformation nwavLimpia = new PDDocumentInformation();
        nwavLimpia.setProducer("NWAV Editor PDF");
        nwavPd.setDocumentInformation(nwavLimpia);
        PDDocumentCatalog nwavCatalogo = nwavPd.getDocumentCatalog();
        nwavCatalogo.setMetadata(null);
        nwavCatalogo.getCOSObject().removeItem(COSName.PIECE_INFO);
        nwavCatalogo.getCOSObject().removeItem(COSName.AA);
        nwavCatalogo.getCOSObject().removeItem(COSName.OPEN_ACTION);
        if (nwavCatalogo.getNames() != null) {
            nwavCatalogo.getNames().getCOSObject().removeItem(COSName.JAVA_SCRIPT);
            nwavCatalogo.getNames().getCOSObject().removeItem(COSName.EMBEDDED_FILES);
        }
        for (PDPage nwavPagina : nwavPd.getPages()) {
            nwavPagina.getCOSObject().removeItem(COSName.METADATA);
            nwavPagina.getCOSObject().removeItem(COSName.PIECE_INFO);
            nwavPagina.getCOSObject().removeItem(COSName.getPDFName("Thumb"));
            nwavPagina.getCOSObject().removeItem(COSName.AA);
        }
    }
}
