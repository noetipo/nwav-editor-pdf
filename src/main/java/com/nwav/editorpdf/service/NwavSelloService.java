package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavPosicionMarca;
import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.model.NwavAlineacion;
import com.nwav.editorpdf.model.NwavEstiloTexto;
import com.nwav.editorpdf.model.NwavOpcionesEncabezado;
import com.nwav.editorpdf.model.NwavOpcionesMarcaAgua;
import com.nwav.editorpdf.model.NwavTipoMarcaAgua;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.comando.NwavComando;
import com.nwav.editorpdf.service.comando.NwavComandoVariasPaginas;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class NwavSelloService {
    public static final String NWAV_MARCA_NUMERO = "{n}";
    public static final String NWAV_MARCA_TOTAL = "{total}";
    public static final String NWAV_MARCA_ARCHIVO = "{archivo}";
    public static final String NWAV_MARCA_FECHA = "{fecha}";
    private static final DateTimeFormatter NWAV_FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NwavContenidoService nwavContenido;

    public NwavComando nwavComandoMarcaAgua(NwavDocumentoPdf nwavDocumento, NwavOpcionesMarcaAgua nwavOpciones) {
        List<Integer> nwavPaginas = nwavPaginasElegidas(nwavDocumento, nwavOpciones.nwavPaginas());
        boolean nwavEsTexto = nwavOpciones.nwavTipo() == NwavTipoMarcaAgua.NWAV_TEXTO;
        if (nwavEsTexto) {
            String nwavTexto = nwavLimpiarLinea(nwavOpciones.nwavTexto());
            if (nwavTexto.isEmpty()) {
                throw new NwavEditorException("Escribe el texto de la marca de agua.");
            }
            nwavValidar(nwavOpciones.nwavFuente(), nwavTexto);
        } else if (nwavOpciones.nwavImagen() == null || nwavOpciones.nwavImagen().length == 0) {
            throw new NwavEditorException("Elige la imagen de la marca de agua.");
        }
        return new NwavComandoVariasPaginas(nwavContenido, nwavDocumento, nwavPaginas, NwavTipoOperacion.NWAV_MARCA_AGUA,
                nwavEsTexto ? "Marca de agua «" + nwavLimpiarLinea(nwavOpciones.nwavTexto()) + "»" : "Marca de agua (imagen)",
                () -> nwavAplicarMarcaAgua(nwavDocumento, nwavPaginas, nwavOpciones));
    }

    private static void nwavAplicarMarcaAgua(NwavDocumentoPdf nwavDocumento, List<Integer> nwavPaginas,
                                             NwavOpcionesMarcaAgua nwavOpciones) {
        PDExtendedGraphicsState nwavTransparencia = new PDExtendedGraphicsState();
        nwavTransparencia.setNonStrokingAlphaConstant((float) nwavOpciones.nwavOpacidad());
        nwavTransparencia.setStrokingAlphaConstant((float) nwavOpciones.nwavOpacidad());
        float nwavAngulo = (float) Math.toRadians(nwavOpciones.nwavRotacion());

        if (nwavOpciones.nwavTipo() == NwavTipoMarcaAgua.NWAV_IMAGEN) {
            PDImageXObject nwavImagen;
            synchronized (nwavDocumento) {
                try {
                    nwavImagen = PDImageXObject.createFromByteArray(nwavDocumento.nwavPdDocument(),
                            nwavOpciones.nwavImagen(), "nwav-marca");
                } catch (IOException | IllegalArgumentException nwavError) {
                    throw new NwavEditorException("La imagen de la marca de agua no es compatible.", nwavError);
                }
            }
            for (int nwavPagina : nwavPaginas) {
                NwavContenidoService.nwavDibujarEnPagina(nwavDocumento, nwavPagina, (nwavFlujo, nwavGeometria) -> {
                    float nwavAncho = (float) (nwavGeometria.nwavAnchoVisual() * nwavOpciones.nwavEscala());
                    float nwavAlto = nwavAncho * nwavImagen.getHeight() / Math.max(1f, nwavImagen.getWidth());
                    nwavFlujo.setGraphicsStateParameters(nwavTransparencia);
                    for (float[] nwavCentro : nwavCentros(nwavOpciones.nwavPosicion(), nwavGeometria.nwavAnchoVisual(),
                            nwavGeometria.nwavAltoVisual(), nwavAncho, nwavAlto, nwavAngulo)) {
                        nwavFlujo.saveGraphicsState();
                        nwavFlujo.transform(Matrix.getRotateInstance(nwavAngulo, nwavCentro[0], nwavCentro[1]));
                        nwavFlujo.drawImage(nwavImagen, -nwavAncho / 2, -nwavAlto / 2, nwavAncho, nwavAlto);
                        nwavFlujo.restoreGraphicsState();
                    }
                });
            }
            return;
        }

        PDType1Font nwavFuente = NwavContenidoService.nwavFuente(
                new NwavEstiloTexto(nwavOpciones.nwavFuente(), nwavOpciones.nwavTamano(), true, false, null));
        String nwavTexto = nwavLimpiarLinea(nwavOpciones.nwavTexto());
        float nwavTamano = (float) nwavOpciones.nwavTamano();
        float nwavAnchoTexto = nwavAncho(nwavFuente, nwavTamano, nwavTexto);
        float[] nwavColor = nwavOpciones.nwavColor().nwavComponentes();
        for (int nwavPagina : nwavPaginas) {
            NwavContenidoService.nwavDibujarEnPagina(nwavDocumento, nwavPagina, (nwavFlujo, nwavGeometria) -> {
                nwavFlujo.setGraphicsStateParameters(nwavTransparencia);
                nwavFlujo.setNonStrokingColor(nwavColor[0], nwavColor[1], nwavColor[2]);
                for (float[] nwavCentro : nwavCentros(nwavOpciones.nwavPosicion(), nwavGeometria.nwavAnchoVisual(),
                        nwavGeometria.nwavAltoVisual(), nwavAnchoTexto, nwavTamano, nwavAngulo)) {
                    Matrix nwavMatriz = Matrix.getRotateInstance(nwavAngulo, nwavCentro[0], nwavCentro[1]);
                    nwavMatriz.translate(-nwavAnchoTexto / 2, -nwavTamano * 0.35f);
                    nwavFlujo.beginText();
                    nwavFlujo.setFont(nwavFuente, nwavTamano);
                    nwavFlujo.setTextMatrix(nwavMatriz);
                    nwavFlujo.showText(nwavTexto);
                    nwavFlujo.endText();
                }
            });
        }
    }

    static List<float[]> nwavCentros(NwavPosicionMarca nwavPosicion, float nwavAnchoPagina, float nwavAltoPagina,
                                     float nwavAncho, float nwavAlto, float nwavAngulo) {
        double nwavCos = Math.abs(Math.cos(nwavAngulo));
        double nwavSen = Math.abs(Math.sin(nwavAngulo));
        float nwavCajaAncho = (float) (nwavAncho * nwavCos + nwavAlto * nwavSen);
        float nwavCajaAlto = (float) (nwavAncho * nwavSen + nwavAlto * nwavCos);
        List<float[]> nwavCentros = new java.util.ArrayList<>();
        if (nwavPosicion == NwavPosicionMarca.NWAV_MOSAICO) {
            float nwavPasoX = nwavCajaAncho + Math.max(36, nwavCajaAncho * 0.35f);
            float nwavPasoY = nwavCajaAlto + Math.max(36, nwavCajaAlto * 0.6f);
            int nwavFila = 0;
            for (float nwavY = nwavAltoPagina - nwavCajaAlto / 2; nwavY > -nwavCajaAlto / 2; nwavY -= nwavPasoY, nwavFila++) {
                float nwavDesfase = nwavFila % 2 == 0 ? 0 : nwavPasoX / 2;
                for (float nwavX = nwavCajaAncho / 2 - nwavDesfase; nwavX < nwavAnchoPagina + nwavCajaAncho / 2;
                     nwavX += nwavPasoX) {
                    nwavCentros.add(new float[]{nwavX, nwavY});
                }
                if (nwavCentros.size() > 400) {
                    break;
                }
            }
            return nwavCentros;
        }
        float nwavMargen = 28;
        float nwavMitadAncho = Math.min(nwavCajaAncho / 2, nwavAnchoPagina / 2);
        float nwavMitadAlto = Math.min(nwavCajaAlto / 2, nwavAltoPagina / 2);
        float nwavX = (float) (nwavMargen + nwavMitadAncho
                + (nwavAnchoPagina - 2 * (nwavMargen + nwavMitadAncho)) * nwavPosicion.getNwavFraccionX());
        float nwavYAbajo = (float) (nwavMargen + nwavMitadAlto
                + (nwavAltoPagina - 2 * (nwavMargen + nwavMitadAlto)) * nwavPosicion.getNwavFraccionY());
        if (nwavPosicion == NwavPosicionMarca.NWAV_CENTRO) {
            nwavX = nwavAnchoPagina / 2;
            nwavYAbajo = nwavAltoPagina / 2;
        }
        nwavCentros.add(new float[]{nwavX, nwavAltoPagina - nwavYAbajo});
        return nwavCentros;
    }

    public NwavComando nwavComandoEncabezado(NwavDocumentoPdf nwavDocumento, NwavOpcionesEncabezado nwavOpciones) {
        if (nwavOpciones.nwavEncabezado().isEmpty() && nwavOpciones.nwavPie().isEmpty()) {
            throw new NwavEditorException("Escribe el texto del encabezado o del pie.");
        }
        List<Integer> nwavPaginas = nwavPaginasElegidas(nwavDocumento, nwavOpciones.nwavPaginas());
        int nwavTotal = nwavDocumento.getNwavNumeroPaginas() + nwavOpciones.nwavNumeroInicial() - 1;
        String nwavArchivo = nwavDocumento.getNwavNombre();
        LocalDate nwavHoy = LocalDate.now();
        nwavValidar(nwavOpciones.nwavFuente(), nwavSustituir(nwavOpciones.nwavEncabezado() + " " + nwavOpciones.nwavPie(),
                nwavTotal, nwavTotal, nwavArchivo, nwavHoy));
        String nwavDescripcion = nwavOpciones.nwavPie().contains(NWAV_MARCA_NUMERO)
                || nwavOpciones.nwavEncabezado().contains(NWAV_MARCA_NUMERO)
                ? "Numerar páginas" : "Encabezado y pie";
        return new NwavComandoVariasPaginas(nwavContenido, nwavDocumento, nwavPaginas, NwavTipoOperacion.NWAV_ENCABEZADO,
                nwavDescripcion, () -> {
            PDType1Font nwavFuente = NwavContenidoService.nwavFuente(
                    new NwavEstiloTexto(nwavOpciones.nwavFuente(), nwavOpciones.nwavTamano(), false, false, null));
            float nwavTamano = (float) nwavOpciones.nwavTamano();
            float nwavMargen = (float) nwavOpciones.nwavMargen();
            float[] nwavColor = nwavOpciones.nwavColor().nwavComponentes();
            for (int nwavPagina : nwavPaginas) {
                int nwavNumero = nwavPagina + nwavOpciones.nwavNumeroInicial();
                String nwavArriba = nwavSustituir(nwavOpciones.nwavEncabezado(), nwavNumero, nwavTotal, nwavArchivo, nwavHoy);
                String nwavAbajo = nwavSustituir(nwavOpciones.nwavPie(), nwavNumero, nwavTotal, nwavArchivo, nwavHoy);
                NwavContenidoService.nwavDibujarEnPagina(nwavDocumento, nwavPagina, (nwavFlujo, nwavGeometria) -> {
                    nwavFlujo.setNonStrokingColor(nwavColor[0], nwavColor[1], nwavColor[2]);
                    float nwavAnchoPagina = nwavGeometria.nwavAnchoVisual();
                    if (!nwavArriba.isEmpty()) {
                        float nwavY = nwavGeometria.nwavAltoVisual() - nwavMargen - nwavTamano * 0.8f;
                        nwavEscribir(nwavFlujo, nwavFuente, nwavTamano, nwavArriba, nwavOpciones.nwavAlineacionEncabezado(),
                                nwavAnchoPagina, nwavMargen, nwavY);
                    }
                    if (!nwavAbajo.isEmpty()) {
                        nwavEscribir(nwavFlujo, nwavFuente, nwavTamano, nwavAbajo, nwavOpciones.nwavAlineacionPie(),
                                nwavAnchoPagina, nwavMargen, nwavMargen);
                    }
                });
            }
        });
    }

    private static void nwavEscribir(org.apache.pdfbox.pdmodel.PDPageContentStream nwavFlujo, PDType1Font nwavFuente,
                                     float nwavTamano, String nwavTexto, NwavAlineacion nwavAlineacion,
                                     float nwavAnchoPagina, float nwavMargen, float nwavY) throws IOException {
        float nwavAnchoTexto = nwavAncho(nwavFuente, nwavTamano, nwavTexto);
        float nwavX = switch (nwavAlineacion) {
            case NWAV_IZQUIERDA -> nwavMargen;
            case NWAV_CENTRO -> (nwavAnchoPagina - nwavAnchoTexto) / 2;
            case NWAV_DERECHA -> nwavAnchoPagina - nwavMargen - nwavAnchoTexto;
        };
        nwavFlujo.beginText();
        nwavFlujo.setFont(nwavFuente, nwavTamano);
        nwavFlujo.newLineAtOffset(nwavX, nwavY);
        nwavFlujo.showText(nwavTexto);
        nwavFlujo.endText();
    }

    public static String nwavSustituir(String nwavPlantilla, int nwavNumero, int nwavTotal, String nwavArchivo,
                                       LocalDate nwavFecha) {
        return nwavLimpiarLinea(nwavPlantilla)
                .replace(NWAV_MARCA_NUMERO, String.valueOf(nwavNumero))
                .replace(NWAV_MARCA_TOTAL, String.valueOf(nwavTotal))
                .replace(NWAV_MARCA_ARCHIVO, nwavArchivo == null ? "" : nwavArchivo)
                .replace(NWAV_MARCA_FECHA, NWAV_FORMATO_FECHA.format(nwavFecha));
    }

    private static List<Integer> nwavPaginasElegidas(NwavDocumentoPdf nwavDocumento, String nwavRangos) {
        int nwavTotal = nwavDocumento.getNwavNumeroPaginas();
        return nwavRangos == null || nwavRangos.isBlank()
                ? IntStream.range(0, nwavTotal).boxed().toList()
                : NwavRangosPaginas.nwavIndices(nwavRangos, nwavTotal);
    }

    private static String nwavLimpiarLinea(String nwavTexto) {
        return nwavTexto == null ? "" : nwavTexto.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').strip();
    }

    private static void nwavValidar(com.nwav.editorpdf.model.NwavFuente nwavFuente, String nwavTexto) {
        PDType1Font nwavLetra = NwavContenidoService.nwavFuente(new NwavEstiloTexto(nwavFuente, 12, false, false, null));
        nwavTexto.codePoints().distinct().forEach(nwavCodigo -> {
            String nwavCaracter = new String(Character.toChars(nwavCodigo));
            try {
                nwavLetra.encode(nwavCaracter);
            } catch (IOException | IllegalArgumentException nwavError) {
                throw new NwavEditorException("La fuente no admite el carácter «" + nwavCaracter + "».");
            }
        });
    }

    private static float nwavAncho(PDType1Font nwavFuente, float nwavTamano, String nwavTexto) {
        try {
            return nwavFuente.getStringWidth(nwavTexto) / 1000f * nwavTamano;
        } catch (IOException nwavError) {
            return nwavTexto.length() * nwavTamano * 0.6f;
        }
    }
}
