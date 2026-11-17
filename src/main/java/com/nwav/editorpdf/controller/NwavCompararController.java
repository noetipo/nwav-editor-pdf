package com.nwav.editorpdf.controller;

import com.nwav.editorpdf.exception.NwavContrasenaRequeridaException;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.NwavComparacionService;
import com.nwav.editorpdf.service.NwavComparacionService.NwavSesion;
import com.nwav.editorpdf.service.NwavHistorialService;
import com.nwav.editorpdf.ui.NwavDefinicionHerramienta;
import com.nwav.editorpdf.ui.NwavImagenFx;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import com.nwav.editorpdf.ui.NwavVistaHerramienta;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Comparar dos PDF: lado a lado con el desplazamiento sincronizado y las palabras quitadas
 * (rojo) y anadidas (verde) resaltadas, o superposicion visual por pixeles.
 */
@Slf4j(topic = "com.nwav.editorpdf.controller.NwavCompararController")
@Component
@RequiredArgsConstructor
public class NwavCompararController implements NwavVistaHerramienta {

    private static final double NWAV_ESCALA_MINIMA = 0.4;
    private static final double NWAV_ESCALA_MAXIMA = 3;

    private final NwavComparacionService nwavComparacion;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavNotificador nwavNotificador;
    private final NwavHistorialService nwavHistorial;
    private final NwavMainController nwavMain;

    @FXML private ToggleGroup nwavGrupoModo;
    @FXML private Label nwavLblOriginal;
    @FXML private Label nwavLblNuevo;
    @FXML private Button nwavBtnComparar;
    @FXML private ToggleButton nwavTglTexto;
    @FXML private ToggleButton nwavTglVisual;
    @FXML private Label nwavLblResumen;
    @FXML private Button nwavBtnAnterior;
    @FXML private Label nwavLblPagina;
    @FXML private Button nwavBtnSiguiente;
    @FXML private Label nwavLblTituloIzquierda;
    @FXML private ScrollPane nwavScrollIzquierda;
    @FXML private StackPane nwavLienzoIzquierda;
    @FXML private VBox nwavColumnaDerecha;
    @FXML private ScrollPane nwavScrollDerecha;
    @FXML private StackPane nwavLienzoDerecha;

    private Path nwavOriginal;
    private Path nwavNuevo;
    private String nwavClaveOriginal;
    private String nwavClaveNuevo;
    private NwavSesion nwavSesion;
    private int nwavPagina;
    private double nwavEscala = 1;
    private boolean nwavSincronizando;

    @FXML
    public void initialize() {
        // Desplazamiento sincronizado en ambas direcciones.
        nwavScrollIzquierda.vvalueProperty().addListener((nwavObs, nwavAntes, nwavAhora) ->
                nwavSincronizar(() -> nwavScrollDerecha.setVvalue(nwavAhora.doubleValue())));
        nwavScrollDerecha.vvalueProperty().addListener((nwavObs, nwavAntes, nwavAhora) ->
                nwavSincronizar(() -> nwavScrollIzquierda.setVvalue(nwavAhora.doubleValue())));
        nwavScrollIzquierda.hvalueProperty().addListener((nwavObs, nwavAntes, nwavAhora) ->
                nwavSincronizar(() -> nwavScrollDerecha.setHvalue(nwavAhora.doubleValue())));
        nwavScrollDerecha.hvalueProperty().addListener((nwavObs, nwavAntes, nwavAhora) ->
                nwavSincronizar(() -> nwavScrollIzquierda.setHvalue(nwavAhora.doubleValue())));
        nwavGrupoModo.selectedToggleProperty().addListener((nwavObs, nwavAntes, nwavAhora) -> {
            if (nwavAhora == null) {
                nwavAntes.setSelected(true); // siempre hay un modo elegido
                return;
            }
            nwavMostrarPagina();
        });
        nwavActualizarControles();
    }

    @Override
    public void nwavPreparar(NwavDefinicionHerramienta nwavDefinicion, List<Path> nwavArchivos) {
        if (nwavArchivos != null && nwavArchivos.size() >= 2) {
            nwavPonerOriginal(nwavArchivos.get(0));
            nwavPonerNuevo(nwavArchivos.get(1));
        } else if (nwavArchivos != null && nwavArchivos.size() == 1) {
            nwavPonerOriginal(nwavArchivos.get(0));
        }
        nwavActualizarControles();
    }

    private void nwavSincronizar(Runnable nwavAccion) {
        if (nwavSincronizando) {
            return;
        }
        nwavSincronizando = true;
        try {
            nwavAccion.run();
        } finally {
            nwavSincronizando = false;
        }
    }

    // ================================================================ archivos

    @FXML
    private void nwavOnElegirOriginal() {
        nwavElegir("PDF original").ifPresent(this::nwavPonerOriginal);
    }

    @FXML
    private void nwavOnElegirNuevo() {
        nwavElegir("PDF nuevo (modificado)").ifPresent(this::nwavPonerNuevo);
    }

    private Optional<Path> nwavElegir(String nwavTitulo) {
        FileChooser nwavSelector = new FileChooser();
        nwavSelector.setTitle(nwavTitulo);
        nwavSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documentos PDF", "*.pdf", "*.PDF"));
        File nwavElegido = nwavSelector.showOpenDialog(nwavScrollIzquierda.getScene().getWindow());
        return Optional.ofNullable(nwavElegido).map(File::toPath);
    }

    private void nwavPonerOriginal(Path nwavRuta) {
        nwavOriginal = nwavRuta;
        nwavClaveOriginal = null;
        nwavLblOriginal.setText(String.valueOf(nwavRuta.getFileName()));
        nwavActualizarControles();
    }

    private void nwavPonerNuevo(Path nwavRuta) {
        nwavNuevo = nwavRuta;
        nwavClaveNuevo = null;
        nwavLblNuevo.setText(String.valueOf(nwavRuta.getFileName()));
        nwavActualizarControles();
    }

    // ============================================================== comparar

    @FXML
    private void nwavOnComparar() {
        if (nwavOriginal == null || nwavNuevo == null) {
            nwavNotificador.nwavAdvertencia("Elige el PDF original y el nuevo.");
            return;
        }
        Path nwavA = nwavOriginal;
        Path nwavB = nwavNuevo;
        String nwavClaveA = nwavClaveOriginal;
        String nwavClaveB = nwavClaveNuevo;
        nwavBtnComparar.setDisable(true);
        nwavTareaFondo.nwavEjecutarCancelable("Comparando…", (nwavAvance, nwavCancelacion) ->
                        nwavComparacion.nwavComparar(nwavA, nwavClaveA, nwavB, nwavClaveB, nwavAvance, nwavCancelacion),
                nwavNueva -> {
                    nwavCerrarSesion();
                    nwavSesion = nwavNueva;
                    nwavPagina = nwavPrimeraConCambios();
                    nwavBtnComparar.setDisable(false);
                    String nwavResumen = nwavNueva.nwavIguales() ? "El texto es idéntico"
                            : nwavNueva.nwavQuitadas() + " palabras quitadas · " + nwavNueva.nwavAnadidas() + " añadidas";
                    if (nwavNueva.nwavPaginasA() != nwavNueva.nwavPaginasB()) {
                        nwavResumen += " · páginas: " + nwavNueva.nwavPaginasA() + " → " + nwavNueva.nwavPaginasB();
                    }
                    nwavLblResumen.setText(nwavResumen);
                    nwavHistorial.nwavRegistrarExito(NwavTipoOperacion.NWAV_COMPARAR, List.of(nwavA, nwavB), null,
                            nwavResumen);
                    nwavMostrarPagina();
                },
                nwavError -> {
                    nwavBtnComparar.setDisable(false);
                    if (nwavError instanceof NwavContrasenaRequeridaException) {
                        boolean nwavEsOriginal = nwavError.getMessage() != null
                                && nwavError.getMessage().contains(String.valueOf(nwavA.getFileName()));
                        nwavPedirClave(nwavEsOriginal ? nwavA : nwavB).ifPresent(nwavClave -> {
                            if (nwavEsOriginal) {
                                nwavClaveOriginal = nwavClave;
                            } else {
                                nwavClaveNuevo = nwavClave;
                            }
                            nwavOnComparar();
                        });
                    }
                });
    }

    private Optional<String> nwavPedirClave(Path nwavRuta) {
        TextInputDialog nwavDialogo = new TextInputDialog();
        nwavDialogo.setTitle("PDF protegido");
        nwavDialogo.setHeaderText("«" + nwavRuta.getFileName() + "» tiene contraseña");
        nwavDialogo.setContentText("Contraseña:");
        nwavDialogo.initOwner(nwavScrollIzquierda.getScene().getWindow());
        return nwavDialogo.showAndWait().filter(nwavClave -> !nwavClave.isEmpty());
    }

    private int nwavPrimeraConCambios() {
        for (int nwavIndice = 0; nwavIndice < nwavSesion.nwavPaginas(); nwavIndice++) {
            if (!nwavSesion.nwavMarcasA(nwavIndice).nwavQuitadas().isEmpty()
                    || !nwavSesion.nwavMarcasB(nwavIndice).nwavAnadidas().isEmpty()) {
                return nwavIndice;
            }
        }
        return 0;
    }

    // ============================================================ navegacion

    @FXML
    private void nwavOnAnterior() {
        if (nwavSesion != null && nwavPagina > 0) {
            nwavPagina--;
            nwavMostrarPagina();
        }
    }

    @FXML
    private void nwavOnSiguiente() {
        if (nwavSesion != null && nwavPagina < nwavSesion.nwavPaginas() - 1) {
            nwavPagina++;
            nwavMostrarPagina();
        }
    }

    @FXML
    private void nwavOnAcercar() {
        nwavEscala = Math.min(NWAV_ESCALA_MAXIMA, nwavEscala * 1.25);
        nwavMostrarPagina();
    }

    @FXML
    private void nwavOnAlejar() {
        nwavEscala = Math.max(NWAV_ESCALA_MINIMA, nwavEscala / 1.25);
        nwavMostrarPagina();
    }

    @FXML
    private void nwavOnVolver() {
        nwavCerrarSesion();
        nwavLblResumen.setText("");
        nwavLienzoIzquierda.getChildren().clear();
        nwavLienzoDerecha.getChildren().clear();
        nwavActualizarControles();
        nwavMain.nwavIrA(NwavMainController.NWAV_VISTA_INICIO);
    }

    private void nwavMostrarPagina() {
        nwavActualizarControles();
        if (nwavSesion == null) {
            return;
        }
        NwavSesion nwavActual = nwavSesion;
        int nwavIndice = nwavPagina;
        float nwavFactor = (float) (nwavEscala * nwavPantalla());
        boolean nwavVisual = nwavTglVisual.isSelected();
        nwavColumnaDerecha.setVisible(!nwavVisual);
        nwavColumnaDerecha.setManaged(!nwavVisual);
        nwavLblTituloIzquierda.setText(nwavVisual
                ? "Superposición: gris = igual · rojo = solo en el original · verde = solo en el nuevo" : "Original");
        nwavTareaFondo.nwavEjecutarSilencioso(() -> {
            if (nwavVisual) {
                NwavComparacionService.NwavSuperposicion nwavResultado = nwavActual.nwavSuperponer(nwavIndice, nwavFactor);
                return new BufferedImage[]{nwavResultado.nwavImagen(), null};
            }
            return new BufferedImage[]{nwavActual.nwavRenderizar(true, nwavIndice, nwavFactor),
                    nwavActual.nwavRenderizar(false, nwavIndice, nwavFactor)};
        }, nwavImagenes -> {
            if (nwavActual != nwavSesion || nwavIndice != nwavPagina) {
                return; // el usuario ya cambio de pagina
            }
            nwavPintar(nwavLienzoIzquierda, nwavImagenes[0],
                    nwavVisual ? List.of() : nwavActual.nwavMarcasA(nwavIndice).nwavQuitadas(), Color.web("#e11d48"));
            if (!nwavVisual) {
                nwavPintar(nwavLienzoDerecha, nwavImagenes[1], nwavActual.nwavMarcasB(nwavIndice).nwavAnadidas(),
                        Color.web("#16a34a"));
            }
        });
    }

    private void nwavPintar(StackPane nwavLienzo, BufferedImage nwavImagen, List<NwavRectangulo> nwavMarcas, Color nwavColor) {
        nwavLienzo.getChildren().clear();
        if (nwavImagen == null) {
            Label nwavFalta = new Label("Este documento no tiene la página " + (nwavPagina + 1));
            nwavFalta.getStyleClass().add("nwav-texto-tenue");
            nwavLienzo.getChildren().add(nwavFalta);
            return;
        }
        double nwavPantalla = nwavPantalla();
        ImageView nwavVista = new ImageView(NwavImagenFx.nwavConvertir(nwavImagen));
        nwavVista.setFitWidth(nwavImagen.getWidth() / nwavPantalla);
        nwavVista.setPreserveRatio(true);
        nwavVista.setSmooth(true);
        Pane nwavCapa = new Pane();
        nwavCapa.setMouseTransparent(true);
        nwavCapa.setPrefSize(nwavImagen.getWidth() / nwavPantalla, nwavImagen.getHeight() / nwavPantalla);
        for (NwavRectangulo nwavMarca : nwavMarcas) {
            Rectangle nwavResalte = new Rectangle(nwavMarca.nwavX() * nwavEscala, nwavMarca.nwavY() * nwavEscala,
                    nwavMarca.nwavAncho() * nwavEscala, nwavMarca.nwavAlto() * nwavEscala);
            nwavResalte.setFill(nwavColor.deriveColor(0, 1, 1, 0.28));
            nwavResalte.setStroke(nwavColor);
            nwavResalte.setStrokeWidth(0.8);
            nwavCapa.getChildren().add(nwavResalte);
        }
        StackPane nwavHoja = new StackPane(nwavVista, nwavCapa);
        nwavHoja.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 10, 0, 0, 2);");
        nwavHoja.setMaxSize(nwavImagen.getWidth() / nwavPantalla, nwavImagen.getHeight() / nwavPantalla);
        nwavLienzo.getChildren().add(nwavHoja);
    }

    private double nwavPantalla() {
        return nwavScrollIzquierda.getScene() != null && nwavScrollIzquierda.getScene().getWindow() != null
                ? nwavScrollIzquierda.getScene().getWindow().getOutputScaleX() : 1;
    }

    private void nwavActualizarControles() {
        boolean nwavHay = nwavSesion != null;
        nwavBtnComparar.setDisable(nwavOriginal == null || nwavNuevo == null);
        nwavBtnAnterior.setDisable(!nwavHay || nwavPagina == 0);
        nwavBtnSiguiente.setDisable(!nwavHay || nwavPagina >= nwavSesion.nwavPaginas() - 1);
        nwavLblPagina.setText(nwavHay ? String.format(Locale.ROOT, "Página %d de %d", nwavPagina + 1,
                nwavSesion.nwavPaginas()) : "—");
    }

    private void nwavCerrarSesion() {
        if (nwavSesion != null) {
            try {
                nwavSesion.close();
            } catch (IOException nwavError) {
                nwavLog.warn("No se pudo cerrar la comparación", nwavError);
            }
            nwavSesion = null;
        }
    }
}
