package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.model.NwavCoincidencia;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.service.NwavTextoPagina.NwavGlifo;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

@Service
public class NwavBusquedaService {
    public static final int NWAV_MAXIMO_RESULTADOS = 5000;
    private static final int NWAV_CONTEXTO = 35;

    public List<NwavCoincidencia> nwavBuscar(NwavDocumentoPdf nwavDocumento, String nwavConsulta,
                                             boolean nwavDistinguirMayusculas, NwavAvance nwavAvance,
                                             BooleanSupplier nwavCancelada) {
        String nwavBuscado = nwavNormalizar(nwavConsulta == null ? "" : nwavConsulta.strip().replaceAll("\\s+", " "),
                nwavDistinguirMayusculas);
        if (nwavBuscado.isEmpty()) {
            throw new NwavEditorException("Escribe el texto que quieres buscar.");
        }
        List<NwavCoincidencia> nwavResultados = new ArrayList<>();
        int nwavTotal = nwavDocumento.getNwavNumeroPaginas();
        for (int nwavPagina = 0; nwavPagina < nwavTotal; nwavPagina++) {
            if (nwavCancelada.getAsBoolean() || nwavResultados.size() >= NWAV_MAXIMO_RESULTADOS) {
                break;
            }
            nwavAvance.nwavInformar(nwavPagina, nwavTotal, "Buscando en la página " + (nwavPagina + 1) + "…");
            List<NwavGlifo> nwavLetras;
            synchronized (nwavDocumento) {
                nwavDocumento.nwavVerificarAbierto();
                nwavLetras = NwavTextoPagina.nwavLetras(nwavDocumento.nwavPdDocument(), nwavPagina);
            }
            nwavBuscarEnPagina(nwavPagina, nwavLetras, nwavBuscado, nwavDistinguirMayusculas, nwavResultados);
        }
        nwavAvance.nwavInformar(nwavTotal, nwavTotal, "Listo");
        return nwavResultados;
    }

    private static void nwavBuscarEnPagina(int nwavPagina, List<NwavGlifo> nwavLetras, String nwavBuscado,
                                           boolean nwavDistinguir, List<NwavCoincidencia> nwavResultados) {
        StringBuilder nwavTexto = new StringBuilder();
        StringBuilder nwavOriginal = new StringBuilder();
        List<Integer> nwavOrigen = new ArrayList<>();
        for (int nwavIndice = 0; nwavIndice < nwavLetras.size(); nwavIndice++) {
            NwavGlifo nwavGlifo = nwavLetras.get(nwavIndice);
            String nwavPieza = nwavGlifo == null ? " " : nwavGlifo.nwavTexto();
            String nwavNormal = nwavPieza.isBlank() ? " " : nwavNormalizar(nwavPieza, nwavDistinguir);
            for (char nwavCaracter : nwavNormal.toCharArray()) {
                boolean nwavEsEspacio = Character.isWhitespace(nwavCaracter);
                if (nwavEsEspacio && (nwavTexto.isEmpty() || nwavTexto.charAt(nwavTexto.length() - 1) == ' ')) {
                    continue;
                }
                nwavTexto.append(nwavEsEspacio ? ' ' : nwavCaracter);
                nwavOriginal.append(nwavEsEspacio ? ' ' : nwavPieza.charAt(0));
                nwavOrigen.add(nwavGlifo == null || nwavEsEspacio ? -1 : nwavIndice);
            }
        }
        int nwavDesde = 0;
        while (nwavResultados.size() < NWAV_MAXIMO_RESULTADOS) {
            int nwavEncontrado = nwavTexto.indexOf(nwavBuscado, nwavDesde);
            if (nwavEncontrado < 0) {
                break;
            }
            int nwavFin = nwavEncontrado + nwavBuscado.length();
            List<NwavGlifo> nwavGlifos = new ArrayList<>();
            for (int nwavPosicion = nwavEncontrado; nwavPosicion < nwavFin; nwavPosicion++) {
                int nwavLetra = nwavOrigen.get(nwavPosicion);
                if (nwavLetra >= 0 && !nwavGlifos.contains(nwavLetras.get(nwavLetra))) {
                    nwavGlifos.add(nwavLetras.get(nwavLetra));
                }
            }
            List<NwavRectangulo> nwavZonas = NwavTextoPagina.nwavAgruparEnLineas(nwavGlifos);
            String nwavContexto = nwavOriginal.substring(Math.max(0, nwavEncontrado - NWAV_CONTEXTO),
                    Math.min(nwavOriginal.length(), nwavFin + NWAV_CONTEXTO)).strip();
            nwavResultados.add(new NwavCoincidencia(nwavPagina, nwavZonas, nwavContexto));
            nwavDesde = nwavFin;
        }
    }

    static String nwavNormalizar(String nwavTexto, boolean nwavDistinguirMayusculas) {
        StringBuilder nwavResultado = new StringBuilder();
        for (char nwavCaracter : nwavTexto.toCharArray()) {
            if (nwavCaracter == 'ñ' || nwavCaracter == 'Ñ') {
                nwavResultado.append(nwavCaracter);
                continue;
            }
            String nwavSinTilde = Normalizer.normalize(String.valueOf(nwavCaracter), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "");
            nwavResultado.append(nwavSinTilde);
        }
        String nwavSalida = nwavResultado.toString();
        return nwavDistinguirMayusculas ? nwavSalida : nwavSalida.toLowerCase(Locale.ROOT);
    }
}
