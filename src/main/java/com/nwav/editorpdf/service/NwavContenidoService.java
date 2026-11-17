package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.model.NwavColorRgb;
import com.nwav.editorpdf.model.NwavEstiloTexto;
import com.nwav.editorpdf.model.NwavPunto;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.model.NwavTipoForma;
import com.nwav.editorpdf.model.NwavTipoMarca;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.comando.NwavComando;
import com.nwav.editorpdf.service.comando.NwavComandoContenido;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationCircle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationHighlight;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationInk;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLine;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationSquare;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationSquareCircle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationStrikeout;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationText;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationUnderline;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

/**
 * Edicion del contenido de una pagina. Cada metodo {@code nwavComando...} devuelve un
 * comando reversible; el dibujo real ocurre al ejecutarlo.
 * <ul>
 *   <li>Texto, imagen y cobertura se <b>fijan</b> en el contenido de la pagina: se ven igual
 *       en cualquier lector.</li>
 *   <li>Resaltado, subrayado, tachado, notas, dibujo a mano y formas son <b>anotaciones</b>
 *       PDF estandar (con su apariencia generada), que otros lectores pueden mostrar u ocultar.</li>
 * </ul>
 * Todas las coordenadas son visuales (ver {@link NwavPunto}).
 */
@Service
@Slf4j
public class NwavContenidoService {

    public static final String NWAV_AUTOR_ANOTACIONES = "NWAV Editor PDF";
    public static final float NWAV_INTERLINEADO = 1.2f;
    public static final float NWAV_TAMANO_ICONO_NOTA = 20f;
    private static final double NWAV_ZONA_MINIMA = 1.0;
    private static final int NWAV_MAXIMO_PUNTOS_TRAZO = 1500;

    // ================================================================ comandos

    public NwavComando nwavComandoTexto(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavRectangulo nwavZona,
                                        String nwavTexto, NwavEstiloTexto nwavEstilo) {
        String nwavLimpio = nwavTexto == null ? "" : nwavTexto.replace("\r", "").replace("\t", "    ");
        if (nwavLimpio.isBlank()) {
            throw new NwavEditorException("Escribe algún texto antes de fijarlo.");
        }
        nwavValidarCaracteres(nwavFuente(nwavEstilo), nwavEstilo, nwavLimpio);
        return new NwavComandoContenido(this, nwavDocumento, nwavPagina, NwavTipoOperacion.NWAV_TEXTO,
                "Agregar texto", () -> nwavDibujarTexto(nwavDocumento, nwavPagina, nwavZona, nwavLimpio, nwavEstilo));
    }

    public NwavComando nwavComandoImagen(NwavDocumentoPdf nwavDocumento, int nwavPagina, Path nwavImagen,
                                         NwavRectangulo nwavZona) {
        nwavValidarZona(nwavZona);
        return new NwavComandoContenido(this, nwavDocumento, nwavPagina, NwavTipoOperacion.NWAV_IMAGEN,
                "Insertar imagen «" + nwavImagen.getFileName() + "»",
                () -> nwavDibujarImagen(nwavDocumento, nwavPagina, nwavImagen, nwavZona));
    }

    public NwavComando nwavComandoCubrir(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavRectangulo nwavZona,
                                         NwavColorRgb nwavColor) {
        nwavValidarZona(nwavZona);
        return new NwavComandoContenido(this, nwavDocumento, nwavPagina, NwavTipoOperacion.NWAV_CUBRIR,
                "Cubrir zona (solo visual)", () -> nwavDibujarCobertura(nwavDocumento, nwavPagina, nwavZona, nwavColor));
    }

    public NwavComando nwavComandoMarca(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavTipoMarca nwavTipo,
                                        NwavRectangulo nwavZona, NwavColorRgb nwavColor) {
        nwavValidarZona(nwavZona);
        NwavTipoOperacion nwavOperacion = switch (nwavTipo) {
            case NWAV_RESALTADO -> NwavTipoOperacion.NWAV_RESALTAR;
            case NWAV_SUBRAYADO -> NwavTipoOperacion.NWAV_SUBRAYAR;
            case NWAV_TACHADO -> NwavTipoOperacion.NWAV_TACHAR;
        };
        return new NwavComandoContenido(this, nwavDocumento, nwavPagina, nwavOperacion, nwavOperacion.getNwavDescripcion(),
                () -> nwavCrearMarca(nwavDocumento, nwavPagina, nwavTipo, nwavZona, nwavColor));
    }

    public NwavComando nwavComandoNota(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavPunto nwavPunto,
                                       String nwavTexto, NwavColorRgb nwavColor) {
        if (nwavTexto == null || nwavTexto.isBlank()) {
            throw new NwavEditorException("La nota está vacía.");
        }
        return new NwavComandoContenido(this, nwavDocumento, nwavPagina, NwavTipoOperacion.NWAV_NOTA, "Agregar nota",
                () -> nwavCrearNota(nwavDocumento, nwavPagina, nwavPunto, nwavTexto.strip(), nwavColor));
    }

    public NwavComando nwavComandoDibujo(NwavDocumentoPdf nwavDocumento, int nwavPagina, List<NwavPunto> nwavTrazo,
                                         NwavColorRgb nwavColor, double nwavGrosor) {
        if (nwavTrazo == null || nwavTrazo.size() < 2) {
            throw new NwavEditorException("El trazo es demasiado corto.");
        }
        List<NwavPunto> nwavCopia = List.copyOf(nwavTrazo);
        return new NwavComandoContenido(this, nwavDocumento, nwavPagina, NwavTipoOperacion.NWAV_DIBUJO,
                "Dibujo a mano alzada",
                () -> nwavCrearDibujo(nwavDocumento, nwavPagina, nwavCopia, nwavColor, nwavGrosor));
    }

    public NwavComando nwavComandoForma(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavTipoForma nwavTipo,
                                        NwavPunto nwavDesde, NwavPunto nwavHasta, NwavColorRgb nwavColor,
                                        double nwavGrosor) {
        if (Math.hypot(nwavHasta.nwavX() - nwavDesde.nwavX(), nwavHasta.nwavY() - nwavDesde.nwavY()) < 2) {
            throw new NwavEditorException("La forma es demasiado pequeña.");
        }
        return new NwavComandoContenido(this, nwavDocumento, nwavPagina, NwavTipoOperacion.NWAV_FORMA,
                "Agregar " + nwavTipo.getNwavDescripcion().toLowerCase(java.util.Locale.ROOT),
                () -> nwavCrearForma(nwavDocumento, nwavPagina, nwavTipo, nwavDesde, nwavHasta, nwavColor, nwavGrosor));
    }

    // ============================================================ instantaneas

    public NwavInstantaneaPagina nwavCapturar(NwavDocumentoPdf nwavDocumento, int nwavPagina) {
        synchronized (nwavDocumento) {
            nwavDocumento.nwavVerificarAbierto();
            return new NwavInstantaneaPagina(nwavDocumento.nwavPdDocument().getPage(nwavPagina));
        }
    }

    public void nwavRestaurar(NwavDocumentoPdf nwavDocumento, NwavInstantaneaPagina nwavInstantanea) {
        synchronized (nwavDocumento) {
            nwavDocumento.nwavVerificarAbierto();
            nwavInstantanea.nwavRestaurar();
            nwavDocumento.nwavMarcarModificada(nwavInstantanea.nwavPagina().getCOSObject());
        }
    }

    // ============================================================ texto en pagina

    /**
     * Rectangulos (visuales) de las lineas de texto que quedan dentro de la zona; vacio si
     * no hay texto (p. ej. un PDF escaneado) o no se puede extraer.
     */
    public List<NwavRectangulo> nwavLineasDeTexto(NwavDocumentoPdf nwavDocumento, int nwavPagina,
                                                  NwavRectangulo nwavZona) {
        List<TextPosition> nwavPosiciones;
        synchronized (nwavDocumento) {
            nwavDocumento.nwavVerificarAbierto();
            nwavPosiciones = nwavPosicionesDeTexto(nwavDocumento.nwavPdDocument(), nwavPagina);
        }
        record NwavGlifo(double nwavX, double nwavArriba, double nwavDerecha, double nwavAbajo, double nwavBase,
                         double nwavTamano) {
        }
        List<NwavGlifo> nwavDentro = new ArrayList<>();
        for (TextPosition nwavPosicion : nwavPosiciones) {
            if (nwavPosicion.getUnicode() == null || nwavPosicion.getUnicode().isBlank()) {
                continue;
            }
            double nwavTamano = Math.max(nwavPosicion.getFontSizeInPt(), nwavPosicion.getHeight());
            double nwavBase = nwavPosicion.getY();
            NwavGlifo nwavGlifo = new NwavGlifo(nwavPosicion.getX(), nwavBase - nwavTamano * 0.8,
                    nwavPosicion.getX() + nwavPosicion.getWidth(), nwavBase + nwavTamano * 0.22, nwavBase, nwavTamano);
            double nwavCentroX = (nwavGlifo.nwavX() + nwavGlifo.nwavDerecha()) / 2;
            boolean nwavCruza = nwavCentroX >= nwavZona.nwavX() && nwavCentroX <= nwavZona.nwavDerecha()
                    && nwavGlifo.nwavArriba() < nwavZona.nwavAbajo() && nwavGlifo.nwavAbajo() > nwavZona.nwavY();
            if (nwavCruza) {
                nwavDentro.add(nwavGlifo);
            }
        }
        nwavDentro.sort(Comparator.comparingDouble(NwavGlifo::nwavBase).thenComparingDouble(NwavGlifo::nwavX));
        List<NwavRectangulo> nwavLineas = new ArrayList<>();
        List<NwavGlifo> nwavLinea = new ArrayList<>();
        for (NwavGlifo nwavGlifo : nwavDentro) {
            if (!nwavLinea.isEmpty()
                    && Math.abs(nwavGlifo.nwavBase() - nwavLinea.get(0).nwavBase()) > nwavLinea.get(0).nwavTamano() * 0.5) {
                nwavLineas.add(nwavCaja(nwavLinea.stream().mapToDouble(NwavGlifo::nwavX).min().orElse(0),
                        nwavLinea.stream().mapToDouble(NwavGlifo::nwavArriba).min().orElse(0),
                        nwavLinea.stream().mapToDouble(NwavGlifo::nwavDerecha).max().orElse(0),
                        nwavLinea.stream().mapToDouble(NwavGlifo::nwavAbajo).max().orElse(0)));
                nwavLinea.clear();
            }
            nwavLinea.add(nwavGlifo);
        }
        if (!nwavLinea.isEmpty()) {
            nwavLineas.add(nwavCaja(nwavLinea.stream().mapToDouble(NwavGlifo::nwavX).min().orElse(0),
                    nwavLinea.stream().mapToDouble(NwavGlifo::nwavArriba).min().orElse(0),
                    nwavLinea.stream().mapToDouble(NwavGlifo::nwavDerecha).max().orElse(0),
                    nwavLinea.stream().mapToDouble(NwavGlifo::nwavAbajo).max().orElse(0)));
        }
        return nwavLineas;
    }

    private static NwavRectangulo nwavCaja(double nwavX1, double nwavY1, double nwavX2, double nwavY2) {
        return new NwavRectangulo(nwavX1, nwavY1, nwavX2 - nwavX1, nwavY2 - nwavY1);
    }

    private static List<TextPosition> nwavPosicionesDeTexto(PDDocument nwavPd, int nwavPagina) {
        List<TextPosition> nwavPosiciones = new ArrayList<>();
        try {
            PDFTextStripper nwavExtractor = new PDFTextStripper() {
                @Override
                protected void writeString(String nwavTexto, List<TextPosition> nwavLetras) {
                    nwavPosiciones.addAll(nwavLetras);
                }
            };
            nwavExtractor.setSortByPosition(true);
            nwavExtractor.setStartPage(nwavPagina + 1);
            nwavExtractor.setEndPage(nwavPagina + 1);
            nwavExtractor.getText(nwavPd);
        } catch (IOException | RuntimeException nwavError) {
            // Sin permiso de extraccion o texto ilegible: se usara la zona dibujada.
            nwavLog.debug("No se pudo leer el texto de la pagina {}", nwavPagina + 1, nwavError);
        }
        return nwavPosiciones;
    }

    // ============================================================ contenido fijo

    private void nwavDibujarTexto(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavRectangulo nwavZona,
                                  String nwavTexto, NwavEstiloTexto nwavEstilo) {
        nwavDibujarEnPagina(nwavDocumento, nwavPagina, (nwavFlujo, nwavGeometria) -> {
            PDType1Font nwavFuente = nwavFuente(nwavEstilo);
            float nwavTamano = (float) nwavEstilo.nwavTamano();
            List<String> nwavLineas = nwavPartirLineas(nwavTexto, nwavFuente, nwavTamano, nwavZona.nwavAncho());
            float nwavAscenso = nwavFuente.getFontDescriptor() != null && nwavFuente.getFontDescriptor().getAscent() > 0
                    ? nwavFuente.getFontDescriptor().getAscent() / 1000f * nwavTamano : nwavTamano * 0.8f;
            float[] nwavColor = nwavEstilo.nwavColor().nwavComponentes();
            nwavFlujo.setNonStrokingColor(nwavColor[0], nwavColor[1], nwavColor[2]);
            nwavFlujo.beginText();
            nwavFlujo.setFont(nwavFuente, nwavTamano);
            nwavFlujo.setLeading(nwavTamano * NWAV_INTERLINEADO);
            nwavFlujo.newLineAtOffset((float) nwavZona.nwavX(),
                    (float) (nwavGeometria.nwavAltoVisual() - nwavZona.nwavY()) - nwavAscenso);
            for (String nwavLinea : nwavLineas) {
                nwavFlujo.showText(nwavLinea);
                nwavFlujo.newLine();
            }
            nwavFlujo.endText();
        });
    }

    private void nwavDibujarImagen(NwavDocumentoPdf nwavDocumento, int nwavPagina, Path nwavRuta,
                                   NwavRectangulo nwavZona) {
        PDImageXObject nwavImagen;
        synchronized (nwavDocumento) {
            nwavImagen = NwavPaginaService.nwavCargarImagen(nwavDocumento.nwavPdDocument(), nwavRuta);
        }
        nwavDibujarEnPagina(nwavDocumento, nwavPagina, (nwavFlujo, nwavGeometria) ->
                nwavFlujo.drawImage(nwavImagen, (float) nwavZona.nwavX(),
                        (float) (nwavGeometria.nwavAltoVisual() - nwavZona.nwavAbajo()),
                        (float) nwavZona.nwavAncho(), (float) nwavZona.nwavAlto()));
    }

    private void nwavDibujarCobertura(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavRectangulo nwavZona,
                                      NwavColorRgb nwavColor) {
        nwavDibujarEnPagina(nwavDocumento, nwavPagina, (nwavFlujo, nwavGeometria) -> {
            float[] nwavComponentes = nwavColor.nwavComponentes();
            nwavFlujo.setNonStrokingColor(nwavComponentes[0], nwavComponentes[1], nwavComponentes[2]);
            nwavFlujo.addRect((float) nwavZona.nwavX(), (float) (nwavGeometria.nwavAltoVisual() - nwavZona.nwavAbajo()),
                    (float) nwavZona.nwavAncho(), (float) nwavZona.nwavAlto());
            nwavFlujo.fill();
        });
    }

    @FunctionalInterface
    private interface NwavDibujo {
        void nwavDibujar(PDPageContentStream nwavFlujo, NwavGeometriaPagina nwavGeometria) throws IOException;
    }

    /**
     * Agrega un flujo de contenido al final de la pagina, aislado del contenido original
     * (q/Q) y con las coordenadas visuales ya transformadas a las de la pagina.
     */
    private static void nwavDibujarEnPagina(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavDibujo nwavDibujo) {
        synchronized (nwavDocumento) {
            nwavDocumento.nwavVerificarAbierto();
            PDDocument nwavPd = nwavDocumento.nwavPdDocument();
            PDPage nwavHoja = nwavPd.getPage(nwavPagina);
            NwavPaginaService.nwavMaterializar(nwavHoja);
            NwavGeometriaPagina nwavGeometria = NwavGeometriaPagina.nwavDe(nwavHoja);
            try (PDPageContentStream nwavFlujo = new PDPageContentStream(nwavPd, nwavHoja, AppendMode.APPEND, true, true)) {
                nwavFlujo.saveGraphicsState();
                nwavFlujo.transform(nwavGeometria.nwavMatrizVisual());
                nwavDibujo.nwavDibujar(nwavFlujo, nwavGeometria);
                nwavFlujo.restoreGraphicsState();
            } catch (IOException nwavError) {
                throw new NwavEditorException("No se pudo modificar la página " + (nwavPagina + 1) + ".", nwavError);
            }
            nwavDocumento.nwavMarcarModificada(nwavHoja.getCOSObject());
        }
    }

    // ================================================================= anotaciones

    private void nwavCrearMarca(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavTipoMarca nwavTipo,
                                NwavRectangulo nwavZona, NwavColorRgb nwavColor) {
        List<NwavRectangulo> nwavLineas = nwavLineasDeTexto(nwavDocumento, nwavPagina, nwavZona);
        List<NwavRectangulo> nwavZonas = nwavLineas.isEmpty() ? List.of(nwavZona) : nwavLineas;
        nwavAgregarAnotacion(nwavDocumento, nwavPagina, nwavGeometria -> {
            PDAnnotationTextMarkup nwavMarca = switch (nwavTipo) {
                case NWAV_RESALTADO -> new PDAnnotationHighlight();
                case NWAV_SUBRAYADO -> new PDAnnotationUnderline();
                case NWAV_TACHADO -> new PDAnnotationStrikeout();
            };
            float[] nwavCuadros = new float[nwavZonas.size() * 8];
            for (int nwavIndice = 0; nwavIndice < nwavZonas.size(); nwavIndice++) {
                System.arraycopy(nwavGeometria.nwavQuadPoints(nwavZonas.get(nwavIndice)), 0, nwavCuadros,
                        nwavIndice * 8, 8);
            }
            nwavMarca.setQuadPoints(nwavCuadros);
            nwavMarca.setRectangle(nwavEnvolvente(nwavCuadros, 1f));
            nwavMarca.setSubject(nwavTipo.getNwavDescripcion());
            return nwavMarca;
        }, nwavColor);
    }

    private void nwavCrearNota(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavPunto nwavPunto, String nwavTexto,
                               NwavColorRgb nwavColor) {
        nwavAgregarAnotacion(nwavDocumento, nwavPagina, nwavGeometria -> {
            PDAnnotationText nwavNota = new PDAnnotationText();
            nwavNota.setName(PDAnnotationText.NAME_COMMENT);
            nwavNota.setContents(nwavTexto);
            nwavNota.setNoZoom(true);
            nwavNota.setNoRotate(true);
            float[] nwavEsquina = nwavGeometria.nwavAPdf(nwavPunto);
            nwavNota.setRectangle(new PDRectangle(nwavEsquina[0], nwavEsquina[1] - NWAV_TAMANO_ICONO_NOTA,
                    NWAV_TAMANO_ICONO_NOTA, NWAV_TAMANO_ICONO_NOTA));
            nwavNota.setSubject("Nota");
            return nwavNota;
        }, nwavColor);
    }

    private void nwavCrearDibujo(NwavDocumentoPdf nwavDocumento, int nwavPagina, List<NwavPunto> nwavTrazo,
                                 NwavColorRgb nwavColor, double nwavGrosor) {
        nwavAgregarAnotacion(nwavDocumento, nwavPagina, nwavGeometria -> {
            List<NwavPunto> nwavPuntos = nwavSimplificar(nwavTrazo);
            float[] nwavCamino = new float[nwavPuntos.size() * 2];
            for (int nwavIndice = 0; nwavIndice < nwavPuntos.size(); nwavIndice++) {
                float[] nwavPdf = nwavGeometria.nwavAPdf(nwavPuntos.get(nwavIndice));
                nwavCamino[nwavIndice * 2] = nwavPdf[0];
                nwavCamino[nwavIndice * 2 + 1] = nwavPdf[1];
            }
            PDAnnotationInk nwavTinta = new PDAnnotationInk();
            nwavTinta.setInkList(new float[][]{nwavCamino});
            nwavTinta.setBorderStyle(nwavBorde(nwavGrosor));
            nwavTinta.setRectangle(nwavEnvolvente(nwavCamino, (float) nwavGrosor + 2));
            nwavTinta.setSubject("Dibujo");
            return nwavTinta;
        }, nwavColor);
    }

    private void nwavCrearForma(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavTipoForma nwavTipo,
                                NwavPunto nwavDesde, NwavPunto nwavHasta, NwavColorRgb nwavColor, double nwavGrosor) {
        nwavAgregarAnotacion(nwavDocumento, nwavPagina, nwavGeometria -> {
            if (nwavTipo == NwavTipoForma.NWAV_LINEA || nwavTipo == NwavTipoForma.NWAV_FLECHA) {
                float[] nwavA = nwavGeometria.nwavAPdf(nwavDesde);
                float[] nwavB = nwavGeometria.nwavAPdf(nwavHasta);
                float[] nwavLinea = {nwavA[0], nwavA[1], nwavB[0], nwavB[1]};
                PDAnnotationLine nwavAnotacion = new PDAnnotationLine();
                nwavAnotacion.setLine(nwavLinea);
                if (nwavTipo == NwavTipoForma.NWAV_FLECHA) {
                    nwavAnotacion.setEndPointEndingStyle(PDAnnotationLine.LE_OPEN_ARROW);
                }
                nwavAnotacion.setBorderStyle(nwavBorde(nwavGrosor));
                nwavAnotacion.setRectangle(nwavEnvolvente(nwavLinea, (float) (nwavGrosor * 4 + 8)));
                nwavAnotacion.setSubject(nwavTipo.getNwavDescripcion());
                return nwavAnotacion;
            }
            PDAnnotationSquareCircle nwavAnotacion = nwavTipo == NwavTipoForma.NWAV_ELIPSE
                    ? new PDAnnotationCircle() : new PDAnnotationSquare();
            PDRectangle nwavCaja = nwavGeometria.nwavRectanguloPdf(NwavRectangulo.nwavEntre(nwavDesde, nwavHasta));
            float nwavMitad = (float) nwavGrosor / 2;
            // El borde se dibuja dentro del rectangulo de la anotacion: se agranda medio grosor.
            nwavAnotacion.setRectangle(new PDRectangle(nwavCaja.getLowerLeftX() - nwavMitad,
                    nwavCaja.getLowerLeftY() - nwavMitad, nwavCaja.getWidth() + 2 * nwavMitad,
                    nwavCaja.getHeight() + 2 * nwavMitad));
            nwavAnotacion.setBorderStyle(nwavBorde(nwavGrosor));
            nwavAnotacion.setSubject(nwavTipo.getNwavDescripcion());
            return nwavAnotacion;
        }, nwavColor);
    }

    @FunctionalInterface
    private interface NwavFabricaAnotacion {
        PDAnnotationMarkup nwavCrear(NwavGeometriaPagina nwavGeometria);
    }

    /** Crea la anotacion, le pone autor, fecha y color, genera su apariencia y la agrega. */
    private static void nwavAgregarAnotacion(NwavDocumentoPdf nwavDocumento, int nwavPagina,
                                             NwavFabricaAnotacion nwavFabrica, NwavColorRgb nwavColor) {
        synchronized (nwavDocumento) {
            nwavDocumento.nwavVerificarAbierto();
            PDDocument nwavPd = nwavDocumento.nwavPdDocument();
            PDPage nwavHoja = nwavPd.getPage(nwavPagina);
            NwavPaginaService.nwavMaterializar(nwavHoja);
            PDAnnotationMarkup nwavAnotacion = nwavFabrica.nwavCrear(NwavGeometriaPagina.nwavDe(nwavHoja));
            Calendar nwavAhora = Calendar.getInstance();
            nwavAnotacion.setColor(new PDColor(nwavColor.nwavComponentes(), PDDeviceRGB.INSTANCE));
            nwavAnotacion.setTitlePopup(NWAV_AUTOR_ANOTACIONES);
            nwavAnotacion.setCreationDate(nwavAhora);
            nwavAnotacion.setModifiedDate(nwavAhora);
            nwavAnotacion.setPrinted(true);
            nwavAnotacion.setPage(nwavHoja);
            try {
                List<PDAnnotation> nwavAnotaciones = new ArrayList<>(nwavHoja.getAnnotations());
                nwavAnotaciones.add(nwavAnotacion);
                nwavHoja.setAnnotations(nwavAnotaciones);
            } catch (IOException nwavError) {
                throw new NwavEditorException("No se pudieron leer las anotaciones de la página " + (nwavPagina + 1) + ".",
                        nwavError);
            }
            nwavAnotacion.constructAppearances(nwavPd);
            nwavDocumento.nwavMarcarModificada(nwavHoja.getCOSObject());
        }
    }

    // ================================================================= utilidades

    static PDType1Font nwavFuente(NwavEstiloTexto nwavEstilo) {
        boolean nwavNegrita = nwavEstilo.nwavNegrita();
        boolean nwavCursiva = nwavEstilo.nwavCursiva();
        Standard14Fonts.FontName nwavNombre = switch (nwavEstilo.nwavFuente()) {
            case NWAV_TIMES -> nwavNegrita && nwavCursiva ? Standard14Fonts.FontName.TIMES_BOLD_ITALIC
                    : nwavNegrita ? Standard14Fonts.FontName.TIMES_BOLD
                    : nwavCursiva ? Standard14Fonts.FontName.TIMES_ITALIC : Standard14Fonts.FontName.TIMES_ROMAN;
            case NWAV_COURIER -> nwavNegrita && nwavCursiva ? Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE
                    : nwavNegrita ? Standard14Fonts.FontName.COURIER_BOLD
                    : nwavCursiva ? Standard14Fonts.FontName.COURIER_OBLIQUE : Standard14Fonts.FontName.COURIER;
            case NWAV_HELVETICA -> nwavNegrita && nwavCursiva ? Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE
                    : nwavNegrita ? Standard14Fonts.FontName.HELVETICA_BOLD
                    : nwavCursiva ? Standard14Fonts.FontName.HELVETICA_OBLIQUE : Standard14Fonts.FontName.HELVETICA;
        };
        return new PDType1Font(nwavNombre);
    }

    /** Las fuentes estandar no tienen todos los caracteres: se avisa del primero que falte. */
    private static void nwavValidarCaracteres(PDType1Font nwavFuente, NwavEstiloTexto nwavEstilo, String nwavTexto) {
        nwavTexto.codePoints().filter(nwavCodigo -> nwavCodigo != '\n').distinct().forEach(nwavCodigo -> {
            String nwavCaracter = new String(Character.toChars(nwavCodigo));
            try {
                nwavFuente.encode(nwavCaracter);
            } catch (IOException | IllegalArgumentException nwavError) {
                throw new NwavEditorException("La fuente " + nwavEstilo.nwavFuente().getNwavDescripcion()
                        + " no admite el carácter «" + nwavCaracter + "». Quítalo o usa solo caracteres latinos.");
            }
        });
    }

    /** Respeta los saltos de linea y ajusta al ancho (en puntos; 0 = sin ajuste). */
    static List<String> nwavPartirLineas(String nwavTexto, PDType1Font nwavFuente, float nwavTamano, double nwavAncho) {
        List<String> nwavLineas = new ArrayList<>();
        for (String nwavParrafo : nwavTexto.split("\n", -1)) {
            if (nwavAncho <= 0 || nwavMedir(nwavFuente, nwavTamano, nwavParrafo) <= nwavAncho) {
                nwavLineas.add(nwavParrafo);
                continue;
            }
            StringBuilder nwavActual = new StringBuilder();
            for (String nwavPalabra : nwavParrafo.split(" ")) {
                String nwavPrueba = nwavActual.isEmpty() ? nwavPalabra : nwavActual + " " + nwavPalabra;
                if (nwavMedir(nwavFuente, nwavTamano, nwavPrueba) <= nwavAncho) {
                    nwavActual = new StringBuilder(nwavPrueba);
                    continue;
                }
                if (!nwavActual.isEmpty()) {
                    nwavLineas.add(nwavActual.toString());
                }
                nwavActual = new StringBuilder(nwavPalabra);
                // Palabra mas larga que el ancho: se corta por caracteres.
                while (nwavMedir(nwavFuente, nwavTamano, nwavActual.toString()) > nwavAncho && nwavActual.length() > 1) {
                    int nwavCorte = nwavActual.length() - 1;
                    while (nwavCorte > 1 && nwavMedir(nwavFuente, nwavTamano, nwavActual.substring(0, nwavCorte)) > nwavAncho) {
                        nwavCorte--;
                    }
                    nwavLineas.add(nwavActual.substring(0, nwavCorte));
                    nwavActual = new StringBuilder(nwavActual.substring(nwavCorte));
                }
            }
            nwavLineas.add(nwavActual.toString());
        }
        return nwavLineas;
    }

    private static double nwavMedir(PDType1Font nwavFuente, float nwavTamano, String nwavTexto) {
        try {
            return nwavFuente.getStringWidth(nwavTexto) / 1000.0 * nwavTamano;
        } catch (IOException | IllegalArgumentException nwavError) {
            return nwavTexto.length() * nwavTamano * 0.6;
        }
    }

    private static PDBorderStyleDictionary nwavBorde(double nwavGrosor) {
        PDBorderStyleDictionary nwavBorde = new PDBorderStyleDictionary();
        nwavBorde.setWidth((float) Math.max(0.5, Math.min(30, nwavGrosor)));
        return nwavBorde;
    }

    /** Rectangulo que envuelve una lista de coordenadas x,y, con margen. */
    private static PDRectangle nwavEnvolvente(float[] nwavCoordenadas, float nwavMargen) {
        float nwavMinX = Float.MAX_VALUE;
        float nwavMinY = Float.MAX_VALUE;
        float nwavMaxX = -Float.MAX_VALUE;
        float nwavMaxY = -Float.MAX_VALUE;
        for (int nwavIndice = 0; nwavIndice + 1 < nwavCoordenadas.length; nwavIndice += 2) {
            nwavMinX = Math.min(nwavMinX, nwavCoordenadas[nwavIndice]);
            nwavMaxX = Math.max(nwavMaxX, nwavCoordenadas[nwavIndice]);
            nwavMinY = Math.min(nwavMinY, nwavCoordenadas[nwavIndice + 1]);
            nwavMaxY = Math.max(nwavMaxY, nwavCoordenadas[nwavIndice + 1]);
        }
        return new PDRectangle(nwavMinX - nwavMargen, nwavMinY - nwavMargen,
                nwavMaxX - nwavMinX + 2 * nwavMargen, nwavMaxY - nwavMinY + 2 * nwavMargen);
    }

    /** Reduce trazos muy largos tomando puntos equiespaciados (el dibujo apenas cambia). */
    private static List<NwavPunto> nwavSimplificar(List<NwavPunto> nwavTrazo) {
        if (nwavTrazo.size() <= NWAV_MAXIMO_PUNTOS_TRAZO) {
            return nwavTrazo;
        }
        List<NwavPunto> nwavReducido = new ArrayList<>();
        double nwavPaso = (double) nwavTrazo.size() / NWAV_MAXIMO_PUNTOS_TRAZO;
        for (double nwavPosicion = 0; nwavPosicion < nwavTrazo.size(); nwavPosicion += nwavPaso) {
            nwavReducido.add(nwavTrazo.get((int) nwavPosicion));
        }
        nwavReducido.add(nwavTrazo.get(nwavTrazo.size() - 1));
        return nwavReducido;
    }

    private static void nwavValidarZona(NwavRectangulo nwavZona) {
        if (nwavZona == null || nwavZona.nwavAncho() < NWAV_ZONA_MINIMA || nwavZona.nwavAlto() < NWAV_ZONA_MINIMA) {
            throw new NwavEditorException("La zona elegida es demasiado pequeña.");
        }
    }
}
