package com.nwav.editorpdf.service.ia;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.service.NwavAvance;
import com.nwav.editorpdf.service.NwavCancelacion;
import com.nwav.editorpdf.service.NwavConversionAPdfService;
import com.nwav.editorpdf.service.NwavConversionDesdePdfService;
import com.nwav.editorpdf.service.NwavEscrituraSegura;
import com.nwav.editorpdf.service.NwavPdfDocumentoService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Resumir y traducir PDF con IA (opcional). El contenido del documento SE ENVIA a un servicio
 * externo (Anthropic); por eso esta desactivado por defecto, pide confirmacion en cada uso y
 * el resultado lleva un aviso de que lo ha generado una IA.
 */
@Service
@RequiredArgsConstructor
public class NwavIaService {

    /** Limites del envio del PDF como documento; por encima se envia el texto extraido. */
    static final long NWAV_BYTES_MAXIMOS_PDF = 30L * 1024 * 1024;
    static final int NWAV_PAGINAS_MAXIMAS_PDF = 600;
    /** Tamano de cada trozo que se traduce (caracteres), respetando paginas. */
    static final int NWAV_TROZO_TRADUCCION = 12_000;
    /** Texto maximo que se envia para resumir cuando no se envia el PDF. */
    static final int NWAV_TEXTO_MAXIMO_RESUMEN = 1_500_000;

    public static final List<String> NWAV_IDIOMAS = List.of("español", "inglés", "francés", "alemán", "italiano",
            "portugués", "catalán", "gallego", "euskera", "neerlandés", "chino simplificado", "japonés", "coreano",
            "ruso", "árabe");

    private final NwavClienteIa nwavCliente;
    private final NwavPdfDocumentoService nwavDocumentos;
    private final NwavConversionDesdePdfService nwavDesdePdf;
    private final NwavConversionAPdfService nwavAPdf;

    public enum NwavLongitud {
        NWAV_BREVE("un párrafo breve (unas 120 palabras) y, si ayuda, hasta 5 viñetas"),
        NWAV_NORMAL("un resumen de una página: idea general, puntos clave en viñetas y conclusiones"),
        NWAV_DETALLADO("un resumen detallado por secciones, siguiendo la estructura del documento, con los datos y "
                + "cifras importantes");

        private final String nwavInstruccion;

        NwavLongitud(String nwavInstruccion) {
            this.nwavInstruccion = nwavInstruccion;
        }
    }

    // ================================================================ resumir

    public Path nwavResumir(Path nwavEntrada, String nwavContrasena, String nwavIdioma, NwavLongitud nwavLongitud,
                            boolean nwavComoPdf, Path nwavDestino, NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) {
        String nwavIdiomaFinal = nwavIdioma(nwavIdioma);
        String nwavSistema = "Eres un asistente que resume documentos con precisión. Responde siempre en "
                + nwavIdiomaFinal + ", en Markdown (títulos con #, viñetas con -). Usa solo información del documento: "
                + "no inventes datos, y si algo no está claro, dilo. No añadas preámbulos como «Aquí tienes el resumen».";
        String nwavPeticion = "Resume este documento en " + nwavLongitud.nwavInstruccion + ".";

        nwavAvance.nwavInformar(0, 3, "Preparando el documento…");
        byte[] nwavPdf = nwavPdfParaEnviar(nwavEntrada, nwavContrasena);
        String nwavRespuesta;
        nwavCancelacion.nwavVerificar();
        if (nwavPdf != null) {
            nwavAvance.nwavInformar(1, 3, "Enviando el documento a la IA…");
            nwavRespuesta = nwavCliente.nwavGenerar(nwavSistema, nwavPdf, nwavPeticion, nwavCancelacion);
        } else {
            String nwavTexto = String.join("\n\n", nwavDesdePdf.nwavMarkdownPorPagina(nwavEntrada, nwavContrasena,
                    nwavCancelacion));
            nwavComprobarTexto(nwavTexto);
            if (nwavTexto.length() > NWAV_TEXTO_MAXIMO_RESUMEN) {
                throw new NwavEditorException("El documento es demasiado largo para resumirlo de una vez. Divídelo antes "
                        + "(Organizar → Dividir PDF) y resume cada parte.");
            }
            nwavAvance.nwavInformar(1, 3, "Enviando el texto a la IA…");
            nwavRespuesta = nwavCliente.nwavGenerar(nwavSistema, null,
                    nwavPeticion + "\n\n<documento>\n" + nwavTexto + "\n</documento>", nwavCancelacion);
        }
        nwavAvance.nwavInformar(2, 3, "Guardando…");
        String nwavMarkdown = nwavCabecera("Resumen de «" + nwavEntrada.getFileName() + "»", nwavIdiomaFinal)
                + nwavRespuesta + "\n";
        return nwavGuardar(nwavMarkdown, "Resumen de " + nwavEntrada.getFileName(), nwavComoPdf, nwavDestino);
    }

    // =============================================================== traducir

    public Path nwavTraducir(Path nwavEntrada, String nwavContrasena, String nwavIdioma, boolean nwavComoPdf,
                             Path nwavDestino, NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) {
        String nwavIdiomaFinal = nwavIdioma(nwavIdioma);
        nwavAvance.nwavInformar(-1, 1, "Leyendo el texto del documento…");
        List<String> nwavPaginas = nwavDesdePdf.nwavMarkdownPorPagina(nwavEntrada, nwavContrasena, nwavCancelacion);
        nwavComprobarTexto(String.join("", nwavPaginas));
        List<String> nwavTrozos = nwavTrozos(nwavPaginas, NWAV_TROZO_TRADUCCION);
        String nwavSistema = "Eres un traductor profesional. Traduce al " + nwavIdiomaFinal + " el texto en Markdown que "
                + "recibes dentro de <texto>. Conserva exactamente la estructura Markdown (títulos, listas, tablas, "
                + "enlaces y saltos de párrafo), los números, nombres propios y URLs. Devuelve solo la traducción, sin "
                + "comentarios ni las etiquetas <texto>. Si un fragmento ya está en " + nwavIdiomaFinal
                + ", déjalo igual.";
        StringBuilder nwavTraduccion = new StringBuilder();
        for (int nwavIndice = 0; nwavIndice < nwavTrozos.size(); nwavIndice++) {
            nwavCancelacion.nwavVerificar();
            nwavAvance.nwavInformar(nwavIndice, nwavTrozos.size() + 1, "Traduciendo la parte " + (nwavIndice + 1) + " de "
                    + nwavTrozos.size() + "…");
            String nwavTraducido = nwavCliente.nwavGenerar(nwavSistema, null,
                    "<texto>\n" + nwavTrozos.get(nwavIndice) + "\n</texto>", nwavCancelacion);
            if (!nwavTraduccion.isEmpty()) {
                nwavTraduccion.append("\n\n");
            }
            nwavTraduccion.append(nwavTraducido.replace("<texto>", "").replace("</texto>", "").strip());
        }
        nwavAvance.nwavInformar(nwavTrozos.size(), nwavTrozos.size() + 1, "Guardando…");
        String nwavMarkdown = nwavCabecera("Traducción de «" + nwavEntrada.getFileName() + "»", nwavIdiomaFinal)
                + nwavTraduccion + "\n";
        return nwavGuardar(nwavMarkdown, "Traducción de " + nwavEntrada.getFileName(), nwavComoPdf, nwavDestino);
    }

    /** Agrupa paginas en trozos de como maximo {@code nwavMaximo} caracteres (una pagina enorme se parte por parrafos). */
    static List<String> nwavTrozos(List<String> nwavPaginas, int nwavMaximo) {
        List<String> nwavPiezas = new ArrayList<>();
        for (String nwavPagina : nwavPaginas) {
            if (nwavPagina.isBlank()) {
                continue;
            }
            if (nwavPagina.length() <= nwavMaximo) {
                nwavPiezas.add(nwavPagina.strip());
                continue;
            }
            StringBuilder nwavActual = new StringBuilder();
            for (String nwavParrafo : nwavPagina.split("\n\n")) {
                if (nwavActual.length() + nwavParrafo.length() + 2 > nwavMaximo && !nwavActual.isEmpty()) {
                    nwavPiezas.add(nwavActual.toString().strip());
                    nwavActual.setLength(0);
                }
                // un parrafo gigante sin saltos se corta tal cual
                for (int nwavInicio = 0; nwavInicio < nwavParrafo.length(); nwavInicio += nwavMaximo) {
                    String nwavParte = nwavParrafo.substring(nwavInicio, Math.min(nwavParrafo.length(),
                            nwavInicio + nwavMaximo));
                    if (nwavParrafo.length() > nwavMaximo) {
                        nwavPiezas.add(nwavParte);
                    } else {
                        nwavActual.append(nwavParte).append("\n\n");
                    }
                }
            }
            if (!nwavActual.isEmpty()) {
                nwavPiezas.add(nwavActual.toString().strip());
            }
        }
        List<String> nwavTrozos = new ArrayList<>();
        StringBuilder nwavTrozo = new StringBuilder();
        for (String nwavPieza : nwavPiezas) {
            if (!nwavTrozo.isEmpty() && nwavTrozo.length() + nwavPieza.length() + 2 > nwavMaximo) {
                nwavTrozos.add(nwavTrozo.toString());
                nwavTrozo.setLength(0);
            }
            if (!nwavTrozo.isEmpty()) {
                nwavTrozo.append("\n\n");
            }
            nwavTrozo.append(nwavPieza);
        }
        if (!nwavTrozo.isEmpty()) {
            nwavTrozos.add(nwavTrozo.toString());
        }
        return nwavTrozos;
    }

    // ================================================================ internos

    private static String nwavIdioma(String nwavIdioma) {
        return nwavIdioma == null || nwavIdioma.isBlank() ? "español" : nwavIdioma.strip();
    }

    private static void nwavComprobarTexto(String nwavTexto) {
        if (nwavTexto.isBlank()) {
            throw new NwavEditorException("El PDF no contiene texto seleccionable (¿es un escaneo?). "
                    + "Aplica primero la herramienta OCR.");
        }
    }

    static String nwavCabecera(String nwavTitulo, String nwavIdioma) {
        return "# " + nwavTitulo + "\n\n> Generado con IA (Claude, de Anthropic) el "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT)) + ", en " + nwavIdioma
                + ". Puede contener errores: compruébalo con el documento original.\n\n";
    }

    /** PDF sin contrasena, listo para adjuntar; null si es demasiado grande (se enviara el texto). */
    private byte[] nwavPdfParaEnviar(Path nwavEntrada, String nwavContrasena) {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            if (nwavPd.getNumberOfPages() > NWAV_PAGINAS_MAXIMAS_PDF) {
                return null;
            }
            if (!nwavPd.isEncrypted() && Files.size(nwavEntrada) <= NWAV_BYTES_MAXIMOS_PDF) {
                return Files.readAllBytes(nwavEntrada);
            }
            nwavPd.setAllSecurityToBeRemoved(true);
            ByteArrayOutputStream nwavBytes = new ByteArrayOutputStream();
            nwavPd.save(nwavBytes);
            return nwavBytes.size() <= NWAV_BYTES_MAXIMOS_PDF ? nwavBytes.toByteArray() : null;
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    private Path nwavGuardar(String nwavMarkdown, String nwavTitulo, boolean nwavComoPdf, Path nwavDestino) {
        if (nwavComoPdf) {
            return nwavAPdf.nwavHtmlCadenaAPdf(NwavMarkdownHtml.nwavConvertir(nwavMarkdown, nwavTitulo),
                    new NwavConversionAPdfService.NwavOpcionesHtml("A4", false, 18, true), nwavDestino);
        }
        return NwavEscrituraSegura.nwavEscribir(nwavDestino, nwavTemporal ->
                Files.writeString(nwavTemporal, nwavMarkdown, StandardCharsets.UTF_8));
    }
}
