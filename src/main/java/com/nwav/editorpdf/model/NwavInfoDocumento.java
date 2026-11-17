package com.nwav.editorpdf.model;

/** Resumen del documento para el panel de propiedades. */
public record NwavInfoDocumento(NwavMetadatos nwavMetadatos, int nwavPaginas, String nwavVersionPdf,
                                long nwavTamanoBytes, boolean nwavCifrado, String nwavProteccionPendiente,
                                int nwavCamposFormulario) {
}
