package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.model.NwavHistorialOperacion;
import com.nwav.editorpdf.model.NwavResultadoOperacion;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.repository.NwavHistorialOperacionRepository;
import com.nwav.editorpdf.soporte.NwavLectorPdf;
import com.nwav.editorpdf.soporte.NwavPdfDePrueba;
import com.nwav.editorpdf.soporte.NwavPruebaConBaseDatos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NwavArchivoPdfServiceTest extends NwavPruebaConBaseDatos {
    @Autowired private NwavArchivoPdfService nwavArchivos;
    @Autowired private NwavEdicionService nwavEdicion;
    @Autowired private NwavPdfDocumentoService nwavDocumentos;
    @Autowired private NwavHistorialOperacionRepository nwavHistorial;

    @TempDir
    Path nwavCarpeta;

    @BeforeEach
    void nwavLimpiar() {
        nwavHistorial.deleteAll();
    }

    private List<NwavTipoOperacion> nwavTiposRegistrados() {
        return nwavHistorial.findAllByOrderByNwavFechaDescNwavIdDesc().stream().map(NwavHistorialOperacion::getNwavTipo).toList();
    }

    @Test
    void nwavGuardarComoEscribeLosCambiosSinTocarElOriginal() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "original.pdf", 4);
        Path nwavCopia = nwavCarpeta.resolve("copia.pdf");
        NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavOriginal, null);
        nwavEdicion.nwavEliminar(nwavDocumento, List.of(0));
        nwavEdicion.nwavMover(nwavDocumento, List.of(2), 0);

        try (NwavDocumentoPdf nwavGuardado = nwavArchivos.nwavGuardarComo(nwavDocumento, nwavCopia)) {
            assertThat(nwavDocumento.isNwavCerrado()).isTrue();
            assertThat(nwavGuardado.getNwavRuta()).isEqualTo(nwavCopia.toAbsolutePath().normalize());
            assertThat(nwavGuardado.nwavHayCambiosSinGuardar()).isFalse();
        }
        assertThat(NwavLectorPdf.nwavTextos(nwavCopia)).containsExactly("Pagina4", "Pagina2", "Pagina3");
        assertThat(NwavLectorPdf.nwavTextos(nwavOriginal)).hasSize(4);
        assertThat(nwavTiposRegistrados()).containsExactly(
                NwavTipoOperacion.NWAV_GUARDAR, NwavTipoOperacion.NWAV_REORDENAR, NwavTipoOperacion.NWAV_ELIMINAR);
        assertThat(Files.list(nwavCarpeta).filter(nwavRuta -> nwavRuta.getFileName().toString().startsWith(".nwav")))
                .as("no quedan temporales").isEmpty();
    }

    @Test
    void nwavSobrescribirElOriginalFuncionaConElDocumentoAbierto() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "mismo.pdf", 3);
        NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavOriginal, null);
        nwavEdicion.nwavRotar(nwavDocumento, List.of(1), 90);

        try (NwavDocumentoPdf nwavGuardado = nwavArchivos.nwavGuardarComo(nwavDocumento, nwavOriginal)) {
            assertThat(nwavGuardado.nwavTamano(1).nwavAncho()).isGreaterThan(nwavGuardado.nwavTamano(1).nwavAlto());
        }
        NwavHistorialOperacion nwavGuardar = nwavHistorial.findAllByOrderByNwavFechaDescNwavIdDesc().get(0);
        assertThat(nwavGuardar.getNwavDetalle()).contains("sobrescribió");
    }

    @Test
    void nwavUnPdfCifradoSigueCifradoTrasGuardar() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrearCifrado(nwavCarpeta, "cifrado.pdf", 3, "clave");
        Path nwavCopia = nwavCarpeta.resolve("cifrado-editado.pdf");
        NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavOriginal, "clave");
        nwavEdicion.nwavEliminar(nwavDocumento, List.of(2));

        try (NwavDocumentoPdf nwavGuardado = nwavArchivos.nwavGuardarComo(nwavDocumento, nwavCopia)) {
            assertThat(nwavGuardado.isNwavCifrado()).isTrue();
            assertThat(nwavGuardado.getNwavNumeroPaginas()).isEqualTo(2);
        }
        assertThat(nwavDocumentos.nwavRequiereContrasena(nwavCopia)).isTrue();
    }

    @Test
    void nwavExtraerCreaUnPdfConLasPaginasElegidas() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrearConRotacion(nwavCarpeta, "base.pdf", 5, 90);
        Path nwavDestino = nwavCarpeta.resolve("extracto.pdf");
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavOriginal, null)) {
            nwavArchivos.nwavExtraer(nwavDocumento, List.of(4, 1), nwavDestino);

            assertThat(nwavDocumento.getNwavNumeroPaginas()).isEqualTo(5);
        }
        assertThat(NwavLectorPdf.nwavTextos(nwavDestino)).containsExactly("Pagina5", "Pagina2");
        try (NwavDocumentoPdf nwavExtracto = nwavDocumentos.nwavAbrir(nwavDestino, null)) {
            assertThat(nwavExtracto.nwavTamano(1).nwavAncho()).as("conserva la rotacion")
                    .isGreaterThan(nwavExtracto.nwavTamano(1).nwavAlto());
        }
        assertThat(nwavTiposRegistrados()).containsExactly(NwavTipoOperacion.NWAV_EXTRAER);
    }

    @Test
    void nwavNoSePuedeExtraerSobreElDocumentoAbierto() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "abierto.pdf", 2);
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavOriginal, null)) {
            assertThatThrownBy(() -> nwavArchivos.nwavExtraer(nwavDocumento, List.of(0), nwavOriginal))
                    .isInstanceOf(NwavEditorException.class);
        }
        assertThat(nwavHistorial.findAll()).singleElement()
                .extracting(NwavHistorialOperacion::getNwavResultado).isEqualTo(NwavResultadoOperacion.NWAV_ERROR);
    }

    @Test
    void nwavDividirPorRangosYCadaN() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "largo.pdf", 7);
        Path nwavSalida = Files.createDirectory(nwavCarpeta.resolve("partes"));
        List<String> nwavAvances = new ArrayList<>();
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavOriginal, null)) {
            List<Path> nwavPartes = nwavArchivos.nwavDividir(nwavDocumento,
                    NwavRangosPaginas.nwavInterpretar("1-3, 4, 5-", 7), nwavSalida, "informe",
                    (nwavHecho, nwavTotal, nwavMensaje) -> nwavAvances.add(nwavMensaje));

            assertThat(nwavPartes).extracting(nwavParte -> nwavParte.getFileName().toString()).containsExactly(
                    "informe - parte 01 (pág. 1-3).pdf", "informe - parte 02 (pág. 4).pdf",
                    "informe - parte 03 (pág. 5-7).pdf");
            assertThat(NwavLectorPdf.nwavTextos(nwavPartes.get(2))).containsExactly("Pagina5", "Pagina6", "Pagina7");

            List<Path> nwavOtraVez = nwavArchivos.nwavDividir(nwavDocumento, NwavRangosPaginas.nwavCada(3, 7),
                    nwavSalida, "informe", NwavAvance.NWAV_NINGUNO);
            assertThat(nwavOtraVez).hasSize(3);
            assertThat(nwavOtraVez.get(0).getFileName().toString()).isEqualTo("informe - parte 01 (pág. 1-3) (2).pdf");
        }
        assertThat(nwavAvances).isNotEmpty();
        assertThat(Files.list(nwavSalida)).hasSize(6);
    }

    @Test
    void nwavUnirVariosPdfEnOrden() throws Exception {
        Path nwavA = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "a.pdf", 2);
        Path nwavB = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "b.pdf", 3);
        Path nwavDestino = nwavCarpeta.resolve("unido.pdf");

        nwavArchivos.nwavUnir(List.of(nwavB, nwavA), nwavDestino, NwavAvance.NWAV_NINGUNO);

        assertThat(NwavLectorPdf.nwavTextos(nwavDestino)).containsExactly(
                "Pagina1", "Pagina2", "Pagina3", "Pagina1", "Pagina2");
        NwavHistorialOperacion nwavFila = nwavHistorial.findAll().get(0);
        assertThat(nwavFila.getNwavTipo()).isEqualTo(NwavTipoOperacion.NWAV_UNIR);
        assertThat(nwavFila.getNwavArchivoOrigen()).contains("b.pdf; ").contains("a.pdf");
    }

    @Test
    void nwavUnirConUnPdfProtegidoExplicaElMotivo() throws Exception {
        Path nwavA = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "a.pdf", 1);
        Path nwavProtegido = NwavPdfDePrueba.nwavCrearCifrado(nwavCarpeta, "secreto.pdf", 1, "x");

        assertThatThrownBy(() -> nwavArchivos.nwavUnir(List.of(nwavA, nwavProtegido),
                nwavCarpeta.resolve("u.pdf"), NwavAvance.NWAV_NINGUNO))
                .isInstanceOf(NwavEditorException.class)
                .hasMessageContaining("secreto.pdf")
                .hasMessageContaining("contraseña");
        assertThat(nwavCarpeta.resolve("u.pdf")).doesNotExist();
    }

    @Test
    void nwavUnirNecesitaAlMenosDosArchivos() throws Exception {
        Path nwavA = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "solo.pdf", 1);

        assertThatThrownBy(() -> nwavArchivos.nwavUnir(List.of(nwavA), nwavCarpeta.resolve("u.pdf"),
                NwavAvance.NWAV_NINGUNO)).hasMessageContaining("al menos dos");
    }

    @Test
    void nwavDeshacerYRehacerQuedanEnElHistorial() throws Exception {
        Path nwavOriginal = NwavPdfDePrueba.nwavCrear(nwavCarpeta, "h.pdf", 2);
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavOriginal, null)) {
            nwavEdicion.nwavDuplicar(nwavDocumento, List.of(0));
            assertThat(nwavEdicion.nwavDeshacer(nwavDocumento)).isPresent();
            assertThat(nwavEdicion.nwavRehacer(nwavDocumento)).get()
                    .extracting(NwavEdicionService.NwavResultadoEdicion::nwavPaginaDestacada).isEqualTo(1);
            assertThat(nwavEdicion.nwavRehacer(nwavDocumento)).isEmpty();
        }
        assertThat(nwavTiposRegistrados()).containsExactly(
                NwavTipoOperacion.NWAV_REHACER, NwavTipoOperacion.NWAV_DESHACER, NwavTipoOperacion.NWAV_DUPLICAR);
        assertThat(nwavHistorial.findAllByOrderByNwavFechaDescNwavIdDesc().get(2).getNwavDetalle()).contains("sin guardar");
    }
}
