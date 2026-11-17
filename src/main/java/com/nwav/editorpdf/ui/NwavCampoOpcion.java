package com.nwav.editorpdf.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public final class NwavCampoOpcion {
    public enum NwavTipo {
        NWAV_ELECCION, NWAV_ENTERO, NWAV_DECIMAL, NWAV_CASILLA, NWAV_TEXTO, NWAV_CONTRASENA, NWAV_MULTIPLE, NWAV_COLOR,
        NWAV_ARCHIVO
    }

    private final String nwavClave;
    private final String nwavEtiqueta;
    private final NwavTipo nwavTipo;
    private final String nwavDefecto;
    private Supplier<List<String>> nwavValores = List::of;
    private double nwavMinimo;
    private double nwavMaximo = 100;
    private double nwavPaso = 1;
    private String nwavAyuda;
    private String nwavSiClave;
    private Set<String> nwavSiValores = Set.of();

    private NwavCampoOpcion(String nwavClave, String nwavEtiqueta, NwavTipo nwavTipo, String nwavDefecto) {
        this.nwavClave = nwavClave;
        this.nwavEtiqueta = nwavEtiqueta;
        this.nwavTipo = nwavTipo;
        this.nwavDefecto = nwavDefecto;
    }

    public static NwavCampoOpcion nwavEleccion(String nwavClave, String nwavEtiqueta, String nwavDefecto,
                                               String... nwavValores) {
        NwavCampoOpcion nwavCampo = new NwavCampoOpcion(nwavClave, nwavEtiqueta, NwavTipo.NWAV_ELECCION, nwavDefecto);
        List<String> nwavLista = List.of(nwavValores);
        nwavCampo.nwavValores = () -> nwavLista;
        return nwavCampo;
    }

    public static NwavCampoOpcion nwavEntero(String nwavClave, String nwavEtiqueta, int nwavDefecto, int nwavMinimo,
                                             int nwavMaximo) {
        NwavCampoOpcion nwavCampo = new NwavCampoOpcion(nwavClave, nwavEtiqueta, NwavTipo.NWAV_ENTERO,
                String.valueOf(nwavDefecto));
        nwavCampo.nwavMinimo = nwavMinimo;
        nwavCampo.nwavMaximo = nwavMaximo;
        return nwavCampo;
    }

    public static NwavCampoOpcion nwavDecimal(String nwavClave, String nwavEtiqueta, double nwavDefecto, double nwavMinimo,
                                              double nwavMaximo, double nwavPaso) {
        NwavCampoOpcion nwavCampo = new NwavCampoOpcion(nwavClave, nwavEtiqueta, NwavTipo.NWAV_DECIMAL,
                String.valueOf(nwavDefecto));
        nwavCampo.nwavMinimo = nwavMinimo;
        nwavCampo.nwavMaximo = nwavMaximo;
        nwavCampo.nwavPaso = nwavPaso;
        return nwavCampo;
    }

    public static NwavCampoOpcion nwavCasilla(String nwavClave, String nwavEtiqueta, boolean nwavDefecto) {
        return new NwavCampoOpcion(nwavClave, nwavEtiqueta, NwavTipo.NWAV_CASILLA, String.valueOf(nwavDefecto));
    }

    public static NwavCampoOpcion nwavTexto(String nwavClave, String nwavEtiqueta, String nwavDefecto) {
        return new NwavCampoOpcion(nwavClave, nwavEtiqueta, NwavTipo.NWAV_TEXTO, nwavDefecto);
    }

    public static NwavCampoOpcion nwavContrasena(String nwavClave, String nwavEtiqueta) {
        return new NwavCampoOpcion(nwavClave, nwavEtiqueta, NwavTipo.NWAV_CONTRASENA, "");
    }

    public static NwavCampoOpcion nwavColor(String nwavClave, String nwavEtiqueta, String nwavDefectoHex) {
        return new NwavCampoOpcion(nwavClave, nwavEtiqueta, NwavTipo.NWAV_COLOR, nwavDefectoHex);
    }

    public static NwavCampoOpcion nwavArchivo(String nwavClave, String nwavEtiqueta, String... nwavExtensiones) {
        NwavCampoOpcion nwavCampo = new NwavCampoOpcion(nwavClave, nwavEtiqueta, NwavTipo.NWAV_ARCHIVO, "");
        List<String> nwavLista = List.of(nwavExtensiones);
        nwavCampo.nwavValores = () -> nwavLista;
        return nwavCampo;
    }

    public static NwavCampoOpcion nwavMultiple(String nwavClave, String nwavEtiqueta, String nwavDefecto,
                                               Supplier<List<String>> nwavValores) {
        NwavCampoOpcion nwavCampo = new NwavCampoOpcion(nwavClave, nwavEtiqueta, NwavTipo.NWAV_MULTIPLE, nwavDefecto);
        nwavCampo.nwavValores = nwavValores;
        return nwavCampo;
    }

    public NwavCampoOpcion nwavSi(String nwavOtraClave, String... nwavValoresQueMuestran) {
        this.nwavSiClave = nwavOtraClave;
        this.nwavSiValores = Set.of(nwavValoresQueMuestran);
        return this;
    }

    public NwavCampoOpcion nwavConAyuda(String nwavTexto) {
        this.nwavAyuda = nwavTexto;
        return this;
    }

    public String getNwavClave() {
        return nwavClave;
    }

    public String getNwavEtiqueta() {
        return nwavEtiqueta;
    }

    public NwavTipo getNwavTipo() {
        return nwavTipo;
    }

    public String getNwavDefecto() {
        return nwavDefecto;
    }

    public List<String> nwavValores() {
        return new ArrayList<>(nwavValores.get());
    }

    public double getNwavMinimo() {
        return nwavMinimo;
    }

    public double getNwavMaximo() {
        return nwavMaximo;
    }

    public double getNwavPaso() {
        return nwavPaso;
    }

    public String getNwavAyuda() {
        return nwavAyuda;
    }

    public String getNwavSiClave() {
        return nwavSiClave;
    }

    public Set<String> getNwavSiValores() {
        return nwavSiValores;
    }

    public boolean nwavSeGuarda() {
        return nwavTipo != NwavTipo.NWAV_CONTRASENA;
    }
}
