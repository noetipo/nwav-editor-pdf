package com.nwav.editorpdf.model;

import java.util.List;

/**
 * Campo de un formulario AcroForm.
 *
 * @param nwavNombre    nombre completo (clave para rellenarlo)
 * @param nwavEtiqueta  texto para el usuario (descripcion del campo o su nombre)
 * @param nwavValor     valor actual; en casillas, "true"/"false"
 * @param nwavOpciones  valores posibles (opciones y listas)
 * @param nwavPagina    pagina del primer widget (-1 si no se sabe)
 * @param nwavZona      posicion visual del primer widget (puede ser null)
 */
public record NwavCampoFormulario(String nwavNombre, String nwavEtiqueta, NwavTipoCampo nwavTipo, String nwavValor,
                                  List<String> nwavOpciones, boolean nwavMultilinea, boolean nwavSoloLectura,
                                  boolean nwavRequerido, int nwavPagina, NwavRectangulo nwavZona) {

    public static final String NWAV_MARCADA = "true";
    public static final String NWAV_DESMARCADA = "false";
}
