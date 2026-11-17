package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public final class NwavRangosPaginas {
    public record NwavRango(int nwavDesde, int nwavHasta) {
        public List<Integer> nwavIndices() {
            return IntStream.rangeClosed(nwavDesde, nwavHasta).boxed().toList();
        }

        public String nwavDescripcion() {
            return nwavDesde == nwavHasta ? String.valueOf(nwavDesde + 1) : (nwavDesde + 1) + "-" + (nwavHasta + 1);
        }

        public int nwavCantidad() {
            return nwavHasta - nwavDesde + 1;
        }
    }

    private NwavRangosPaginas() {
    }

    public static List<NwavRango> nwavInterpretar(String nwavTexto, int nwavTotal) {
        if (nwavTexto == null || nwavTexto.isBlank()) {
            throw new NwavEditorException("Escribe las páginas, por ejemplo: 1-3, 5, 8-10.");
        }
        List<NwavRango> nwavRangos = new ArrayList<>();
        for (String nwavParte : nwavTexto.split("[,;]")) {
            String nwavLimpia = nwavParte.replace(" ", "").replace('–', '-');
            if (nwavLimpia.isEmpty()) {
                continue;
            }
            if (!nwavLimpia.matches("\\d*-?\\d*") || nwavLimpia.equals("-")) {
                throw new NwavEditorException("«" + nwavParte.trim()
                        + "» no es un rango válido. Usa números y guiones, por ejemplo: 1-3, 5, 8-10.");
            }
            int nwavGuion = nwavLimpia.indexOf('-');
            int nwavDesde;
            int nwavHasta;
            if (nwavGuion < 0) {
                nwavDesde = nwavHasta = nwavNumero(nwavLimpia, nwavTotal);
            } else {
                String nwavIzquierda = nwavLimpia.substring(0, nwavGuion);
                String nwavDerecha = nwavLimpia.substring(nwavGuion + 1);
                nwavDesde = nwavIzquierda.isEmpty() ? 1 : nwavNumero(nwavIzquierda, nwavTotal);
                nwavHasta = nwavDerecha.isEmpty() ? nwavTotal : nwavNumero(nwavDerecha, nwavTotal);
            }
            if (nwavDesde > nwavHasta) {
                throw new NwavEditorException("En «" + nwavParte.trim()
                        + "» la primera página debe ser menor o igual que la última.");
            }
            nwavRangos.add(new NwavRango(nwavDesde - 1, nwavHasta - 1));
        }
        if (nwavRangos.isEmpty()) {
            throw new NwavEditorException("Escribe las páginas, por ejemplo: 1-3, 5, 8-10.");
        }
        return nwavRangos;
    }

    public static List<Integer> nwavIndices(String nwavTexto, int nwavTotal) {
        Set<Integer> nwavUnicos = new LinkedHashSet<>();
        nwavInterpretar(nwavTexto, nwavTotal).forEach(nwavRango -> nwavUnicos.addAll(nwavRango.nwavIndices()));
        return List.copyOf(nwavUnicos);
    }

    public static List<NwavRango> nwavCada(int nwavTamano, int nwavTotal) {
        if (nwavTamano < 1) {
            throw new NwavEditorException("El número de páginas por archivo debe ser al menos 1.");
        }
        List<NwavRango> nwavRangos = new ArrayList<>();
        for (int nwavDesde = 0; nwavDesde < nwavTotal; nwavDesde += nwavTamano) {
            nwavRangos.add(new NwavRango(nwavDesde, Math.min(nwavTotal, nwavDesde + nwavTamano) - 1));
        }
        return nwavRangos;
    }

    public static String nwavDescribir(List<Integer> nwavIndices) {
        List<Integer> nwavOrdenados = nwavIndices.stream().distinct().sorted().toList();
        StringBuilder nwavTexto = new StringBuilder();
        int nwavPosicion = 0;
        while (nwavPosicion < nwavOrdenados.size()) {
            int nwavInicio = nwavOrdenados.get(nwavPosicion);
            int nwavFin = nwavInicio;
            while (nwavPosicion + 1 < nwavOrdenados.size() && nwavOrdenados.get(nwavPosicion + 1) == nwavFin + 1) {
                nwavFin = nwavOrdenados.get(++nwavPosicion);
            }
            if (!nwavTexto.isEmpty()) {
                nwavTexto.append(", ");
            }
            nwavTexto.append(new NwavRango(nwavInicio, nwavFin).nwavDescripcion());
            nwavPosicion++;
        }
        return nwavTexto.toString();
    }

    private static int nwavNumero(String nwavTexto, int nwavTotal) {
        int nwavValor;
        try {
            nwavValor = Integer.parseInt(nwavTexto);
        } catch (NumberFormatException nwavError) {
            throw new NwavEditorException("«" + nwavTexto + "» no es un número de página válido.");
        }
        if (nwavValor < 1 || nwavValor > nwavTotal) {
            throw new NwavEditorException("La página " + nwavValor + " no existe: el documento tiene "
                    + nwavTotal + (nwavTotal == 1 ? " página." : " páginas."));
        }
        return nwavValor;
    }
}
