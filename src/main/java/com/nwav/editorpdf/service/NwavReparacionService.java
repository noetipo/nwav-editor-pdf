package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavContrasenaIncorrectaException;
import com.nwav.editorpdf.exception.NwavContrasenaRequeridaException;
import com.nwav.editorpdf.exception.NwavEditorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NwavReparacionService {
    public record NwavResultadoReparacion(Path nwavDestino, int nwavPaginas, int nwavPaginasConProblemas,
                                          List<String> nwavProblemas) {
        public String nwavResumen() {
            return nwavPaginas + (nwavPaginas == 1 ? " página recuperada" : " páginas recuperadas")
                    + (nwavPaginasConProblemas == 0 ? " sin problemas"
                    : " · " + nwavPaginasConProblemas + " con problemas: " + String.join("; ", nwavProblemas));
        }
    }

    public NwavResultadoReparacion nwavReparar(Path nwavEntrada, String nwavContrasena, Path nwavDestino,
                                               NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) {
        String nwavNombre = String.valueOf(nwavEntrada.getFileName());
        if (!Files.isRegularFile(nwavEntrada)) {
            throw new com.nwav.editorpdf.exception.NwavArchivoNoEncontradoException(nwavNombre);
        }
        nwavAvance.nwavInformar(0, 1, "Analizando «" + nwavNombre + "»…");
        PDDocument nwavPd;
        try {
            nwavPd = Loader.loadPDF(new RandomAccessReadBufferedFile(nwavEntrada.toFile()),
                    nwavContrasena == null ? "" : nwavContrasena);
        } catch (InvalidPasswordException nwavError) {
            throw nwavContrasena == null || nwavContrasena.isEmpty()
                    ? new NwavContrasenaRequeridaException(nwavNombre, nwavError)
                    : new NwavContrasenaIncorrectaException(nwavNombre, nwavError);
        } catch (IOException | RuntimeException nwavError) {
            throw new NwavEditorException("«" + nwavNombre + "» no se puede recuperar: no tiene una estructura PDF "
                    + "reconocible (" + nwavError.getMessage() + ").", nwavError);
        }
        try (nwavPd) {
            int nwavTotal = nwavPd.getNumberOfPages();
            if (nwavTotal == 0) {
                throw new NwavEditorException("«" + nwavNombre + "» no se puede recuperar: no se encontró ninguna página.");
            }
            List<String> nwavProblemas = new ArrayList<>();
            PDFRenderer nwavRenderizador = new PDFRenderer(nwavPd);
            for (int nwavIndice = 0; nwavIndice < nwavTotal; nwavIndice++) {
                nwavCancelacion.nwavVerificar();
                nwavAvance.nwavInformar(nwavIndice, nwavTotal + 1, "Comprobando la página " + (nwavIndice + 1) + "…");
                try {
                    nwavRenderizador.renderImage(nwavIndice, 0.15f, ImageType.GRAY);
                } catch (IOException | RuntimeException nwavError) {
                    nwavProblemas.add("página " + (nwavIndice + 1) + " (" + nwavError.getClass().getSimpleName() + ")");
                }
            }
            nwavCancelacion.nwavVerificar();
            nwavAvance.nwavInformar(nwavTotal, nwavTotal + 1, "Reconstruyendo el archivo…");
            NwavEscrituraSegura.nwavMantenerProteccion(nwavPd, nwavContrasena);
            Path nwavGenerado = NwavEscrituraSegura.nwavGuardarPdf(nwavPd, nwavDestino);
            return new NwavResultadoReparacion(nwavGenerado, nwavTotal, nwavProblemas.size(), nwavProblemas);
        } catch (IOException nwavError) {
            throw new NwavEditorException("No se pudo reconstruir «" + nwavNombre + "».", nwavError);
        }
    }
}
