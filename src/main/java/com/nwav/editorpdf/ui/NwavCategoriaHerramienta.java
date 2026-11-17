package com.nwav.editorpdf.ui;

/** Categorias de la pantalla de inicio. */
public enum NwavCategoriaHerramienta {
    NWAV_ORGANIZAR("Organizar", "nwav-cat-organizar"),
    NWAV_OPTIMIZAR("Optimizar", "nwav-cat-optimizar"),
    NWAV_CONVERTIR_A_PDF("Convertir a PDF", "nwav-cat-a-pdf"),
    NWAV_CONVERTIR_DESDE_PDF("Convertir desde PDF", "nwav-cat-desde-pdf"),
    NWAV_EDITAR("Editar", "nwav-cat-editar"),
    NWAV_SEGURIDAD("Seguridad", "nwav-cat-seguridad"),
    NWAV_IA("Inteligencia artificial (opcional)", "nwav-cat-ia");

    private final String nwavNombre;
    private final String nwavClaseEstilo;

    NwavCategoriaHerramienta(String nwavNombre, String nwavClaseEstilo) {
        this.nwavNombre = nwavNombre;
        this.nwavClaseEstilo = nwavClaseEstilo;
    }

    public String getNwavNombre() {
        return nwavNombre;
    }

    public String getNwavClaseEstilo() {
        return nwavClaseEstilo;
    }
}
