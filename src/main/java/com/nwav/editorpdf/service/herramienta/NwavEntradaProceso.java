package com.nwav.editorpdf.service.herramienta;

import com.nwav.editorpdf.service.NwavAvance;
import com.nwav.editorpdf.service.NwavCancelacion;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record NwavEntradaProceso(List<Path> nwavArchivos, Map<String, String> nwavOpciones, Path nwavCarpetaSalida,
                                 Map<Path, String> nwavContrasenas, NwavAvance nwavAvance,
                                 NwavCancelacion nwavCancelacion) {
    public Path nwavArchivo() {
        return nwavArchivos.get(0);
    }

    public String nwavContrasena(Path nwavArchivo) {
        return nwavContrasenas == null ? null : nwavContrasenas.get(nwavArchivo);
    }

    public String nwavTexto(String nwavClave, String nwavPorDefecto) {
        String nwavValor = nwavOpciones.get(nwavClave);
        return nwavValor == null || nwavValor.isBlank() ? nwavPorDefecto : nwavValor;
    }

    public int nwavEntero(String nwavClave, int nwavPorDefecto) {
        try {
            return Integer.parseInt(nwavTexto(nwavClave, String.valueOf(nwavPorDefecto)).trim());
        } catch (NumberFormatException nwavError) {
            return nwavPorDefecto;
        }
    }

    public double nwavDecimal(String nwavClave, double nwavPorDefecto) {
        try {
            return Double.parseDouble(nwavTexto(nwavClave, String.valueOf(nwavPorDefecto)).trim().replace(',', '.'));
        } catch (NumberFormatException nwavError) {
            return nwavPorDefecto;
        }
    }

    public boolean nwavSiNo(String nwavClave, boolean nwavPorDefecto) {
        return Boolean.parseBoolean(nwavTexto(nwavClave, String.valueOf(nwavPorDefecto)));
    }
}
