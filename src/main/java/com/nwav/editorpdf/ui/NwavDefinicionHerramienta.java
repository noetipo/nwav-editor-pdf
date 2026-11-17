package com.nwav.editorpdf.ui;

import com.nwav.editorpdf.service.herramienta.NwavDependenciasService;
import com.nwav.editorpdf.service.herramienta.NwavEjecutorHerramientasService.NwavTrabajo;
import javafx.scene.Node;
import org.kordamp.ikonli.feather.Feather;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

public final class NwavDefinicionHerramienta {
    private final String nwavId;
    private final String nwavNombre;
    private final String nwavDescripcion;
    private final NwavCategoriaHerramienta nwavCategoria;
    private final Feather nwavIcono;
    private NwavModoHerramienta nwavModo = NwavModoHerramienta.NWAV_LOTE;
    private List<String> nwavExtensiones = List.of("pdf");
    private boolean nwavVariosArchivos = true;
    private int nwavMinimoArchivos = 1;
    private boolean nwavOrdenImporta;
    private final List<NwavCampoOpcion> nwavOpciones = new ArrayList<>();
    private NwavTrabajo nwavTrabajo;
    private Supplier<NwavDependenciasService.NwavEstado> nwavDependencia;
    private Function<NwavContextoHerramienta, Node> nwavExtra;
    private Runnable nwavAccionEditor;
    private String nwavVista;
    private String nwavSiguiente;
    private String nwavTextoSiguiente;
    private String nwavPalabrasClave = "";
    private String nwavAviso;
    private boolean nwavNecesitaDocumento = true;

    public NwavDefinicionHerramienta(String nwavId, String nwavNombre, String nwavDescripcion,
                                     NwavCategoriaHerramienta nwavCategoria, Feather nwavIcono) {
        this.nwavId = nwavId;
        this.nwavNombre = nwavNombre;
        this.nwavDescripcion = nwavDescripcion;
        this.nwavCategoria = nwavCategoria;
        this.nwavIcono = nwavIcono;
    }

    public NwavDefinicionHerramienta nwavLote(NwavTrabajo nwavTrabajo) {
        this.nwavModo = NwavModoHerramienta.NWAV_LOTE;
        this.nwavTrabajo = nwavTrabajo;
        return this;
    }

    public NwavDefinicionHerramienta nwavEditor(Runnable nwavAccion) {
        this.nwavModo = NwavModoHerramienta.NWAV_EDITOR;
        this.nwavAccionEditor = nwavAccion;
        return this;
    }

    public NwavDefinicionHerramienta nwavSinDocumento() {
        this.nwavNecesitaDocumento = false;
        return this;
    }

    public boolean isNwavNecesitaDocumento() {
        return nwavNecesitaDocumento;
    }

    public NwavDefinicionHerramienta nwavVistaPropia(String nwavNombreVista) {
        this.nwavModo = NwavModoHerramienta.NWAV_VISTA;
        this.nwavVista = nwavNombreVista;
        return this;
    }

    public NwavDefinicionHerramienta nwavConExtensiones(String... nwavValores) {
        this.nwavExtensiones = List.of(nwavValores);
        return this;
    }

    public NwavDefinicionHerramienta nwavUnSoloArchivo() {
        this.nwavVariosArchivos = false;
        return this;
    }

    public NwavDefinicionHerramienta nwavSinArchivos() {
        this.nwavMinimoArchivos = 0;
        return this;
    }

    public NwavDefinicionHerramienta nwavCombina(int nwavMinimo) {
        this.nwavMinimoArchivos = nwavMinimo;
        this.nwavOrdenImporta = true;
        return this;
    }

    public NwavDefinicionHerramienta nwavConOpciones(NwavCampoOpcion... nwavCampos) {
        this.nwavOpciones.addAll(List.of(nwavCampos));
        return this;
    }

    public NwavDefinicionHerramienta nwavRequiere(Supplier<NwavDependenciasService.NwavEstado> nwavEstado) {
        this.nwavDependencia = nwavEstado;
        return this;
    }

    public NwavDefinicionHerramienta nwavConExtra(Function<NwavContextoHerramienta, Node> nwavFabrica) {
        this.nwavExtra = nwavFabrica;
        return this;
    }

    public NwavDefinicionHerramienta nwavOfrecer(String nwavOtraHerramienta, String nwavTexto) {
        this.nwavSiguiente = nwavOtraHerramienta;
        this.nwavTextoSiguiente = nwavTexto;
        return this;
    }

    public NwavDefinicionHerramienta nwavConPalabrasClave(String nwavPalabras) {
        this.nwavPalabrasClave = nwavPalabras;
        return this;
    }

    public NwavDefinicionHerramienta nwavConAviso(String nwavTexto) {
        this.nwavAviso = nwavTexto;
        return this;
    }

    public String getNwavId() {
        return nwavId;
    }

    public String getNwavNombre() {
        return nwavNombre;
    }

    public String getNwavDescripcion() {
        return nwavDescripcion;
    }

    public NwavCategoriaHerramienta getNwavCategoria() {
        return nwavCategoria;
    }

    public Feather getNwavIcono() {
        return nwavIcono;
    }

    public NwavModoHerramienta getNwavModo() {
        return nwavModo;
    }

    public List<String> getNwavExtensiones() {
        return nwavExtensiones;
    }

    public boolean isNwavVariosArchivos() {
        return nwavVariosArchivos;
    }

    public int getNwavMinimoArchivos() {
        return nwavMinimoArchivos;
    }

    public boolean isNwavOrdenImporta() {
        return nwavOrdenImporta;
    }

    public List<NwavCampoOpcion> getNwavOpciones() {
        return nwavOpciones;
    }

    public NwavTrabajo getNwavTrabajo() {
        return nwavTrabajo;
    }

    public Runnable getNwavAccionEditor() {
        return nwavAccionEditor;
    }

    public String getNwavVista() {
        return nwavVista;
    }

    public String getNwavSiguiente() {
        return nwavSiguiente;
    }

    public String getNwavTextoSiguiente() {
        return nwavTextoSiguiente;
    }

    public String getNwavAviso() {
        return nwavAviso;
    }

    public Function<NwavContextoHerramienta, Node> getNwavExtra() {
        return nwavExtra;
    }

    public NwavDependenciasService.NwavEstado nwavEstado() {
        return nwavDependencia == null ? new NwavDependenciasService.NwavEstado(true, "", "") : nwavDependencia.get();
    }

    public boolean nwavCoincide(String nwavBusqueda) {
        if (nwavBusqueda == null || nwavBusqueda.isBlank()) {
            return true;
        }
        String nwavTexto = java.text.Normalizer.normalize((nwavNombre + " " + nwavDescripcion + " " + nwavPalabrasClave + " "
                + nwavCategoria.getNwavNombre()), java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        String nwavConsulta = java.text.Normalizer.normalize(nwavBusqueda.strip(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        for (String nwavParte : nwavConsulta.split("\\s+")) {
            if (!nwavTexto.contains(nwavParte)) {
                return false;
            }
        }
        return true;
    }

    public boolean nwavAceptaArchivo(String nwavNombreArchivo) {
        String nwavMinusculas = nwavNombreArchivo.toLowerCase(Locale.ROOT);
        return nwavExtensiones.stream().anyMatch(nwavExtension -> nwavMinusculas.endsWith("." + nwavExtension));
    }
}
