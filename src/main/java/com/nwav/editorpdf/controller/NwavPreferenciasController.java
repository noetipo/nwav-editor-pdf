package com.nwav.editorpdf.controller;

import com.nwav.editorpdf.model.NwavModoZoom;
import com.nwav.editorpdf.model.NwavZoomPreferido;
import com.nwav.editorpdf.service.NwavArchivoRecienteService;
import com.nwav.editorpdf.service.NwavConfiguracionService;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavRefrescable;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Preferencias del usuario, guardadas en {@code nwav_configuracion}. */
@Component
@RequiredArgsConstructor
public class NwavPreferenciasController implements NwavRefrescable {

    private static final String NWAV_OPCION_ANCHO = NwavModoZoom.NWAV_AJUSTAR_ANCHO.getNwavDescripcion();
    private static final String NWAV_OPCION_PAGINA = NwavModoZoom.NWAV_AJUSTAR_PAGINA.getNwavDescripcion();
    private static final List<Integer> NWAV_PORCENTAJES = List.of(50, 75, 100, 125, 150, 200);
    private static final String NWAV_TEMA_CLARO = "Claro";
    private static final String NWAV_TEMA_OSCURO = "Oscuro";

    private final NwavConfiguracionService nwavConfiguracion;
    private final NwavArchivoRecienteService nwavRecientes;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavNotificador nwavNotificador;
    private final NwavMainController nwavMain;
    private final Environment nwavEntorno;

    @FXML private ComboBox<String> nwavCmbTema;
    @FXML private ComboBox<String> nwavCmbZoomDefecto;
    @FXML private CheckBox nwavChkRestaurar;
    @FXML private TextField nwavTxtCarpeta;
    @FXML private Spinner<Integer> nwavSpnMaximoRecientes;
    @FXML private Label nwavLblRutaBase;

    @FXML
    public void initialize() {
        nwavCmbTema.getItems().setAll(NWAV_TEMA_CLARO, NWAV_TEMA_OSCURO);
        nwavCmbZoomDefecto.getItems().setAll(NWAV_OPCION_ANCHO, NWAV_OPCION_PAGINA);
        NWAV_PORCENTAJES.forEach(nwavPorcentaje -> nwavCmbZoomDefecto.getItems().add(nwavPorcentaje + " %"));
        nwavSpnMaximoRecientes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                NwavConfiguracionService.NWAV_MAXIMO_RECIENTES_MIN,
                NwavConfiguracionService.NWAV_MAXIMO_RECIENTES_MAX,
                NwavConfiguracionService.NWAV_MAXIMO_RECIENTES_DEFECTO));
        nwavLblRutaBase.setText(Path.of(nwavEntorno.getProperty("nwav.datos.carpeta", ""),
                nwavEntorno.getProperty("nwav.datos.archivo", "")).toString());
    }

    /** Las preferencias ya estan en memoria en el servicio: se leen sin tocar la base. */
    @Override
    public void nwavRefrescar() {
        nwavCmbTema.setValue(nwavMain.nwavEsOscuro() ? NWAV_TEMA_OSCURO : NWAV_TEMA_CLARO);
        nwavCmbZoomDefecto.setValue(nwavTextoZoom(nwavConfiguracion.nwavZoomPorDefecto()));
        nwavChkRestaurar.setSelected(nwavConfiguracion.nwavRestaurarPosicion());
        nwavTxtCarpeta.setText(nwavConfiguracion.nwavCarpetaPorDefecto().map(Path::toString).orElse(""));
        nwavSpnMaximoRecientes.getValueFactory().setValue(nwavConfiguracion.nwavMaximoRecientes());
    }

    @FXML
    private void nwavOnElegirCarpeta() {
        DirectoryChooser nwavSelector = new DirectoryChooser();
        nwavSelector.setTitle("Carpeta por defecto para abrir y guardar");
        nwavConfiguracion.nwavCarpetaInicial().map(Path::toFile).ifPresent(nwavSelector::setInitialDirectory);
        File nwavElegida = nwavSelector.showDialog(nwavTxtCarpeta.getScene().getWindow());
        if (nwavElegida != null) {
            nwavTxtCarpeta.setText(nwavElegida.getAbsolutePath());
        }
    }

    @FXML
    private void nwavOnQuitarCarpeta() {
        nwavTxtCarpeta.clear();
    }

    @FXML
    private void nwavOnGuardar() {
        boolean nwavOscuro = NWAV_TEMA_OSCURO.equals(nwavCmbTema.getValue());
        NwavZoomPreferido nwavZoom = nwavLeerZoom(nwavCmbZoomDefecto.getValue());
        boolean nwavRestaurar = nwavChkRestaurar.isSelected();
        String nwavCarpeta = nwavTxtCarpeta.getText();
        int nwavMaximo = nwavSpnMaximoRecientes.getValue();

        nwavTareaFondo.nwavEjecutar("Guardando preferencias…", () -> {
            nwavConfiguracion.nwavGuardarTemaOscuro(nwavOscuro);
            nwavConfiguracion.nwavGuardarZoomPorDefecto(nwavZoom);
            nwavConfiguracion.nwavGuardarRestaurarPosicion(nwavRestaurar);
            nwavConfiguracion.nwavGuardarCarpetaPorDefecto(
                    nwavCarpeta == null || nwavCarpeta.isBlank() ? null : Path.of(nwavCarpeta));
            nwavConfiguracion.nwavGuardarMaximoRecientes(nwavMaximo);
            nwavRecientes.nwavRecortar();
            return null;
        }, nwavNada -> {
            if (nwavOscuro != nwavMain.nwavEsOscuro()) {
                nwavMain.nwavAplicarTema(nwavOscuro, false);
            }
            nwavMain.nwavActualizarMenuRecientes();
            nwavNotificador.nwavExito("Preferencias guardadas.");
        }, null);
    }

    static String nwavTextoZoom(NwavZoomPreferido nwavZoom) {
        return switch (nwavZoom.nwavModo()) {
            case NWAV_AJUSTAR_ANCHO -> NWAV_OPCION_ANCHO;
            case NWAV_AJUSTAR_PAGINA -> NWAV_OPCION_PAGINA;
            case NWAV_PORCENTAJE -> Math.round(nwavZoom.nwavFactor() * 100) + " %";
        };
    }

    static NwavZoomPreferido nwavLeerZoom(String nwavTexto) {
        if (nwavTexto == null || NWAV_OPCION_ANCHO.equals(nwavTexto)) {
            return new NwavZoomPreferido(NwavModoZoom.NWAV_AJUSTAR_ANCHO, 1.0);
        }
        if (NWAV_OPCION_PAGINA.equals(nwavTexto)) {
            return new NwavZoomPreferido(NwavModoZoom.NWAV_AJUSTAR_PAGINA, 1.0);
        }
        try {
            double nwavPorcentaje = Double.parseDouble(nwavTexto.replaceAll("[^0-9]", "").toLowerCase(Locale.ROOT));
            return new NwavZoomPreferido(NwavModoZoom.NWAV_PORCENTAJE, nwavPorcentaje / 100.0);
        } catch (NumberFormatException nwavError) {
            return NwavZoomPreferido.NWAV_POR_DEFECTO;
        }
    }
}
