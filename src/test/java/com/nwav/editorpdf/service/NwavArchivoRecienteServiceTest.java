package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavArchivoReciente;
import com.nwav.editorpdf.model.NwavModoZoom;
import com.nwav.editorpdf.model.NwavZoomPreferido;
import com.nwav.editorpdf.repository.NwavArchivoRecienteRepository;
import com.nwav.editorpdf.repository.NwavConfiguracionRepository;
import com.nwav.editorpdf.service.NwavArchivoRecienteService.NwavRecienteVista;
import com.nwav.editorpdf.soporte.NwavPdfDePrueba;
import com.nwav.editorpdf.soporte.NwavPruebaConBaseDatos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NwavArchivoRecienteServiceTest extends NwavPruebaConBaseDatos {

    @Autowired private NwavArchivoRecienteService nwavServicio;
    @Autowired private NwavConfiguracionService nwavConfiguracion;
    @Autowired private NwavArchivoRecienteRepository nwavRepositorio;
    @Autowired private NwavConfiguracionRepository nwavConfiguraciones;

    @TempDir
    Path nwavCarpeta;

    @BeforeEach
    void nwavLimpiar() {
        nwavRepositorio.deleteAll();
        nwavConfiguraciones.deleteAll();
        nwavConfiguracion.nwavRecargar();
    }

    @Test
    void nwavUnArchivoNuevoEmpiezaEnLaPrimeraPaginaConElZoomPorDefecto() throws Exception {
        nwavConfiguracion.nwavGuardarZoomPorDefecto(new NwavZoomPreferido(NwavModoZoom.NWAV_PORCENTAJE, 1.5));
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "nuevo.pdf", 1);

        NwavArchivoReciente nwavReciente = nwavServicio.nwavRegistrarApertura(nwavPdf);

        assertThat(nwavReciente.getNwavNombre()).isEqualTo("nuevo.pdf");
        assertThat(nwavReciente.getNwavRuta()).isEqualTo(nwavPdf.toAbsolutePath().normalize().toString());
        assertThat(nwavReciente.getNwavUltimaPagina()).isZero();
        assertThat(nwavReciente.getNwavModoZoom()).isEqualTo(NwavModoZoom.NWAV_PORCENTAJE);
        assertThat(nwavReciente.getNwavZoom()).isEqualTo(1.5);
    }

    @Test
    void nwavReabrirConservaLaPaginaYElZoomGuardados() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "informe.pdf", 5);
        nwavServicio.nwavRegistrarApertura(nwavPdf);
        nwavServicio.nwavGuardarEstado(nwavPdf, 3, NwavModoZoom.NWAV_PORCENTAJE, 2.0);

        nwavServicio.nwavRegistrarApertura(nwavPdf);

        NwavArchivoReciente nwavLeido = nwavServicio.nwavBuscar(nwavPdf).orElseThrow();
        assertThat(nwavLeido.getNwavUltimaPagina()).isEqualTo(3);
        assertThat(nwavLeido.getNwavZoomPreferido())
                .isEqualTo(new NwavZoomPreferido(NwavModoZoom.NWAV_PORCENTAJE, 2.0));
        assertThat(nwavRepositorio.count()).isEqualTo(1);
    }

    @Test
    void nwavLaRutaSeNormaliza() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "ruta.pdf", 1);
        Path nwavRodeo = nwavCarpeta.resolve("sub").resolve("..").resolve("ruta.pdf");

        nwavServicio.nwavRegistrarApertura(nwavPdf);
        nwavServicio.nwavRegistrarApertura(nwavRodeo);

        assertThat(nwavRepositorio.count()).isEqualTo(1);
    }

    @Test
    void nwavLosArchivosBorradosSeMarcanNoDisponiblesYSePuedenQuitar() throws Exception {
        Path nwavExiste = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "existe.pdf", 1);
        Path nwavBorrado = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "borrado.pdf", 1);
        nwavServicio.nwavRegistrarApertura(nwavExiste);
        nwavServicio.nwavRegistrarApertura(nwavBorrado);
        Files.delete(nwavBorrado);

        List<NwavRecienteVista> nwavLista = nwavServicio.nwavListar();
        assertThat(nwavLista).hasSize(2);
        assertThat(nwavLista).filteredOn(NwavRecienteVista::nwavDisponible)
                .extracting(nwavVista -> nwavVista.nwavArchivo().getNwavNombre())
                .containsExactly("existe.pdf");

        assertThat(nwavServicio.nwavQuitarNoDisponibles()).isEqualTo(1);
        assertThat(nwavServicio.nwavListar()).hasSize(1);
    }

    @Test
    void nwavLaListaSeRecortaAlMaximoConfigurado() throws Exception {
        nwavConfiguracion.nwavGuardarMaximoRecientes(5);
        for (int nwavIndice = 0; nwavIndice < 7; nwavIndice++) {
            Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "doc" + nwavIndice + ".pdf", 1);
            nwavServicio.nwavRegistrarApertura(nwavPdf);
            Thread.sleep(5); // fechas de apertura distintas
        }

        assertThat(nwavRepositorio.count()).isEqualTo(5);
        assertThat(nwavServicio.nwavListar())
                .extracting(nwavVista -> nwavVista.nwavArchivo().getNwavNombre())
                .startsWith("doc6.pdf")
                .doesNotContain("doc0.pdf", "doc1.pdf");
    }

    @Test
    void nwavQuitarUnRecienteNoBorraElArchivo() throws Exception {
        Path nwavPdf = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "conservar.pdf", 1);
        Long nwavId = nwavServicio.nwavRegistrarApertura(nwavPdf).getNwavId();

        nwavServicio.nwavQuitar(nwavId);

        assertThat(nwavServicio.nwavListar()).isEmpty();
        assertThat(Files.exists(nwavPdf)).isTrue();
    }
}
