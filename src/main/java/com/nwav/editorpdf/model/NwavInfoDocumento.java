package com.nwav.editorpdf.model;

public record NwavInfoDocumento(NwavMetadatos nwavMetadatos, int nwavPaginas, String nwavVersionPdf,
                                long nwavTamanoBytes, boolean nwavCifrado, String nwavProteccionPendiente,
                                int nwavCamposFormulario) {
}
