package com.nwav.editorpdf.service.ia;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conversor minimo de Markdown (titulos, parrafos, listas, tablas, negrita, cursiva, enlaces) a HTML. */
public final class NwavMarkdownHtml {

    private static final Pattern NWAV_TITULO = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern NWAV_VINETA = Pattern.compile("^\\s*[-*+]\\s+(.*)$");
    private static final Pattern NWAV_NUMERO = Pattern.compile("^\\s*\\d+[.)]\\s+(.*)$");
    private static final Pattern NWAV_SEPARADOR_TABLA = Pattern.compile("^\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)*\\|?$");

    private NwavMarkdownHtml() {
    }

    public static String nwavConvertir(String nwavMarkdown, String nwavTituloDocumento) {
        StringBuilder nwavHtml = new StringBuilder("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>")
                .append(nwavEscapar(nwavTituloDocumento)).append("</title><style>")
                .append("body{font-size:11pt;line-height:1.45;color:#1f2937}h1{font-size:20pt}h2{font-size:15pt}")
                .append("h3{font-size:12.5pt}table{border-collapse:collapse;margin:8px 0}")
                .append("td,th{border:1px solid #cbd5e1;padding:4px 7px}th{background:#f1f5f9}")
                .append(".nwav-aviso-ia{font-size:8.5pt;color:#64748b;border-bottom:1px solid #e2e8f0;padding-bottom:6px}")
                .append("</style></head><body>");
        List<String> nwavLineas = List.of(nwavMarkdown.replace("\r", "").split("\n", -1));
        String nwavLista = null;
        List<String> nwavParrafo = new ArrayList<>();
        int nwavIndice = 0;
        while (nwavIndice < nwavLineas.size()) {
            String nwavLinea = nwavLineas.get(nwavIndice);
            Matcher nwavTitulo = NWAV_TITULO.matcher(nwavLinea);
            Matcher nwavVineta = NWAV_VINETA.matcher(nwavLinea);
            Matcher nwavNumero = NWAV_NUMERO.matcher(nwavLinea);
            boolean nwavTabla = nwavLinea.strip().startsWith("|") && nwavIndice + 1 < nwavLineas.size()
                    && NWAV_SEPARADOR_TABLA.matcher(nwavLineas.get(nwavIndice + 1).strip()).matches();
            String nwavTipoLista = nwavVineta.matches() ? "ul" : nwavNumero.matches() ? "ol" : null;
            if (nwavLinea.isBlank() || nwavTitulo.matches() || nwavTipoLista != null || nwavTabla) {
                nwavCerrarParrafo(nwavHtml, nwavParrafo);
            }
            if (nwavTipoLista == null && nwavLista != null) {
                nwavHtml.append("</").append(nwavLista).append('>');
                nwavLista = null;
            }
            if (nwavLinea.isBlank()) {
                nwavIndice++;
            } else if (nwavTitulo.matches()) {
                int nwavNivel = nwavTitulo.group(1).length();
                nwavHtml.append("<h").append(nwavNivel).append('>').append(nwavEnLinea(nwavTitulo.group(2)))
                        .append("</h").append(nwavNivel).append('>');
                nwavIndice++;
            } else if (nwavTipoLista != null) {
                if (!nwavTipoLista.equals(nwavLista)) {
                    if (nwavLista != null) {
                        nwavHtml.append("</").append(nwavLista).append('>');
                    }
                    nwavHtml.append('<').append(nwavTipoLista).append('>');
                    nwavLista = nwavTipoLista;
                }
                nwavHtml.append("<li>").append(nwavEnLinea((nwavVineta.matches() ? nwavVineta : nwavNumero).group(1)))
                        .append("</li>");
                nwavIndice++;
            } else if (nwavTabla) {
                nwavHtml.append("<table><thead><tr>");
                nwavCeldas(nwavLinea).forEach(nwavC -> nwavHtml.append("<th>").append(nwavEnLinea(nwavC)).append("</th>"));
                nwavHtml.append("</tr></thead><tbody>");
                nwavIndice += 2;
                while (nwavIndice < nwavLineas.size() && nwavLineas.get(nwavIndice).strip().startsWith("|")) {
                    nwavHtml.append("<tr>");
                    nwavCeldas(nwavLineas.get(nwavIndice))
                            .forEach(nwavC -> nwavHtml.append("<td>").append(nwavEnLinea(nwavC)).append("</td>"));
                    nwavHtml.append("</tr>");
                    nwavIndice++;
                }
                nwavHtml.append("</tbody></table>");
            } else {
                nwavParrafo.add(nwavLinea.strip());
                nwavIndice++;
            }
        }
        nwavCerrarParrafo(nwavHtml, nwavParrafo);
        if (nwavLista != null) {
            nwavHtml.append("</").append(nwavLista).append('>');
        }
        return nwavHtml.append("</body></html>").toString();
    }

    private static void nwavCerrarParrafo(StringBuilder nwavHtml, List<String> nwavParrafo) {
        if (!nwavParrafo.isEmpty()) {
            nwavHtml.append("<p>").append(nwavEnLinea(String.join(" ", nwavParrafo))).append("</p>");
            nwavParrafo.clear();
        }
    }

    private static List<String> nwavCeldas(String nwavLinea) {
        String nwavLimpia = nwavLinea.strip();
        if (nwavLimpia.startsWith("|")) {
            nwavLimpia = nwavLimpia.substring(1);
        }
        if (nwavLimpia.endsWith("|")) {
            nwavLimpia = nwavLimpia.substring(0, nwavLimpia.length() - 1);
        }
        List<String> nwavCeldas = new ArrayList<>();
        for (String nwavCelda : nwavLimpia.split("(?<!\\\\)\\|", -1)) {
            nwavCeldas.add(nwavCelda.strip().replace("\\|", "|"));
        }
        return nwavCeldas;
    }

    static String nwavEnLinea(String nwavTexto) {
        String nwavHtml = nwavEscapar(nwavTexto);
        nwavHtml = nwavHtml.replaceAll("\\[([^\\]]+)]\\((https?://[^)\\s]+)\\)", "<a href=\"$2\">$1</a>");
        nwavHtml = nwavHtml.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        nwavHtml = nwavHtml.replaceAll("(?<![*\\w])\\*(?!\\s)(.+?)(?<!\\s)\\*(?![*\\w])", "<em>$1</em>");
        return nwavHtml;
    }

    static String nwavEscapar(String nwavTexto) {
        return nwavTexto == null ? "" : nwavTexto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
