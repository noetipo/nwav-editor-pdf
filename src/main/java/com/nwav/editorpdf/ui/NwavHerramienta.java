package com.nwav.editorpdf.ui;

import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;

/** Herramientas de edicion del visor. */
public enum NwavHerramienta {
    NWAV_NAVEGAR("Navegar", Feather.MOUSE_POINTER, null,
            "Desplázate por el documento. Elige una herramienta para editar la página."),
    NWAV_TEXTO("Cuadro de texto", Feather.TYPE, Color.BLACK,
            "Haz clic (o arrastra para fijar el ancho) y escribe. Ctrl/⌘ + Intro o ✓ para fijarlo en la página."),
    NWAV_IMAGEN("Imagen", Feather.IMAGE, null,
            "Haz clic donde irá la imagen y elígela. Arrástrala para moverla y usa la esquina para el tamaño; luego ✓."),
    NWAV_RESALTAR("Resaltar", Feather.EDIT_3, Color.rgb(255, 221, 0),
            "Arrastra sobre el texto: el resaltado se ajusta a las líneas de texto."),
    NWAV_SUBRAYAR("Subrayar", Feather.UNDERLINE, Color.rgb(37, 99, 235),
            "Arrastra sobre el texto que quieres subrayar."),
    NWAV_TACHAR("Tachar", Feather.SLASH, Color.rgb(220, 38, 38),
            "Arrastra sobre el texto que quieres tachar."),
    NWAV_NOTA("Nota", Feather.MESSAGE_SQUARE, Color.rgb(255, 196, 0),
            "Haz clic donde quieras la nota y escribe su texto. Otros lectores la muestran como nota desplegable."),
    NWAV_DIBUJO("Dibujo a mano alzada", Feather.PEN_TOOL, Color.rgb(220, 38, 38),
            "Arrastra para dibujar. Cada trazo se puede deshacer por separado."),
    NWAV_RECTANGULO("Rectángulo", Feather.SQUARE, Color.rgb(220, 38, 38),
            "Arrastra para dibujar un rectángulo."),
    NWAV_ELIPSE("Elipse", Feather.CIRCLE, Color.rgb(220, 38, 38),
            "Arrastra para dibujar una elipse."),
    NWAV_LINEA("Línea", Feather.MINUS, Color.rgb(220, 38, 38),
            "Arrastra desde el inicio hasta el final de la línea."),
    NWAV_FLECHA("Flecha", Feather.ARROW_UP_RIGHT, Color.rgb(220, 38, 38),
            "Arrastra desde el inicio hasta la punta de la flecha."),
    NWAV_RECORTAR("Recortar", Feather.CROP, null,
            "Arrastra para marcar el área que quieres conservar; luego elige si se aplica a esta página o a todas."),
    NWAV_CUBRIR("Cubrir zona", Feather.EYE_OFF, Color.WHITE,
            "Cobertura VISUAL: tapa la zona con un rectángulo, pero el contenido de debajo NO se elimina; "
                    + "sigue en el PDF y se puede copiar o buscar.");

    private final String nwavDescripcion;
    private final Feather nwavIcono;
    private final Color nwavColorInicial;
    private final String nwavAyuda;

    NwavHerramienta(String nwavDescripcion, Feather nwavIcono, Color nwavColorInicial, String nwavAyuda) {
        this.nwavDescripcion = nwavDescripcion;
        this.nwavIcono = nwavIcono;
        this.nwavColorInicial = nwavColorInicial;
        this.nwavAyuda = nwavAyuda;
    }

    public String getNwavDescripcion() {
        return nwavDescripcion;
    }

    public Feather getNwavIcono() {
        return nwavIcono;
    }

    public Color getNwavColorInicial() {
        return nwavColorInicial;
    }

    public String getNwavAyuda() {
        return nwavAyuda;
    }

    public boolean nwavUsaColor() {
        return nwavColorInicial != null;
    }

    public boolean nwavUsaGrosor() {
        return this == NWAV_DIBUJO || this == NWAV_RECTANGULO || this == NWAV_ELIPSE
                || this == NWAV_LINEA || this == NWAV_FLECHA;
    }

    public boolean nwavUsaTexto() {
        return this == NWAV_TEXTO;
    }

    /** Herramientas que se usan arrastrando un rectangulo. */
    public boolean nwavEsDeZona() {
        return this == NWAV_RESALTAR || this == NWAV_SUBRAYAR || this == NWAV_TACHAR || this == NWAV_RECTANGULO
                || this == NWAV_ELIPSE || this == NWAV_CUBRIR || this == NWAV_TEXTO || this == NWAV_RECORTAR;
    }
}
