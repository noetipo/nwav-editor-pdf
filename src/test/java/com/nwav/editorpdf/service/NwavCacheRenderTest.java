package com.nwav.editorpdf.service;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class NwavCacheRenderTest {

    /** 10x10 px = 400 bytes. */
    private static BufferedImage nwavImagen() {
        return new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    }

    @Test
    void nwavDescartaLaMenosUsadaAlSuperarElLimite() {
        NwavCacheRender nwavCache = new NwavCacheRender(1000);
        nwavCache.nwavGuardar(0, 1f, nwavImagen());
        nwavCache.nwavGuardar(1, 1f, nwavImagen());
        nwavCache.nwavObtener(0, 1f); // la 0 pasa a ser la mas reciente
        nwavCache.nwavGuardar(2, 1f, nwavImagen());

        assertThat(nwavCache.nwavObtener(1, 1f)).isNull();
        assertThat(nwavCache.nwavObtener(0, 1f)).isNotNull();
        assertThat(nwavCache.nwavObtener(2, 1f)).isNotNull();
        assertThat(nwavCache.nwavBytesUsados()).isEqualTo(800);
    }

    @Test
    void nwavLaEscalaFormaParteDeLaClave() {
        NwavCacheRender nwavCache = new NwavCacheRender(10_000);
        nwavCache.nwavGuardar(0, 1f, nwavImagen());

        assertThat(nwavCache.nwavObtener(0, 1.5f)).isNull();
        assertThat(nwavCache.nwavObtener(0, 1.0001f)).isNotNull();
    }

    @Test
    void nwavNoGuardaImagenesMayoresQueLaCache() {
        NwavCacheRender nwavCache = new NwavCacheRender(100);
        nwavCache.nwavGuardar(0, 1f, nwavImagen());

        assertThat(nwavCache.nwavCantidad()).isZero();
    }

    @Test
    void nwavLimpiarVaciaTodo() {
        NwavCacheRender nwavCache = new NwavCacheRender(10_000);
        nwavCache.nwavGuardar(0, 1f, nwavImagen());
        nwavCache.nwavGuardar(0, 1f, nwavImagen()); // reemplazo: no duplica bytes
        assertThat(nwavCache.nwavBytesUsados()).isEqualTo(400);

        nwavCache.nwavLimpiar();

        assertThat(nwavCache.nwavCantidad()).isZero();
        assertThat(nwavCache.nwavBytesUsados()).isZero();
    }
}
