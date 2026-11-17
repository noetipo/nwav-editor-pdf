package com.nwav.editorpdf.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NwavFormularioOpciones {
    private final VBox nwavRaiz = new VBox(12);
    private final Map<String, StringProperty> nwavValores = new LinkedHashMap<>();
    private final List<NwavCampoOpcion> nwavCampos;

    public NwavFormularioOpciones(List<NwavCampoOpcion> nwavCampos, Map<String, String> nwavGuardados) {
        this.nwavCampos = nwavCampos;
        nwavRaiz.getStyleClass().add("nwav-formulario-opciones");
        for (NwavCampoOpcion nwavCampo : nwavCampos) {
            String nwavInicial = nwavCampo.nwavSeGuarda() && nwavGuardados.containsKey(nwavCampo.getNwavClave())
                    ? nwavGuardados.get(nwavCampo.getNwavClave()) : nwavCampo.getNwavDefecto();
            StringProperty nwavValor = new SimpleStringProperty(nwavInicial);
            nwavValores.put(nwavCampo.getNwavClave(), nwavValor);
        }
        for (NwavCampoOpcion nwavCampo : nwavCampos) {
            nwavRaiz.getChildren().add(nwavFila(nwavCampo));
        }
    }

    public Node nwavNodo() {
        return nwavRaiz;
    }

    public Map<String, String> nwavValores() {
        Map<String, String> nwavSalida = new LinkedHashMap<>();
        nwavValores.forEach((nwavClave, nwavValor) -> nwavSalida.put(nwavClave, nwavValor.get()));
        return nwavSalida;
    }

    public Map<String, String> nwavValoresGuardables() {
        Map<String, String> nwavSalida = new LinkedHashMap<>();
        for (NwavCampoOpcion nwavCampo : nwavCampos) {
            if (nwavCampo.nwavSeGuarda()) {
                nwavSalida.put(nwavCampo.getNwavClave(), nwavValores.get(nwavCampo.getNwavClave()).get());
            }
        }
        return nwavSalida;
    }

    public StringProperty nwavPropiedad(String nwavClave) {
        return nwavValores.get(nwavClave);
    }

    private Node nwavFila(NwavCampoOpcion nwavCampo) {
        StringProperty nwavValor = nwavValores.get(nwavCampo.getNwavClave());
        Node nwavControl = nwavControl(nwavCampo, nwavValor);
        VBox nwavFila = new VBox(5);
        if (nwavCampo.getNwavTipo() != NwavCampoOpcion.NwavTipo.NWAV_CASILLA) {
            Label nwavEtiqueta = new Label(nwavCampo.getNwavEtiqueta());
            nwavEtiqueta.getStyleClass().add("nwav-campo-label");
            nwavFila.getChildren().add(nwavEtiqueta);
        }
        nwavFila.getChildren().add(nwavControl);
        if (nwavCampo.getNwavAyuda() != null) {
            Label nwavAyuda = new Label(nwavCampo.getNwavAyuda());
            nwavAyuda.getStyleClass().add("nwav-panel-ayuda");
            nwavAyuda.setWrapText(true);
            nwavFila.getChildren().add(nwavAyuda);
        }
        if (nwavCampo.getNwavSiClave() != null) {
            StringProperty nwavOtra = nwavValores.get(nwavCampo.getNwavSiClave());
            Runnable nwavActualizar = () -> {
                boolean nwavVisible = nwavOtra != null && nwavCampo.getNwavSiValores().contains(nwavOtra.get());
                nwavFila.setVisible(nwavVisible);
                nwavFila.setManaged(nwavVisible);
            };
            if (nwavOtra != null) {
                nwavOtra.addListener((nwavObs, nwavAntes, nwavAhora) -> nwavActualizar.run());
            }
            nwavActualizar.run();
        }
        return nwavFila;
    }

    private Node nwavControl(NwavCampoOpcion nwavCampo, StringProperty nwavValor) {
        switch (nwavCampo.getNwavTipo()) {
            case NWAV_ELECCION -> {
                ComboBox<String> nwavCombo = new ComboBox<>();
                List<String> nwavOpciones = nwavCampo.nwavValores();
                nwavOpciones.forEach(nwavOpcion -> nwavCombo.getItems().add(nwavParte(nwavOpcion, 0)));
                Map<String, String> nwavEtiquetas = nwavOpciones.stream().collect(Collectors.toMap(
                        nwavOpcion -> nwavParte(nwavOpcion, 0), nwavOpcion -> nwavParte(nwavOpcion, 1), (nwavA, nwavB) -> nwavA,
                        LinkedHashMap::new));
                nwavCombo.setCellFactory(nwavLista -> new NwavCeldaEtiqueta(nwavEtiquetas));
                nwavCombo.setButtonCell(new NwavCeldaEtiqueta(nwavEtiquetas));
                nwavCombo.setValue(nwavCombo.getItems().contains(nwavValor.get()) ? nwavValor.get()
                        : nwavCombo.getItems().isEmpty() ? null : nwavCombo.getItems().get(0));
                nwavValor.set(nwavCombo.getValue());
                nwavCombo.valueProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavValor.set(nwavAhora));
                nwavCombo.setMaxWidth(Double.MAX_VALUE);
                return nwavCombo;
            }
            case NWAV_ENTERO -> {
                int nwavInicial = (int) Math.round(nwavNumero(nwavValor.get(), Double.parseDouble(nwavCampo.getNwavDefecto())));
                Spinner<Integer> nwavSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        (int) nwavCampo.getNwavMinimo(), (int) nwavCampo.getNwavMaximo(),
                        Math.max((int) nwavCampo.getNwavMinimo(), Math.min((int) nwavCampo.getNwavMaximo(), nwavInicial))));
                nwavSpinner.setEditable(true);
                nwavSpinner.setPrefWidth(130);
                nwavValor.set(String.valueOf(nwavSpinner.getValue()));
                nwavSpinner.valueProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavValor.set(String.valueOf(nwavAhora)));
                return nwavSpinner;
            }
            case NWAV_DECIMAL -> {
                double nwavInicial = nwavNumero(nwavValor.get(), Double.parseDouble(nwavCampo.getNwavDefecto()));
                Spinner<Double> nwavSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(
                        nwavCampo.getNwavMinimo(), nwavCampo.getNwavMaximo(), nwavInicial, nwavCampo.getNwavPaso()));
                nwavSpinner.setEditable(true);
                nwavSpinner.setPrefWidth(130);
                nwavValor.set(String.valueOf(nwavSpinner.getValue()));
                nwavSpinner.valueProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavValor.set(String.valueOf(nwavAhora)));
                return nwavSpinner;
            }
            case NWAV_CASILLA -> {
                CheckBox nwavCasilla = new CheckBox(nwavCampo.getNwavEtiqueta());
                nwavCasilla.setWrapText(true);
                nwavCasilla.setSelected(Boolean.parseBoolean(nwavValor.get()));
                nwavCasilla.selectedProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavValor.set(String.valueOf(nwavAhora)));
                return nwavCasilla;
            }
            case NWAV_CONTRASENA -> {
                PasswordField nwavCampoClave = new PasswordField();
                nwavCampoClave.textProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavValor.set(nwavAhora));
                return nwavCampoClave;
            }
            case NWAV_ARCHIVO -> {
                javafx.scene.control.TextField nwavRuta = new javafx.scene.control.TextField(nwavValor.get());
                nwavRuta.setPromptText("Ningún archivo elegido");
                nwavRuta.textProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavValor.set(nwavAhora));
                javafx.scene.control.Button nwavElegir = new javafx.scene.control.Button("Elegir…");
                nwavElegir.getStyleClass().add("button-outlined");
                nwavElegir.setOnAction(nwavEvento -> {
                    javafx.stage.FileChooser nwavSelector = new javafx.stage.FileChooser();
                    nwavSelector.setTitle(nwavCampo.getNwavEtiqueta());
                    List<String> nwavExtensiones = nwavCampo.nwavValores();
                    if (!nwavExtensiones.isEmpty()) {
                        nwavSelector.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(
                                String.join(", ", nwavExtensiones).toUpperCase(java.util.Locale.ROOT),
                                nwavExtensiones.stream().flatMap(nwavE -> java.util.stream.Stream.of("*." + nwavE,
                                        "*." + nwavE.toUpperCase(java.util.Locale.ROOT))).toList()));
                    }
                    java.io.File nwavElegido = nwavSelector.showOpenDialog(nwavElegir.getScene().getWindow());
                    if (nwavElegido != null) {
                        nwavRuta.setText(nwavElegido.getAbsolutePath());
                    }
                });
                javafx.scene.layout.HBox nwavFila = new javafx.scene.layout.HBox(6, nwavRuta, nwavElegir);
                javafx.scene.layout.HBox.setHgrow(nwavRuta, javafx.scene.layout.Priority.ALWAYS);
                return nwavFila;
            }
            case NWAV_COLOR -> {
                ColorPicker nwavSelector = new ColorPicker(nwavColor(nwavValor.get()));
                nwavSelector.valueProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavValor.set(nwavHex(nwavAhora)));
                return nwavSelector;
            }
            case NWAV_MULTIPLE -> {
                FlowPane nwavCasillas = new FlowPane(12, 6);
                Set<String> nwavMarcados = new HashSet<>(Arrays.asList(nwavValor.get() == null ? new String[0]
                        : nwavValor.get().split(",")));
                for (String nwavOpcion : nwavCampo.nwavValores()) {
                    String nwavClave = nwavParte(nwavOpcion, 0);
                    CheckBox nwavCasilla = new CheckBox(nwavParte(nwavOpcion, 1));
                    nwavCasilla.setUserData(nwavClave);
                    nwavCasilla.setSelected(nwavMarcados.contains(nwavClave));
                    nwavCasilla.selectedProperty().addListener((nwavObs, nwavAntes, nwavAhora) ->
                            nwavValor.set(nwavCasillas.getChildren().stream().map(nwavNodo -> (CheckBox) nwavNodo)
                                    .filter(CheckBox::isSelected).map(nwavMarcada -> (String) nwavMarcada.getUserData())
                                    .collect(Collectors.joining(","))));
                    nwavCasillas.getChildren().add(nwavCasilla);
                }
                return nwavCasillas;
            }
            default -> {
                TextField nwavTexto = new TextField(nwavValor.get());
                nwavTexto.textProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> nwavValor.set(nwavAhora));
                return nwavTexto;
            }
        }
    }

    static String nwavParte(String nwavOpcion, int nwavParte) {
        int nwavIgual = nwavOpcion.indexOf('=');
        if (nwavIgual < 0) {
            return nwavOpcion;
        }
        return nwavParte == 0 ? nwavOpcion.substring(0, nwavIgual) : nwavOpcion.substring(nwavIgual + 1);
    }

    private static double nwavNumero(String nwavTexto, double nwavPorDefecto) {
        try {
            return Double.parseDouble(nwavTexto);
        } catch (RuntimeException nwavError) {
            return nwavPorDefecto;
        }
    }

    private static Color nwavColor(String nwavHex) {
        try {
            return Color.web(nwavHex);
        } catch (RuntimeException nwavError) {
            return Color.GRAY;
        }
    }

    static String nwavHex(Color nwavColor) {
        return String.format(Locale.ROOT, "#%02X%02X%02X", (int) Math.round(nwavColor.getRed() * 255),
                (int) Math.round(nwavColor.getGreen() * 255), (int) Math.round(nwavColor.getBlue() * 255));
    }

    private static final class NwavCeldaEtiqueta extends ListCell<String> {
        private final Map<String, String> nwavEtiquetas;

        private NwavCeldaEtiqueta(Map<String, String> nwavEtiquetas) {
            this.nwavEtiquetas = nwavEtiquetas;
        }

        @Override
        protected void updateItem(String nwavValor, boolean nwavVacia) {
            super.updateItem(nwavValor, nwavVacia);
            setText(nwavVacia || nwavValor == null ? null : nwavEtiquetas.getOrDefault(nwavValor, nwavValor));
        }
    }
}
