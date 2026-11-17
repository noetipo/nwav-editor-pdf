package com.nwav.editorpdf;

import com.nwav.editorpdf.controller.NwavVisorController;
import com.nwav.editorpdf.soporte.NwavPruebaConBaseDatos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class NwavEditorPdfApplicationTests extends NwavPruebaConBaseDatos {

    @Autowired
    private ApplicationContext nwavContexto;

    @Test
    void nwavElContextoArrancaConLosControladores() {
        assertThat(nwavContexto.getBean(NwavVisorController.class)).isNotNull();
    }
}
