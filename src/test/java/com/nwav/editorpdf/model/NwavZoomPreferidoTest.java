package com.nwav.editorpdf.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NwavZoomPreferidoTest {

    @Test
    void nwavSerializaYDeserializaElPorcentaje() {
        NwavZoomPreferido nwavZoom = new NwavZoomPreferido(NwavModoZoom.NWAV_PORCENTAJE, 1.25);

        assertThat(NwavZoomPreferido.nwavDeserializar(nwavZoom.nwavSerializar())).isEqualTo(nwavZoom);
    }

    @Test
    void nwavLosModosDeAjusteNoGuardanFactor() {
        NwavZoomPreferido nwavZoom = new NwavZoomPreferido(NwavModoZoom.NWAV_AJUSTAR_PAGINA, 3);

        assertThat(nwavZoom.nwavSerializar()).isEqualTo("NWAV_AJUSTAR_PAGINA");
        assertThat(NwavZoomPreferido.nwavDeserializar("NWAV_AJUSTAR_PAGINA").nwavModo())
                .isEqualTo(NwavModoZoom.NWAV_AJUSTAR_PAGINA);
    }

    @Test
    void nwavValoresInvalidosUsanElDefecto() {
        assertThat(NwavZoomPreferido.nwavDeserializar("basura")).isEqualTo(NwavZoomPreferido.NWAV_POR_DEFECTO);
        assertThat(NwavZoomPreferido.nwavDeserializar(null)).isEqualTo(NwavZoomPreferido.NWAV_POR_DEFECTO);
    }

    @Test
    void nwavElFactorSeAcota() {
        assertThat(new NwavZoomPreferido(NwavModoZoom.NWAV_PORCENTAJE, 50).nwavFactor())
                .isEqualTo(NwavZoomPreferido.NWAV_FACTOR_MAXIMO);
        assertThat(new NwavZoomPreferido(NwavModoZoom.NWAV_PORCENTAJE, 0.01).nwavFactor())
                .isEqualTo(NwavZoomPreferido.NWAV_FACTOR_MINIMO);
    }
}
