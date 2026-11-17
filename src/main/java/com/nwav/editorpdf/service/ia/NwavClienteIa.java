package com.nwav.editorpdf.service.ia;

import com.nwav.editorpdf.service.NwavCancelacion;

/** Llamada a un modelo de IA externo (separada para poder probar sin red). */
public interface NwavClienteIa {

    /**
     * @param nwavSistema instrucciones del sistema
     * @param nwavPdf     el PDF a adjuntar como documento, o null
     * @param nwavTexto   mensaje del usuario
     * @return el texto generado
     */
    String nwavGenerar(String nwavSistema, byte[] nwavPdf, String nwavTexto, NwavCancelacion nwavCancelacion);
}
