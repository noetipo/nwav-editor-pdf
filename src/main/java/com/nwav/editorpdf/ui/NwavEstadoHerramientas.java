package com.nwav.editorpdf.ui;

import com.nwav.editorpdf.model.NwavColorRgb;
import com.nwav.editorpdf.model.NwavEstiloTexto;
import com.nwav.editorpdf.model.NwavFuente;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import java.util.EnumMap;
import java.util.Map;

/**
 * Herramienta elegida y sus opciones (color, grosor, letra). Cada herramienta recuerda el
 * ultimo color usado con ella: cambiar de resaltar a tachar no arrastra el amarillo.
 */
public class NwavEstadoHerramientas {

    private final ObjectProperty<NwavHerramienta> nwavHerramienta =
            new SimpleObjectProperty<>(NwavHerramienta.NWAV_NAVEGAR);
    private final ObjectProperty<Color> nwavColor = new SimpleObjectProperty<>(Color.BLACK);
    private final DoubleProperty nwavGrosor = new SimpleDoubleProperty(2);
    private final ObjectProperty<NwavFuente> nwavFuente = new SimpleObjectProperty<>(NwavFuente.NWAV_HELVETICA);
    private final DoubleProperty nwavTamano = new SimpleDoubleProperty(14);
    private final BooleanProperty nwavNegrita = new SimpleBooleanProperty(false);
    private final BooleanProperty nwavCursiva = new SimpleBooleanProperty(false);
    private final Map<NwavHerramienta, Color> nwavColoresUsados = new EnumMap<>(NwavHerramienta.class);

    public NwavEstadoHerramientas() {
        nwavHerramienta.addListener((nwavObs, nwavAntes, nwavAhora) -> {
            if (nwavAntes != null && nwavAntes.nwavUsaColor()) {
                nwavColoresUsados.put(nwavAntes, nwavColor.get());
            }
            if (nwavAhora != null && nwavAhora.nwavUsaColor()) {
                nwavColor.set(nwavColoresUsados.getOrDefault(nwavAhora, nwavAhora.getNwavColorInicial()));
            }
        });
    }

    public ObjectProperty<NwavHerramienta> nwavHerramientaProperty() {
        return nwavHerramienta;
    }

    public NwavHerramienta nwavHerramienta() {
        return nwavHerramienta.get();
    }

    public ObjectProperty<Color> nwavColorProperty() {
        return nwavColor;
    }

    public DoubleProperty nwavGrosorProperty() {
        return nwavGrosor;
    }

    public ObjectProperty<NwavFuente> nwavFuenteProperty() {
        return nwavFuente;
    }

    public DoubleProperty nwavTamanoProperty() {
        return nwavTamano;
    }

    public BooleanProperty nwavNegritaProperty() {
        return nwavNegrita;
    }

    public BooleanProperty nwavCursivaProperty() {
        return nwavCursiva;
    }

    public NwavColorRgb nwavColorRgb() {
        return nwavConvertir(nwavColor.get());
    }

    public double nwavGrosor() {
        return nwavGrosor.get();
    }

    public NwavEstiloTexto nwavEstiloTexto() {
        return new NwavEstiloTexto(nwavFuente.get(), nwavTamano.get(), nwavNegrita.get(), nwavCursiva.get(),
                nwavColorRgb());
    }

    /** Fuente de pantalla equivalente, para que la vista previa se parezca al resultado. */
    public Font nwavFuentePantalla(double nwavEscala) {
        String nwavFamilia = switch (nwavFuente.get()) {
            case NWAV_TIMES -> "Times New Roman";
            case NWAV_COURIER -> "Courier New";
            case NWAV_HELVETICA -> "Helvetica";
        };
        return Font.font(nwavFamilia, nwavNegrita.get() ? FontWeight.BOLD : FontWeight.NORMAL,
                nwavCursiva.get() ? FontPosture.ITALIC : FontPosture.REGULAR, nwavTamano.get() * nwavEscala);
    }

    public static NwavColorRgb nwavConvertir(Color nwavColorFx) {
        Color nwavValor = nwavColorFx == null ? Color.BLACK : nwavColorFx;
        return new NwavColorRgb((int) Math.round(nwavValor.getRed() * 255),
                (int) Math.round(nwavValor.getGreen() * 255), (int) Math.round(nwavValor.getBlue() * 255));
    }
}
