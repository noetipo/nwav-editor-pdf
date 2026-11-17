package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavOpcionesEncabezado;
import com.nwav.editorpdf.model.NwavOpcionesMarcaAgua;
import com.nwav.editorpdf.service.comando.NwavComando;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class NwavEdicionLoteService {
    private final NwavPdfDocumentoService nwavDocumentos;
    private final NwavSelloService nwavSellos;

    public Path nwavMarcaAgua(Path nwavEntrada, String nwavContrasena, NwavOpcionesMarcaAgua nwavOpciones, Path nwavDestino) {
        return nwavAplicar(nwavEntrada, nwavContrasena, nwavDestino,
                nwavDocumento -> nwavSellos.nwavComandoMarcaAgua(nwavDocumento, nwavOpciones));
    }

    public Path nwavEncabezado(Path nwavEntrada, String nwavContrasena, NwavOpcionesEncabezado nwavOpciones, Path nwavDestino) {
        return nwavAplicar(nwavEntrada, nwavContrasena, nwavDestino,
                nwavDocumento -> nwavSellos.nwavComandoEncabezado(nwavDocumento, nwavOpciones));
    }

    private Path nwavAplicar(Path nwavEntrada, String nwavContrasena, Path nwavDestino,
                             Function<NwavDocumentoPdf, NwavComando> nwavComando) {
        try (NwavDocumentoPdf nwavDocumento = nwavDocumentos.nwavAbrir(nwavEntrada, nwavContrasena)) {
            nwavDocumento.getNwavGestor().nwavEjecutar(nwavComando.apply(nwavDocumento));
            synchronized (nwavDocumento) {
                NwavEscrituraSegura.nwavMantenerProteccion(nwavDocumento.nwavPdDocument(), nwavContrasena);
                return NwavEscrituraSegura.nwavGuardarPdf(nwavDocumento.nwavPdDocument(), nwavDestino);
            }
        }
    }
}
