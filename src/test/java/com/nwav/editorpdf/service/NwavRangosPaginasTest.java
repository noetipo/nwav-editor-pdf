package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.service.NwavRangosPaginas.NwavRango;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NwavRangosPaginasTest {

    @Test
    void nwavInterpretaRangosSueltosYAbiertos() {
        assertThat(NwavRangosPaginas.nwavInterpretar(" 1-3, 5 ; 8- ,-2", 10)).containsExactly(
                new NwavRango(0, 2), new NwavRango(4, 4), new NwavRango(7, 9), new NwavRango(0, 1));
    }

    @Test
    void nwavIndicesSinRepetirEnElOrdenEscrito() {
        assertThat(NwavRangosPaginas.nwavIndices("5, 1-2, 2", 6)).containsExactly(4, 0, 1);
    }

    @Test
    void nwavRechazaPaginasInexistentesConMensajeClaro() {
        assertThatThrownBy(() -> NwavRangosPaginas.nwavInterpretar("2-12", 10))
                .isInstanceOf(NwavEditorException.class)
                .hasMessageContaining("La página 12 no existe")
                .hasMessageContaining("10 páginas");
    }

    @Test
    void nwavRechazaTextoInvalidoYRangosInvertidos() {
        assertThatThrownBy(() -> NwavRangosPaginas.nwavInterpretar("a-b", 5)).hasMessageContaining("no es un rango válido");
        assertThatThrownBy(() -> NwavRangosPaginas.nwavInterpretar("4-2", 5)).hasMessageContaining("menor o igual");
        assertThatThrownBy(() -> NwavRangosPaginas.nwavInterpretar("  ", 5)).isInstanceOf(NwavEditorException.class);
    }

    @Test
    void nwavDivideCadaNPaginas() {
        assertThat(NwavRangosPaginas.nwavCada(3, 7)).containsExactly(
                new NwavRango(0, 2), new NwavRango(3, 5), new NwavRango(6, 6));
    }

    @Test
    void nwavDescribeUnaSeleccionDeFormaCompacta() {
        assertThat(NwavRangosPaginas.nwavDescribir(List.of(4, 0, 1, 2, 7, 8))).isEqualTo("1-3, 5, 8-9");
        assertThat(new NwavRango(3, 3).nwavDescripcion()).isEqualTo("4");
    }
}
