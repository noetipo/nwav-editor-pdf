package com.nwav.editorpdf.controller;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Las carpetas de escaneos se importan en orden natural de nombre. */
class NwavOrdenNaturalTest {

    @Test
    void nwavLosNumerosSeComparanComoNumeros() {
        List<String> nwavNombres = new ArrayList<>(List.of("escaneo10.jpg", "Escaneo2.jpg", "escaneo1.jpg", "anexo.jpg",
                "escaneo002b.jpg"));
        nwavNombres.sort(NwavHerramientaController::nwavCompararNatural);
        assertThat(nwavNombres).containsExactly("anexo.jpg", "escaneo1.jpg", "Escaneo2.jpg", "escaneo002b.jpg",
                "escaneo10.jpg");
    }
}
