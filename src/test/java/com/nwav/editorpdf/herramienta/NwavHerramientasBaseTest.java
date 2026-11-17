package com.nwav.editorpdf.herramienta;

import com.nwav.editorpdf.controller.NwavCatalogoHerramientas;
import com.nwav.editorpdf.exception.NwavCanceladoException;
import com.nwav.editorpdf.model.NwavHistorialOperacion;
import com.nwav.editorpdf.model.NwavResultadoOperacion;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.repository.NwavHistorialOperacionRepository;
import com.nwav.editorpdf.service.NwavAvance;
import com.nwav.editorpdf.service.NwavCancelacion;
import com.nwav.editorpdf.service.NwavEscrituraSegura;
import com.nwav.editorpdf.service.herramienta.NwavEjecutorHerramientasService;
import com.nwav.editorpdf.service.herramienta.NwavEjecutorHerramientasService.NwavTrabajo;
import com.nwav.editorpdf.service.herramienta.NwavOpcionesHerramientaService;
import com.nwav.editorpdf.service.herramienta.NwavResultadoArchivo;
import com.nwav.editorpdf.service.herramienta.NwavResultadoProceso;
import com.nwav.editorpdf.soporte.NwavPdfDePrueba;
import com.nwav.editorpdf.soporte.NwavPruebaConBaseDatos;
import com.nwav.editorpdf.ui.NwavCampoOpcion;
import com.nwav.editorpdf.ui.NwavDefinicionHerramienta;
import com.nwav.editorpdf.ui.NwavModoHerramienta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Base comun de las herramientas: lotes, historial, cancelacion, opciones y catalogo. */
class NwavHerramientasBaseTest extends NwavPruebaConBaseDatos {

    @Autowired private NwavEjecutorHerramientasService nwavEjecutor;
    @Autowired private NwavOpcionesHerramientaService nwavOpciones;
    @Autowired private NwavHistorialOperacionRepository nwavHistorial;
    @Autowired private NwavCatalogoHerramientas nwavCatalogo;

    @TempDir
    Path nwavCarpeta;

    @BeforeEach
    void nwavLimpiar() {
        nwavHistorial.deleteAll();
    }

    @Test
    void nwavUnLoteSigueAunqueUnArchivoFalle() throws Exception {
        Path nwavA = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "a.pdf", 1);
        Path nwavB = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "b.pdf", 1);
        Path nwavC = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "c.pdf", 1);
        NwavTrabajo nwavTrabajo = new NwavTrabajo(NwavTipoOperacion.NWAV_COMPRIMIR, true, nwavEntrada -> {
            if (nwavEntrada.nwavArchivo().equals(nwavB)) {
                throw new IllegalStateException("fallo simulado");
            }
            Path nwavSalida = NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(), null, "x", "pdf");
            Files.copy(nwavEntrada.nwavArchivo(), nwavSalida);
            return NwavResultadoProceso.nwavDe(nwavSalida, "ok");
        });

        List<NwavResultadoArchivo> nwavResultados = nwavEjecutor.nwavEjecutar(nwavTrabajo, List.of(nwavA, nwavB, nwavC),
                Map.of(), null, Map.of(), NwavAvance.NWAV_NINGUNO, NwavCancelacion.NWAV_NUNCA);

        assertThat(nwavResultados).extracting(NwavResultadoArchivo::nwavExito).containsExactly(true, false, true);
        assertThat(nwavResultados.get(1).nwavResumen()).contains("fallo simulado");
        assertThat(nwavCarpeta.resolve("a_x.pdf")).exists();
        assertThat(nwavHistorial.findAll()).extracting(NwavHistorialOperacion::getNwavResultado)
                .containsExactlyInAnyOrder(NwavResultadoOperacion.NWAV_EXITO, NwavResultadoOperacion.NWAV_ERROR,
                        NwavResultadoOperacion.NWAV_EXITO);
    }

    @Test
    void nwavCancelarDetieneElLote() throws Exception {
        Path nwavA = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "a.pdf", 1);
        NwavTrabajo nwavTrabajo = new NwavTrabajo(NwavTipoOperacion.NWAV_REPARAR, true,
                nwavEntrada -> NwavResultadoProceso.nwavDe(nwavEntrada.nwavArchivo(), "ok"));

        assertThatThrownBy(() -> nwavEjecutor.nwavEjecutar(nwavTrabajo, List.of(nwavA, nwavA), Map.of(), null, Map.of(),
                NwavAvance.NWAV_NINGUNO, () -> true)).isInstanceOf(NwavCanceladoException.class);
    }

    @Test
    void nwavLasOpcionesSeRecuerdanPorHerramienta() {
        nwavOpciones.nwavGuardar("comprimir", Map.of("nivel", "NWAV_EXTREMA", "grises", "true"));
        nwavOpciones.nwavGuardar("comprimir", Map.of("nivel", "NWAV_BAJA"));
        nwavOpciones.nwavGuardar("ocr", Map.of("idiomas", "spa"));

        assertThat(nwavOpciones.nwavCargar("comprimir")).containsEntry("nivel", "NWAV_BAJA").containsEntry("grises", "true");
        assertThat(nwavOpciones.nwavCargar("ocr")).containsOnly(Map.entry("idiomas", "spa"));
        assertThat(nwavOpciones.nwavCargar("nada")).isEmpty();
    }

    @Test
    void nwavLasRutasDeSalidaNuncaPisanElOriginalNiOtroArchivo() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "doc.pdf", 1);
        Path nwavPrimera = NwavEscrituraSegura.nwavRutaSalida(nwavOriginal, null, "comprimido", "pdf");
        Files.createFile(nwavPrimera);
        Path nwavSegunda = NwavEscrituraSegura.nwavRutaSalida(nwavOriginal, null, "comprimido", "pdf");

        assertThat(nwavPrimera.getFileName().toString()).isEqualTo("doc_comprimido.pdf");
        assertThat(nwavSegunda.getFileName().toString()).isEqualTo("doc_comprimido (2).pdf");
        assertThat(NwavEscrituraSegura.nwavRutaSalida(nwavOriginal, null, "", "pdf")).isNotEqualTo(nwavOriginal);
    }

    @Test
    void nwavElCatalogoEsCoherente() {
        List<NwavDefinicionHerramienta> nwavTodas = nwavCatalogo.nwavTodas();
        Set<String> nwavIds = new HashSet<>();
        for (NwavDefinicionHerramienta nwavDefinicion : nwavTodas) {
            assertThat(nwavIds.add(nwavDefinicion.getNwavId())).as("id único: " + nwavDefinicion.getNwavId()).isTrue();
            if (nwavDefinicion.getNwavModo() == NwavModoHerramienta.NWAV_LOTE) {
                assertThat(nwavDefinicion.getNwavTrabajo()).as(nwavDefinicion.getNwavId()).isNotNull();
            }
            if (nwavDefinicion.getNwavModo() == NwavModoHerramienta.NWAV_EDITOR) {
                assertThat(nwavDefinicion.getNwavAccionEditor()).as(nwavDefinicion.getNwavId()).isNotNull();
            }
            for (NwavCampoOpcion nwavCampo : nwavDefinicion.getNwavOpciones()) {
                if (nwavCampo.getNwavTipo() == NwavCampoOpcion.NwavTipo.NWAV_ELECCION) {
                    assertThat(nwavCampo.nwavValores()).as(nwavDefinicion.getNwavId() + "." + nwavCampo.getNwavClave())
                            .anyMatch(nwavValor -> nwavValor.startsWith(nwavCampo.getNwavDefecto() + "="));
                }
                if (nwavCampo.getNwavSiClave() != null) {
                    assertThat(nwavDefinicion.getNwavOpciones()).extracting(NwavCampoOpcion::getNwavClave)
                            .contains(nwavCampo.getNwavSiClave());
                }
            }
            if (nwavDefinicion.getNwavSiguiente() != null) {
                assertThat(nwavCatalogo.nwavBuscar(nwavDefinicion.getNwavSiguiente())).isPresent();
            }
        }
        assertThat(nwavCatalogo.nwavBuscar(NwavCatalogoHerramientas.NWAV_COMPRIMIR)).get()
                .satisfies(nwavDefinicion -> assertThat(nwavDefinicion.nwavCoincide("reducir tamaño")).isTrue());
    }
}
