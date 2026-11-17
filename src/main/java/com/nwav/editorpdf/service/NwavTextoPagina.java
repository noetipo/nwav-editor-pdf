package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavRectangulo;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
final class NwavTextoPagina {
    record NwavGlifo(String nwavTexto, double nwavX, double nwavArriba, double nwavDerecha, double nwavAbajo,
                     double nwavBase, double nwavTamano) {
        static NwavGlifo nwavDe(TextPosition nwavPosicion) {
            double nwavTamano = Math.max(nwavPosicion.getFontSizeInPt(), nwavPosicion.getHeight());
            double nwavBase = nwavPosicion.getY();
            return new NwavGlifo(nwavPosicion.getUnicode(), nwavPosicion.getX(), nwavBase - nwavTamano * 0.8,
                    nwavPosicion.getX() + nwavPosicion.getWidth(), nwavBase + nwavTamano * 0.22, nwavBase, nwavTamano);
        }

        double nwavCentroX() {
            return (nwavX + nwavDerecha) / 2;
        }
    }

    private NwavTextoPagina() {
    }

    static List<NwavGlifo> nwavLetras(PDDocument nwavPd, int nwavPagina) {
        List<NwavGlifo> nwavLetras = new ArrayList<>();
        try {
            PDFTextStripper nwavExtractor = new PDFTextStripper() {
                @Override
                protected void writeString(String nwavTexto, List<TextPosition> nwavPosiciones) {
                    for (TextPosition nwavPosicion : nwavPosiciones) {
                        if (nwavPosicion.getUnicode() != null) {
                            nwavLetras.add(NwavGlifo.nwavDe(nwavPosicion));
                        }
                    }
                    nwavLetras.add(null);
                }
            };
            nwavExtractor.setSortByPosition(true);
            nwavExtractor.setStartPage(nwavPagina + 1);
            nwavExtractor.setEndPage(nwavPagina + 1);
            nwavExtractor.getText(nwavPd);
        } catch (IOException | RuntimeException nwavError) {
            nwavLog.debug("No se pudo leer el texto de la pagina {}", nwavPagina + 1, nwavError);
        }
        return nwavLetras;
    }

    static List<NwavRectangulo> nwavAgruparEnLineas(List<NwavGlifo> nwavGlifos) {
        List<NwavGlifo> nwavOrdenados = new ArrayList<>(nwavGlifos);
        nwavOrdenados.sort(Comparator.comparingDouble(NwavGlifo::nwavBase).thenComparingDouble(NwavGlifo::nwavX));
        List<NwavRectangulo> nwavLineas = new ArrayList<>();
        List<NwavGlifo> nwavLinea = new ArrayList<>();
        for (NwavGlifo nwavGlifo : nwavOrdenados) {
            if (!nwavLinea.isEmpty()
                    && Math.abs(nwavGlifo.nwavBase() - nwavLinea.get(0).nwavBase()) > nwavLinea.get(0).nwavTamano() * 0.5) {
                nwavLineas.add(nwavCajaDe(nwavLinea));
                nwavLinea.clear();
            }
            nwavLinea.add(nwavGlifo);
        }
        if (!nwavLinea.isEmpty()) {
            nwavLineas.add(nwavCajaDe(nwavLinea));
        }
        return nwavLineas;
    }

    private static NwavRectangulo nwavCajaDe(List<NwavGlifo> nwavLinea) {
        double nwavX1 = nwavLinea.stream().mapToDouble(NwavGlifo::nwavX).min().orElse(0);
        double nwavY1 = nwavLinea.stream().mapToDouble(NwavGlifo::nwavArriba).min().orElse(0);
        double nwavX2 = nwavLinea.stream().mapToDouble(NwavGlifo::nwavDerecha).max().orElse(0);
        double nwavY2 = nwavLinea.stream().mapToDouble(NwavGlifo::nwavAbajo).max().orElse(0);
        return new NwavRectangulo(nwavX1, nwavY1, nwavX2 - nwavX1, nwavY2 - nwavY1);
    }
}
