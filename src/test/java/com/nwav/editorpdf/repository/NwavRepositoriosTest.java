package com.nwav.editorpdf.repository;

import com.nwav.editorpdf.model.NwavArchivoReciente;
import com.nwav.editorpdf.model.NwavConfiguracion;
import com.nwav.editorpdf.model.NwavFirma;
import com.nwav.editorpdf.model.NwavHistorialOperacion;
import com.nwav.editorpdf.model.NwavMarcaAgua;
import com.nwav.editorpdf.model.NwavModoZoom;
import com.nwav.editorpdf.model.NwavResultadoOperacion;
import com.nwav.editorpdf.model.NwavTipoMarcaAgua;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.soporte.NwavPruebaConBaseDatos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Repositorios sobre una base SQLite temporal creada por las migraciones de Flyway. */
class NwavRepositoriosTest extends NwavPruebaConBaseDatos {

    @Autowired private NwavArchivoRecienteRepository nwavRecientes;
    @Autowired private NwavConfiguracionRepository nwavConfiguraciones;
    @Autowired private NwavFirmaRepository nwavFirmas;
    @Autowired private NwavMarcaAguaRepository nwavMarcas;
    @Autowired private NwavHistorialOperacionRepository nwavHistorial;
    @Autowired private JdbcTemplate nwavJdbc;

    @BeforeEach
    void nwavLimpiar() {
        nwavRecientes.deleteAll();
        nwavConfiguraciones.deleteAll();
        nwavFirmas.deleteAll();
        nwavMarcas.deleteAll();
        nwavHistorial.deleteAll();
    }

    @Test
    void nwavLaBaseSeCreaEnLaCarpetaIndicadaConModoWal() {
        assertThat(Files.isRegularFile(nwavCarpetaDatos.resolve("nwav_editor.db"))).isTrue();
        assertThat(nwavJdbc.queryForObject("PRAGMA journal_mode", String.class)).isEqualToIgnoringCase("wal");
        assertThat(nwavJdbc.queryForObject("PRAGMA busy_timeout", Integer.class)).isEqualTo(5000);
    }

    @Test
    void nwavFlywayCreaTodasLasTablasConPrefijo() {
        List<String> nwavTablas = nwavJdbc.queryForList(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name",
                String.class);

        assertThat(nwavTablas).containsExactlyInAnyOrder(
                "nwav_archivo_reciente", "nwav_configuracion", "nwav_firma", "nwav_marca_agua",
                "nwav_historial_operacion", "nwav_flyway_historial");
        assertThat(nwavTablas).allMatch(nwavTabla -> nwavTabla.startsWith("nwav_"));
    }

    @Test
    void nwavGuardaYBuscaUnRecientePorRuta() {
        NwavArchivoReciente nwavReciente = new NwavArchivoReciente();
        nwavReciente.setNwavRuta("/tmp/informe.pdf");
        nwavReciente.setNwavNombre("informe.pdf");
        nwavReciente.setNwavUltimaApertura(LocalDateTime.of(2026, 11, 17, 10, 30));
        nwavReciente.setNwavUltimaPagina(4);
        nwavReciente.setNwavZoom(1.5);
        nwavReciente.setNwavModoZoom(NwavModoZoom.NWAV_PORCENTAJE);
        nwavRecientes.save(nwavReciente);

        NwavArchivoReciente nwavLeido = nwavRecientes.findByNwavRuta("/tmp/informe.pdf").orElseThrow();
        assertThat(nwavLeido.getNwavId()).isNotNull();
        assertThat(nwavLeido.getNwavUltimaPagina()).isEqualTo(4);
        assertThat(nwavLeido.getNwavZoom()).isEqualTo(1.5);
        assertThat(nwavLeido.getNwavModoZoom()).isEqualTo(NwavModoZoom.NWAV_PORCENTAJE);
        assertThat(nwavLeido.getNwavUltimaApertura()).isEqualTo(LocalDateTime.of(2026, 11, 17, 10, 30));
    }

    @Test
    void nwavLaRutaDeUnRecienteEsUnica() {
        nwavRecientes.saveAndFlush(nwavReciente("/tmp/a.pdf", 1));

        assertThatThrownBy(() -> nwavRecientes.saveAndFlush(nwavReciente("/tmp/a.pdf", 2)))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("UNIQUE");
    }

    @Test
    void nwavLosRecientesSeOrdenanDelMasNuevoAlMasAntiguo() {
        nwavRecientes.save(nwavReciente("/tmp/viejo.pdf", 1));
        nwavRecientes.save(nwavReciente("/tmp/nuevo.pdf", 3));
        nwavRecientes.save(nwavReciente("/tmp/medio.pdf", 2));

        assertThat(nwavRecientes.findAllByOrderByNwavUltimaAperturaDesc())
                .extracting(NwavArchivoReciente::getNwavNombre)
                .containsExactly("nuevo.pdf", "medio.pdf", "viejo.pdf");
    }

    @Test
    void nwavGuardaPreferenciasClaveValor() {
        nwavConfiguraciones.save(new NwavConfiguracion("nwav.tema", "oscuro"));
        nwavConfiguraciones.save(new NwavConfiguracion("nwav.tema", "claro"));

        assertThat(nwavConfiguraciones.findById("nwav.tema")).get()
                .extracting(NwavConfiguracion::getNwavValor).isEqualTo("claro");
        assertThat(nwavConfiguraciones.count()).isEqualTo(1);
    }

    @Test
    void nwavLaFirmaGuardaLaImagenComoBlob() {
        byte[] nwavPng = {(byte) 0x89, 'P', 'N', 'G', 0, 1, 2, 3, (byte) 0xFF};
        NwavFirma nwavFirma = new NwavFirma();
        nwavFirma.setNwavNombre("Firma principal");
        nwavFirma.setNwavImagen(nwavPng);
        nwavFirma.setNwavFechaCreacion(LocalDateTime.now());
        Long nwavId = nwavFirmas.save(nwavFirma).getNwavId();

        assertThat(nwavFirmas.findById(nwavId).orElseThrow().getNwavImagen()).containsExactly(nwavPng);
        assertThat(nwavJdbc.queryForObject("SELECT typeof(nwav_imagen) FROM nwav_firma WHERE nwav_id = ?",
                String.class, nwavId)).isEqualTo("blob");
    }

    @Test
    void nwavGuardaPlantillasDeMarcaDeAgua() {
        NwavMarcaAgua nwavMarca = new NwavMarcaAgua();
        nwavMarca.setNwavNombre("Confidencial");
        nwavMarca.setNwavTipo(NwavTipoMarcaAgua.NWAV_TEXTO);
        nwavMarca.setNwavTexto("CONFIDENCIAL");
        nwavMarca.setNwavTamanoFuente(48.0);
        nwavMarca.setNwavColor("#FF0000");
        nwavMarca.setNwavFechaCreacion(LocalDateTime.now());
        nwavMarcas.save(nwavMarca);

        NwavMarcaAgua nwavLeida = nwavMarcas.findAllByOrderByNwavNombreAsc().get(0);
        assertThat(nwavLeida.getNwavTipo()).isEqualTo(NwavTipoMarcaAgua.NWAV_TEXTO);
        assertThat(nwavLeida.getNwavOpacidad()).isEqualTo(0.3);
        assertThat(nwavLeida.getNwavImagen()).isNull();
    }

    @Test
    void nwavElHistorialSeOrdenaPorFecha() {
        nwavHistorial.save(nwavOperacion(NwavTipoOperacion.NWAV_ROTAR, LocalDateTime.now().minusHours(1)));
        nwavHistorial.save(nwavOperacion(NwavTipoOperacion.NWAV_UNIR, LocalDateTime.now()));

        assertThat(nwavHistorial.findAllByOrderByNwavFechaDescNwavIdDesc())
                .extracting(NwavHistorialOperacion::getNwavTipo)
                .containsExactly(NwavTipoOperacion.NWAV_UNIR, NwavTipoOperacion.NWAV_ROTAR);
    }

    private static NwavArchivoReciente nwavReciente(String nwavRuta, int nwavHorasDespues) {
        NwavArchivoReciente nwavReciente = new NwavArchivoReciente();
        nwavReciente.setNwavRuta(nwavRuta);
        nwavReciente.setNwavNombre(nwavRuta.substring(nwavRuta.lastIndexOf('/') + 1));
        nwavReciente.setNwavUltimaApertura(LocalDateTime.of(2026, 1, 1, 0, 0).plusHours(nwavHorasDespues));
        return nwavReciente;
    }

    private static NwavHistorialOperacion nwavOperacion(NwavTipoOperacion nwavTipo, LocalDateTime nwavFecha) {
        NwavHistorialOperacion nwavOperacion = new NwavHistorialOperacion();
        nwavOperacion.setNwavTipo(nwavTipo);
        nwavOperacion.setNwavArchivoOrigen("/tmp/origen.pdf");
        nwavOperacion.setNwavArchivoDestino("/tmp/destino.pdf");
        nwavOperacion.setNwavFecha(nwavFecha);
        nwavOperacion.setNwavResultado(NwavResultadoOperacion.NWAV_EXITO);
        return nwavOperacion;
    }
}
