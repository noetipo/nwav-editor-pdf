package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavRectangulo;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reconstruye la estructura de las paginas de un PDF (que solo guarda glifos posicionados):
 * lineas con su tamano y estilo, titulos, parrafos, listas, tablas, enlaces e imagenes con
 * su posicion. Es heuristico: sirve para Markdown, Word, Excel y PowerPoint, y el resultado
 * puede necesitar ajustes. Coordenadas "visuales" (origen arriba a la izquierda).
 */
public final class NwavDisposicionPdf {

    private static final Pattern NWAV_VINETA = Pattern.compile("^([•●▪◦■□➢►‣∙·\\-–—*])\\s+(.*)$");
    private static final Pattern NWAV_NUMERADA = Pattern.compile("^((\\d{1,3}|[a-zA-Z])[.)])\\s+(.*)$");

    private NwavDisposicionPdf() {
    }

    // ================================================================= modelo

    public record NwavPalabra(String nwavTexto, float nwavX, float nwavXFin, float nwavY, float nwavAlto, float nwavTamano,
                              boolean nwavNegrita, boolean nwavCursiva, int nwavColor) {
    }

    public record NwavCelda(String nwavTexto, float nwavX, float nwavXFin) {
    }

    public record NwavLinea(List<NwavPalabra> nwavPalabras) {
        public String nwavTexto() {
            StringBuilder nwavTexto = new StringBuilder();
            for (NwavPalabra nwavPalabra : nwavPalabras) {
                if (!nwavTexto.isEmpty()) {
                    nwavTexto.append(' ');
                }
                nwavTexto.append(nwavPalabra.nwavTexto());
            }
            return nwavTexto.toString();
        }

        public float nwavX() {
            return nwavPalabras.get(0).nwavX();
        }

        public float nwavXFin() {
            return nwavPalabras.get(nwavPalabras.size() - 1).nwavXFin();
        }

        /** Linea base (Y visual hacia abajo). */
        public float nwavY() {
            return nwavPalabras.get(0).nwavY();
        }

        public float nwavAlto() {
            return (float) nwavPalabras.stream().mapToDouble(NwavPalabra::nwavAlto).max().orElse(10);
        }

        /** Tamano de letra dominante (el de mas caracteres). */
        public float nwavTamano() {
            Map<Float, Integer> nwavCuenta = new HashMap<>();
            nwavPalabras.forEach(nwavP -> nwavCuenta.merge(Math.round(nwavP.nwavTamano() * 2) / 2f,
                    nwavP.nwavTexto().length(), Integer::sum));
            return nwavCuenta.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(10f);
        }

        public boolean nwavNegrita() {
            return nwavPalabras.stream().allMatch(NwavPalabra::nwavNegrita);
        }

        public boolean nwavCursiva() {
            return nwavPalabras.stream().allMatch(NwavPalabra::nwavCursiva);
        }

        public int nwavColor() {
            return nwavPalabras.get(0).nwavColor();
        }

        /** Divide la linea en celdas donde hay un hueco grande entre palabras. */
        public List<NwavCelda> nwavCeldas() {
            List<NwavCelda> nwavCeldas = new ArrayList<>();
            StringBuilder nwavTexto = new StringBuilder();
            float nwavInicio = nwavPalabras.get(0).nwavX();
            float nwavFin = nwavInicio;
            for (int nwavIndice = 0; nwavIndice < nwavPalabras.size(); nwavIndice++) {
                NwavPalabra nwavPalabra = nwavPalabras.get(nwavIndice);
                if (nwavIndice > 0 && nwavPalabra.nwavX() - nwavFin > Math.max(8, nwavPalabra.nwavTamano() * 1.2f)) {
                    nwavCeldas.add(new NwavCelda(nwavTexto.toString(), nwavInicio, nwavFin));
                    nwavTexto.setLength(0);
                    nwavInicio = nwavPalabra.nwavX();
                }
                if (!nwavTexto.isEmpty()) {
                    nwavTexto.append(' ');
                }
                nwavTexto.append(nwavPalabra.nwavTexto());
                nwavFin = nwavPalabra.nwavXFin();
            }
            nwavCeldas.add(new NwavCelda(nwavTexto.toString(), nwavInicio, nwavFin));
            return nwavCeldas;
        }
    }

    public record NwavEnlace(NwavRectangulo nwavZona, String nwavUri) {
    }

    public record NwavImagenUbicada(PDImageXObject nwavImagen, NwavRectangulo nwavZona) {
    }

    public record NwavPagina(int nwavIndice, float nwavAncho, float nwavAlto, List<NwavLinea> nwavLineas,
                             List<NwavEnlace> nwavEnlaces, List<NwavImagenUbicada> nwavImagenes) {
    }

    // ================================================================= bloques

    public enum NwavTipoBloque { NWAV_TITULO, NWAV_PARRAFO, NWAV_LISTA, NWAV_TABLA, NWAV_IMAGEN }

    /**
     * Bloque de contenido en orden de lectura. Segun el tipo: {@code nwavNivel} (titulo 1-3),
     * {@code nwavOrdenada}/{@code nwavMarca} (lista), {@code nwavFilas} (tabla) o
     * {@code nwavImagen}. {@code nwavLineas} son las lineas originales (para estilo y enlaces).
     */
    public record NwavBloque(NwavTipoBloque nwavTipo, String nwavTexto, int nwavNivel, boolean nwavOrdenada, String nwavMarca,
                             List<List<String>> nwavFilas, NwavImagenUbicada nwavImagen, List<NwavLinea> nwavLineas,
                             float nwavY) {
    }

    /** Estadisticas del documento para decidir que es titulo. */
    public record NwavEstilos(float nwavCuerpo, List<Float> nwavTitulos) {
        public int nwavNivel(NwavLinea nwavLinea) {
            float nwavTamano = nwavLinea.nwavTamano();
            for (int nwavIndice = 0; nwavIndice < nwavTitulos.size(); nwavIndice++) {
                if (Math.abs(nwavTitulos.get(nwavIndice) - nwavTamano) < 0.6f) {
                    return Math.min(3, nwavIndice + 1);
                }
            }
            return 0;
        }
    }

    // ================================================================= analisis

    public static List<NwavPagina> nwavAnalizar(PDDocument nwavPd, List<Integer> nwavIndices, NwavCancelacion nwavCancelacion)
            throws IOException {
        NwavExtractor nwavExtractor = new NwavExtractor();
        List<NwavPagina> nwavPaginas = new ArrayList<>();
        for (int nwavIndice : nwavIndices) {
            nwavCancelacion.nwavVerificar();
            PDPage nwavPagina = nwavPd.getPage(nwavIndice);
            NwavGeometriaPagina nwavGeometria = NwavGeometriaPagina.nwavDe(nwavPagina);
            List<NwavLinea> nwavLineas = nwavExtractor.nwavLineas(nwavPd, nwavIndice);
            nwavPaginas.add(new NwavPagina(nwavIndice, nwavGeometria.nwavAnchoVisual(), nwavGeometria.nwavAltoVisual(),
                    nwavLineas, nwavEnlaces(nwavPagina, nwavGeometria), nwavImagenes(nwavPagina, nwavGeometria)));
        }
        return nwavPaginas;
    }

    public static NwavEstilos nwavEstilos(List<NwavPagina> nwavPaginas) {
        Map<Float, Integer> nwavPorTamano = new TreeMap<>();
        for (NwavPagina nwavPagina : nwavPaginas) {
            for (NwavLinea nwavLinea : nwavPagina.nwavLineas()) {
                nwavPorTamano.merge(nwavLinea.nwavTamano(), nwavLinea.nwavTexto().length(), Integer::sum);
            }
        }
        float nwavCuerpo = nwavPorTamano.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
                .orElse(11f);
        List<Float> nwavTitulos = nwavPorTamano.keySet().stream().filter(nwavTamano -> nwavTamano >= nwavCuerpo * 1.15f)
                .sorted(Comparator.reverseOrder()).limit(3).toList();
        return new NwavEstilos(nwavCuerpo, nwavTitulos);
    }

    /** Agrupa las lineas de una pagina en bloques (titulos, parrafos, listas, tablas e imagenes). */
    public static List<NwavBloque> nwavBloques(NwavPagina nwavPagina, NwavEstilos nwavEstilos) {
        List<NwavBloque> nwavBloques = new ArrayList<>();
        List<NwavLinea> nwavLineas = nwavPagina.nwavLineas();
        int nwavIndice = 0;
        while (nwavIndice < nwavLineas.size()) {
            NwavLinea nwavLinea = nwavLineas.get(nwavIndice);
            // Tabla: dos o mas lineas seguidas con las mismas columnas.
            int nwavFinTabla = nwavFinTabla(nwavLineas, nwavIndice);
            if (nwavFinTabla - nwavIndice >= 2) {
                List<List<String>> nwavFilas = new ArrayList<>();
                for (int nwavFila = nwavIndice; nwavFila < nwavFinTabla; nwavFila++) {
                    nwavFilas.add(nwavLineas.get(nwavFila).nwavCeldas().stream().map(NwavCelda::nwavTexto).toList());
                }
                nwavBloques.add(new NwavBloque(NwavTipoBloque.NWAV_TABLA, null, 0, false, null, nwavFilas, null,
                        nwavLineas.subList(nwavIndice, nwavFinTabla), nwavLinea.nwavY()));
                nwavIndice = nwavFinTabla;
                continue;
            }
            String nwavTexto = nwavLinea.nwavTexto().strip();
            int nwavNivel = nwavTexto.length() <= 150 ? nwavEstilos.nwavNivel(nwavLinea) : 0;
            if (nwavNivel == 0 && nwavLinea.nwavNegrita() && nwavTexto.length() <= 80 && !nwavTexto.endsWith(".")
                    && nwavEstilos.nwavTitulos().size() < 3 && nwavAislada(nwavLineas, nwavIndice)) {
                nwavNivel = Math.min(3, nwavEstilos.nwavTitulos().size() + 1);
            }
            if (nwavNivel > 0) {
                // Un titulo puede ocupar dos lineas seguidas del mismo tamano.
                List<NwavLinea> nwavPartes = new ArrayList<>(List.of(nwavLinea));
                while (nwavIndice + 1 < nwavLineas.size() && nwavEstilos.nwavNivel(nwavLineas.get(nwavIndice + 1)) == nwavNivel
                        && nwavSeguida(nwavLinea, nwavLineas.get(nwavIndice + 1))) {
                    nwavPartes.add(nwavLineas.get(++nwavIndice));
                }
                nwavBloques.add(new NwavBloque(NwavTipoBloque.NWAV_TITULO, nwavUnir(nwavPartes), nwavNivel, false, null,
                        null, null, nwavPartes, nwavLinea.nwavY()));
                nwavIndice++;
                continue;
            }
            Matcher nwavVineta = NWAV_VINETA.matcher(nwavTexto);
            Matcher nwavNumero = NWAV_NUMERADA.matcher(nwavTexto);
            if (nwavVineta.matches() || nwavNumero.matches()) {
                boolean nwavOrdenada = !nwavVineta.matches();
                String nwavMarca = nwavOrdenada ? nwavNumero.group(1) : nwavVineta.group(1);
                List<NwavLinea> nwavPartes = new ArrayList<>(List.of(nwavLinea));
                // Lineas siguientes con sangria mayor = continuacion del elemento.
                while (nwavIndice + 1 < nwavLineas.size()) {
                    NwavLinea nwavSiguiente = nwavLineas.get(nwavIndice + 1);
                    String nwavTextoSiguiente = nwavSiguiente.nwavTexto().strip();
                    if (nwavSiguiente.nwavX() > nwavLinea.nwavX() + 4 && nwavSeguida(nwavPartes.get(nwavPartes.size() - 1),
                            nwavSiguiente) && !NWAV_VINETA.matcher(nwavTextoSiguiente).matches()
                            && !NWAV_NUMERADA.matcher(nwavTextoSiguiente).matches()) {
                        nwavPartes.add(nwavSiguiente);
                        nwavIndice++;
                    } else {
                        break;
                    }
                }
                String nwavContenido = nwavUnir(nwavPartes);
                nwavContenido = nwavContenido.substring(Math.min(nwavContenido.length(), nwavMarca.length())).strip();
                nwavBloques.add(new NwavBloque(NwavTipoBloque.NWAV_LISTA, nwavContenido, 0, nwavOrdenada, nwavMarca, null,
                        null, nwavPartes, nwavLinea.nwavY()));
                nwavIndice++;
                continue;
            }
            // Parrafo: lineas seguidas del mismo estilo y con interlineado normal.
            List<NwavLinea> nwavPartes = new ArrayList<>(List.of(nwavLinea));
            while (nwavIndice + 1 < nwavLineas.size()) {
                NwavLinea nwavSiguiente = nwavLineas.get(nwavIndice + 1);
                String nwavTextoSiguiente = nwavSiguiente.nwavTexto().strip();
                if (nwavSeguida(nwavPartes.get(nwavPartes.size() - 1), nwavSiguiente)
                        && Math.abs(nwavSiguiente.nwavTamano() - nwavLinea.nwavTamano()) < 0.6f
                        && nwavEstilos.nwavNivel(nwavSiguiente) == 0
                        && nwavFinTabla(nwavLineas, nwavIndice + 1) - (nwavIndice + 1) < 2
                        && !NWAV_VINETA.matcher(nwavTextoSiguiente).matches()
                        && !NWAV_NUMERADA.matcher(nwavTextoSiguiente).matches()) {
                    nwavPartes.add(nwavSiguiente);
                    nwavIndice++;
                } else {
                    break;
                }
            }
            nwavBloques.add(new NwavBloque(NwavTipoBloque.NWAV_PARRAFO, nwavUnir(nwavPartes), 0, false, null, null, null,
                    nwavPartes, nwavLinea.nwavY()));
            nwavIndice++;
        }
        // Las imagenes se intercalan por su posicion vertical.
        for (NwavImagenUbicada nwavImagen : nwavPagina.nwavImagenes()) {
            nwavBloques.add(new NwavBloque(NwavTipoBloque.NWAV_IMAGEN, null, 0, false, null, null, nwavImagen, List.of(),
                    (float) nwavImagen.nwavZona().nwavAbajo()));
        }
        nwavBloques.sort(Comparator.comparingDouble(NwavBloque::nwavY));
        return nwavBloques;
    }

    /** Une lineas en un texto, juntando las palabras cortadas con guion al final de linea. */
    public static String nwavUnir(List<NwavLinea> nwavLineas) {
        StringBuilder nwavTexto = new StringBuilder();
        for (NwavLinea nwavLinea : nwavLineas) {
            String nwavParte = nwavLinea.nwavTexto().strip();
            if (nwavTexto.length() > 1 && nwavTexto.charAt(nwavTexto.length() - 1) == '-'
                    && Character.isLetter(nwavTexto.charAt(nwavTexto.length() - 2))
                    && !nwavParte.isEmpty() && Character.isLowerCase(nwavParte.charAt(0))) {
                nwavTexto.setLength(nwavTexto.length() - 1);
            } else if (!nwavTexto.isEmpty()) {
                nwavTexto.append(' ');
            }
            nwavTexto.append(nwavParte);
        }
        return nwavTexto.toString();
    }

    private static boolean nwavSeguida(NwavLinea nwavAnterior, NwavLinea nwavSiguiente) {
        float nwavSalto = nwavSiguiente.nwavY() - nwavAnterior.nwavY();
        return nwavSalto > 0 && nwavSalto <= Math.max(nwavAnterior.nwavTamano(), nwavSiguiente.nwavTamano()) * 1.75f;
    }

    private static boolean nwavAislada(List<NwavLinea> nwavLineas, int nwavIndice) {
        return nwavIndice + 1 >= nwavLineas.size() || !nwavLineas.get(nwavIndice + 1).nwavNegrita();
    }

    /** Indice de fin (exclusivo) de una tabla que empieza en {@code nwavInicio}. */
    private static int nwavFinTabla(List<NwavLinea> nwavLineas, int nwavInicio) {
        List<NwavCelda> nwavColumnas = nwavLineas.get(nwavInicio).nwavCeldas();
        if (nwavColumnas.size() < 2) {
            return nwavInicio;
        }
        int nwavFin = nwavInicio + 1;
        while (nwavFin < nwavLineas.size()) {
            NwavLinea nwavLinea = nwavLineas.get(nwavFin);
            List<NwavCelda> nwavCeldas = nwavLinea.nwavCeldas();
            if (nwavCeldas.size() != nwavColumnas.size()
                    || nwavLinea.nwavY() - nwavLineas.get(nwavFin - 1).nwavY() > nwavLinea.nwavTamano() * 3.5f) {
                break;
            }
            boolean nwavAlineadas = true;
            for (int nwavColumna = 0; nwavColumna < nwavCeldas.size() && nwavAlineadas; nwavColumna++) {
                NwavCelda nwavA = nwavColumnas.get(nwavColumna);
                NwavCelda nwavB = nwavCeldas.get(nwavColumna);
                // Se solapan horizontalmente (sirve para columnas alineadas a izquierda o derecha).
                nwavAlineadas = nwavB.nwavX() <= nwavA.nwavXFin() + 4 && nwavA.nwavX() <= nwavB.nwavXFin() + 4;
            }
            if (!nwavAlineadas) {
                break;
            }
            nwavFin++;
        }
        return nwavFin;
    }

    // ============================================================ extraccion

    /** Extractor de texto que conserva posicion, tamano, estilo y color de cada palabra. */
    private static final class NwavExtractor extends PDFTextStripper {
        private final List<NwavLinea> nwavLineas = new ArrayList<>();
        private final List<NwavPalabra> nwavActual = new ArrayList<>();
        private final Map<TextPosition, Integer> nwavColores = new IdentityHashMap<>();

        NwavExtractor() {
            setSortByPosition(true);
            setSuppressDuplicateOverlappingText(true);
        }

        List<NwavLinea> nwavLineas(PDDocument nwavPd, int nwavIndice) throws IOException {
            nwavLineas.clear();
            nwavActual.clear();
            nwavColores.clear();
            setStartPage(nwavIndice + 1);
            setEndPage(nwavIndice + 1);
            writeText(nwavPd, new StringWriter());
            nwavCerrarLinea();
            nwavLineas.sort(Comparator.comparingDouble(NwavLinea::nwavY).thenComparingDouble(NwavLinea::nwavX));
            return List.copyOf(nwavLineas);
        }

        @Override
        protected void processTextPosition(TextPosition nwavTexto) {
            try {
                nwavColores.put(nwavTexto, getGraphicsState().getNonStrokingColor().toRGB());
            } catch (IOException | RuntimeException nwavError) {
                nwavColores.put(nwavTexto, 0);
            }
            super.processTextPosition(nwavTexto);
        }

        @Override
        protected void writeString(String nwavTexto, List<TextPosition> nwavPosiciones) {
            if (nwavPosiciones.isEmpty() || nwavTexto.isBlank()) {
                return;
            }
            TextPosition nwavPrimera = nwavPosiciones.get(0);
            TextPosition nwavUltima = nwavPosiciones.get(nwavPosiciones.size() - 1);
            PDFont nwavFuente = nwavPrimera.getFont();
            String nwavNombre = nwavFuente == null || nwavFuente.getName() == null ? ""
                    : nwavFuente.getName().toLowerCase(Locale.ROOT);
            boolean nwavNegrita = nwavNombre.contains("bold") || nwavNombre.contains("black") || nwavNombre.contains("heavy")
                    || nwavFuente != null && nwavFuente.getFontDescriptor() != null
                    && nwavFuente.getFontDescriptor().getFontWeight() >= 600;
            boolean nwavCursiva = nwavNombre.contains("italic") || nwavNombre.contains("oblique")
                    || nwavFuente != null && nwavFuente.getFontDescriptor() != null && nwavFuente.getFontDescriptor().isItalic();
            float nwavTamano = nwavPrimera.getFontSizeInPt();
            if (nwavTamano <= 0.1f) {
                nwavTamano = nwavPrimera.getHeightDir() * 1.4f;
            }
            // Una "palabra" de PDFBox puede contener espacios: se separa por ellos.
            float nwavInicio = nwavPrimera.getXDirAdj();
            StringBuilder nwavTrozo = new StringBuilder();
            for (TextPosition nwavPos : nwavPosiciones) {
                String nwavCaracter = nwavPos.getUnicode();
                if (nwavCaracter == null) {
                    continue;
                }
                if (nwavCaracter.isBlank()) {
                    if (!nwavTrozo.isEmpty()) {
                        nwavActual.add(new NwavPalabra(nwavTrozo.toString(), nwavInicio, nwavPos.getXDirAdj(),
                                nwavPrimera.getYDirAdj(), nwavPrimera.getHeightDir(), nwavTamano, nwavNegrita, nwavCursiva,
                                nwavColores.getOrDefault(nwavPrimera, 0)));
                        nwavTrozo.setLength(0);
                    }
                    nwavInicio = nwavPos.getXDirAdj() + nwavPos.getWidthDirAdj();
                } else {
                    if (nwavTrozo.isEmpty()) {
                        nwavInicio = nwavPos.getXDirAdj();
                    }
                    nwavTrozo.append(nwavCaracter);
                }
            }
            if (!nwavTrozo.isEmpty()) {
                nwavActual.add(new NwavPalabra(nwavTrozo.toString(), nwavInicio,
                        nwavUltima.getXDirAdj() + nwavUltima.getWidthDirAdj(), nwavPrimera.getYDirAdj(),
                        nwavPrimera.getHeightDir(), nwavTamano, nwavNegrita, nwavCursiva,
                        nwavColores.getOrDefault(nwavPrimera, 0)));
            }
        }

        @Override
        protected void writeLineSeparator() {
            nwavCerrarLinea();
        }

        @Override
        protected void writeParagraphEnd() {
            nwavCerrarLinea();
        }

        @Override
        protected void writePageEnd() {
            nwavCerrarLinea();
        }

        private void nwavCerrarLinea() {
            if (!nwavActual.isEmpty()) {
                List<NwavPalabra> nwavOrdenadas = new ArrayList<>(nwavActual);
                nwavOrdenadas.sort(Comparator.comparingDouble(NwavPalabra::nwavX));
                nwavLineas.add(new NwavLinea(List.copyOf(nwavOrdenadas)));
                nwavActual.clear();
            }
        }
    }

    private static List<NwavEnlace> nwavEnlaces(PDPage nwavPagina, NwavGeometriaPagina nwavGeometria) throws IOException {
        List<NwavEnlace> nwavEnlaces = new ArrayList<>();
        for (PDAnnotation nwavAnotacion : nwavPagina.getAnnotations()) {
            if (nwavAnotacion instanceof PDAnnotationLink nwavLink && nwavLink.getAction() instanceof PDActionURI nwavUri
                    && nwavUri.getURI() != null && nwavLink.getRectangle() != null) {
                NwavRectangulo nwavZona = nwavGeometria.nwavRectanguloVisual(nwavLink.getRectangle());
                if (nwavZona != null) {
                    nwavEnlaces.add(new NwavEnlace(nwavZona, nwavUri.getURI()));
                }
            }
        }
        return nwavEnlaces;
    }

    /** Enlace (si lo hay) bajo una palabra. */
    public static String nwavEnlaceDe(NwavPalabra nwavPalabra, List<NwavEnlace> nwavEnlaces) {
        double nwavCx = (nwavPalabra.nwavX() + nwavPalabra.nwavXFin()) / 2;
        double nwavCy = nwavPalabra.nwavY() - nwavPalabra.nwavAlto() / 2;
        for (NwavEnlace nwavEnlace : nwavEnlaces) {
            NwavRectangulo nwavZona = nwavEnlace.nwavZona();
            if (nwavCx >= nwavZona.nwavX() - 1 && nwavCx <= nwavZona.nwavDerecha() + 1 && nwavCy >= nwavZona.nwavY() - 2
                    && nwavCy <= nwavZona.nwavAbajo() + 2) {
                return nwavEnlace.nwavUri();
            }
        }
        return null;
    }

    private static List<NwavImagenUbicada> nwavImagenes(PDPage nwavPagina, NwavGeometriaPagina nwavGeometria)
            throws IOException {
        NwavLocalizador nwavLocalizador = new NwavLocalizador();
        nwavLocalizador.processPage(nwavPagina);
        List<NwavImagenUbicada> nwavImagenes = new ArrayList<>();
        nwavLocalizador.nwavEncontradas.forEach((nwavImagen, nwavCaja) -> {
            NwavRectangulo nwavZona = nwavGeometria.nwavRectanguloVisual(nwavCaja);
            if (nwavZona != null && nwavZona.nwavAncho() >= 8 && nwavZona.nwavAlto() >= 8) {
                nwavImagenes.add(new NwavImagenUbicada(nwavImagen, nwavZona));
            }
        });
        return nwavImagenes;
    }

    /** Recorre el contenido (y los formularios XObject) anotando donde se dibuja cada imagen. */
    private static final class NwavLocalizador extends PDFStreamEngine {
        private final Map<PDImageXObject, PDRectangle> nwavEncontradas = new LinkedHashMap<>();

        NwavLocalizador() {
            addOperator(new Concatenate(this));
            addOperator(new SetGraphicsStateParameters(this));
            addOperator(new Save(this));
            addOperator(new Restore(this));
            addOperator(new SetMatrix(this));
        }

        @Override
        protected void processOperator(Operator nwavOperador, List<COSBase> nwavOperandos) throws IOException {
            if ("Do".equals(nwavOperador.getName()) && !nwavOperandos.isEmpty()
                    && nwavOperandos.get(0) instanceof COSName nwavNombre && getResources() != null) {
                PDXObject nwavObjeto = getResources().getXObject(nwavNombre);
                if (nwavObjeto instanceof PDImageXObject nwavImagen) {
                    Matrix nwavCtm = getGraphicsState().getCurrentTransformationMatrix();
                    float[][] nwavEsquinas = {{0, 0}, {1, 0}, {0, 1}, {1, 1}};
                    float nwavMinX = Float.MAX_VALUE;
                    float nwavMinY = Float.MAX_VALUE;
                    float nwavMaxX = -Float.MAX_VALUE;
                    float nwavMaxY = -Float.MAX_VALUE;
                    for (float[] nwavEsquina : nwavEsquinas) {
                        java.awt.geom.Point2D.Float nwavPunto = nwavCtm.transformPoint(nwavEsquina[0], nwavEsquina[1]);
                        nwavMinX = Math.min(nwavMinX, nwavPunto.x);
                        nwavMinY = Math.min(nwavMinY, nwavPunto.y);
                        nwavMaxX = Math.max(nwavMaxX, nwavPunto.x);
                        nwavMaxY = Math.max(nwavMaxY, nwavPunto.y);
                    }
                    nwavEncontradas.putIfAbsent(nwavImagen,
                            new PDRectangle(nwavMinX, nwavMinY, nwavMaxX - nwavMinX, nwavMaxY - nwavMinY));
                    return;
                }
                if (nwavObjeto instanceof PDFormXObject nwavFormulario) {
                    showForm(nwavFormulario);
                    return;
                }
            }
            super.processOperator(nwavOperador, nwavOperandos);
        }
    }
}
