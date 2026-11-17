package com.nwav.editorpdf;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class NwavLauncherTest {
    @Test
    void nwavCarpetaDeDatosSegunElSistema() {
        assertThat(NwavLauncher.nwavCarpetaDatos("Windows 11", "C:\\Users\\ana\\AppData\\Local"))
                .isEqualTo(Path.of("C:\\Users\\ana\\AppData\\Local", "NWAV Editor PDF").toString());
        assertThat(NwavLauncher.nwavCarpetaDatos("Windows 10", null)).isNull();
        assertThat(NwavLauncher.nwavCarpetaDatos("Mac OS X", "/x")).isNull();
        assertThat(NwavLauncher.nwavCarpetaDatos("Linux", null)).isNull();
    }
}
