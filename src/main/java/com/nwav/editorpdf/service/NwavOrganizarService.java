package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavContrasenaRequeridaException;
import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.service.NwavRangosPaginas.NwavRango;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class NwavOrganizarService {
    public static final double NWAV_PUNTOS_POR_MM = 72.0 / 25.4;

    private final NwavPdfDocumentoService nwavDocumentos;

    public Path nwavUnir(List<Path> nwavOrigenes, Map<Path, String> nwavContrasenas, Path nwavDestino,
                         NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) {
        if (nwavOrigenes == null || nwavOrigenes.size() < 2) {
            throw new NwavEditorException("Elige al menos dos PDF para unir.");
        }
        List<PDDocument> nwavAbiertos = new ArrayList<>();
        try (PDDocument nwavResultado = new PDDocument()) {
            PDFMergerUtility nwavUnion = new PDFMergerUtility();
            for (int nwavIndice = 0; nwavIndice < nwavOrigenes.size(); nwavIndice++) {
                nwavCancelacion.nwavVerificar();
                Path nwavOrigen = nwavOrigenes.get(nwavIndice);
                nwavAvance.nwavInformar(nwavIndice, nwavOrigenes.size() + 1, "Añadiendo «" + nwavOrigen.getFileName() + "»…");
                PDDocument nwavFuente = nwavDocumentos.nwavCargarDocumento(nwavOrigen,
                        nwavContrasenas == null ? null : nwavContrasenas.get(nwavOrigen));
                nwavAbiertos.add(nwavFuente);
                nwavUnion.appendDocument(nwavResultado, nwavFuente);
            }
            nwavCancelacion.nwavVerificar();
            nwavAvance.nwavInformar(nwavOrigenes.size(), nwavOrigenes.size() + 1, "Guardando…");
            return NwavEscrituraSegura.nwavGuardarPdf(nwavResultado, nwavDestino);
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavDestino.getFileName()));
        } finally {
            nwavAbiertos.forEach(NwavOrganizarService::nwavCerrar);
        }
    }

    public List<Path> nwavDividirPorRangos(Path nwavEntrada, String nwavContrasena, String nwavRangos, Path nwavCarpeta,
                                           NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) {
        return nwavDividir(nwavEntrada, nwavContrasena, nwavCarpeta, nwavAvance, nwavCancelacion,
                nwavTotal -> NwavRangosPaginas.nwavInterpretar(nwavRangos, nwavTotal));
    }

    public List<Path> nwavDividirCada(Path nwavEntrada, String nwavContrasena, int nwavPaginas, Path nwavCarpeta,
                                      NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) {
        return nwavDividir(nwavEntrada, nwavContrasena, nwavCarpeta, nwavAvance, nwavCancelacion,
                nwavTotal -> NwavRangosPaginas.nwavCada(nwavPaginas, nwavTotal));
    }

    public List<Path> nwavDividirPorTamano(Path nwavEntrada, String nwavContrasena, long nwavBytesMaximos,
                                           Path nwavCarpeta, NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) {
        if (nwavBytesMaximos < 10_000) {
            throw new NwavEditorException("El tamaño máximo por archivo es demasiado pequeño.");
        }
        return nwavDividir(nwavEntrada, nwavContrasena, nwavCarpeta, nwavAvance, nwavCancelacion, nwavTotal -> {
            try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
                List<NwavRango> nwavRangos = new ArrayList<>();
                int nwavInicio = 0;
                long nwavAcumulado = 0;
                for (int nwavIndice = 0; nwavIndice < nwavTotal; nwavIndice++) {
                    nwavCancelacion.nwavVerificar();
                    nwavAvance.nwavInformar(nwavIndice * 0.5, nwavTotal, "Midiendo la página " + (nwavIndice + 1) + "…");
                    long nwavPagina = nwavMedirPagina(nwavPd, nwavIndice);
                    if (nwavIndice > nwavInicio && nwavAcumulado + nwavPagina > nwavBytesMaximos) {
                        nwavRangos.add(new NwavRango(nwavInicio, nwavIndice - 1));
                        nwavInicio = nwavIndice;
                        nwavAcumulado = 0;
                    }
                    nwavAcumulado += nwavPagina;
                }
                nwavRangos.add(new NwavRango(nwavInicio, nwavTotal - 1));
                return nwavRangos;
            } catch (IOException nwavError) {
                throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
            }
        });
    }

    private List<Path> nwavDividir(Path nwavEntrada, String nwavContrasena, Path nwavCarpeta, NwavAvance nwavAvance,
                                   NwavCancelacion nwavCancelacion, Function<Integer, List<NwavRango>> nwavPlan) {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            List<NwavRango> nwavRangos = nwavPlan.apply(nwavPd.getNumberOfPages());
            List<Path> nwavCreados = new ArrayList<>();
            int nwavCifras = Math.max(2, String.valueOf(nwavRangos.size()).length());
            for (int nwavParte = 0; nwavParte < nwavRangos.size(); nwavParte++) {
                nwavCancelacion.nwavVerificar();
                NwavRango nwavRango = nwavRangos.get(nwavParte);
                nwavAvance.nwavInformar(nwavParte, nwavRangos.size(), "Creando la parte " + (nwavParte + 1) + "…");
                String nwavSufijo = String.format(java.util.Locale.ROOT, "parte%0" + nwavCifras + "d", nwavParte + 1);
                Path nwavDestino = NwavEscrituraSegura.nwavRutaSalida(nwavEntrada, nwavCarpeta, nwavSufijo, "pdf");
                nwavCreados.add(nwavEscribirPaginas(nwavPd, nwavRango.nwavIndices(), nwavDestino));
            }
            return nwavCreados;
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    public Path nwavEliminarPaginas(Path nwavEntrada, String nwavContrasena, String nwavRangos, Path nwavDestino) {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            int nwavTotal = nwavPd.getNumberOfPages();
            List<Integer> nwavQuitar = NwavRangosPaginas.nwavIndices(nwavRangos, nwavTotal);
            if (nwavQuitar.size() >= nwavTotal) {
                throw new NwavEditorException("No se pueden eliminar todas las páginas.");
            }
            List<Integer> nwavQuedan = IntStream.range(0, nwavTotal).filter(nwavIndice -> !nwavQuitar.contains(nwavIndice))
                    .boxed().toList();
            return nwavEscribirPaginas(nwavPd, nwavQuedan, nwavDestino);
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    public List<Path> nwavExtraerPaginas(Path nwavEntrada, String nwavContrasena, String nwavRangos,
                                         boolean nwavUnoPorPagina, Path nwavCarpeta, NwavCancelacion nwavCancelacion) {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            List<Integer> nwavIndices = NwavRangosPaginas.nwavIndices(nwavRangos, nwavPd.getNumberOfPages());
            if (!nwavUnoPorPagina) {
                return List.of(nwavEscribirPaginas(nwavPd, nwavIndices,
                        NwavEscrituraSegura.nwavRutaSalida(nwavEntrada, nwavCarpeta,
                                "pags_" + NwavRangosPaginas.nwavDescribir(nwavIndices).replace(", ", "_"), "pdf")));
            }
            List<Path> nwavCreados = new ArrayList<>();
            for (int nwavIndice : nwavIndices) {
                nwavCancelacion.nwavVerificar();
                nwavCreados.add(nwavEscribirPaginas(nwavPd, List.of(nwavIndice),
                        NwavEscrituraSegura.nwavRutaSalida(nwavEntrada, nwavCarpeta, "pag" + (nwavIndice + 1), "pdf")));
            }
            return nwavCreados;
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    public Path nwavOrdenar(Path nwavEntrada, String nwavContrasena, String nwavOrden, boolean nwavInvertir,
                            Path nwavDestino) {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            int nwavTotal = nwavPd.getNumberOfPages();
            List<Integer> nwavIndices;
            if (nwavInvertir) {
                nwavIndices = IntStream.range(0, nwavTotal).map(nwavIndice -> nwavTotal - 1 - nwavIndice).boxed().toList();
            } else {
                nwavIndices = new ArrayList<>();
                for (NwavRango nwavRango : NwavRangosPaginas.nwavInterpretar(nwavOrden, nwavTotal)) {
                    nwavIndices.addAll(nwavRango.nwavIndices());
                }
            }
            return nwavEscribirPaginas(nwavPd, nwavIndices, nwavDestino);
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    public Path nwavRotar(Path nwavEntrada, String nwavContrasena, int nwavGrados, String nwavRangos, Path nwavDestino) {
        if (nwavGrados % 90 != 0) {
            throw new NwavEditorException("La rotación debe ser de 90, 180 o 270 grados.");
        }
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            for (int nwavIndice : nwavPaginasElegidas(nwavPd, nwavRangos)) {
                PDPage nwavPagina = nwavPd.getPage(nwavIndice);
                nwavPagina.setRotation((((nwavPagina.getRotation() + nwavGrados) % 360) + 360) % 360);
            }
            return nwavGuardarConProteccion(nwavPd, nwavContrasena, nwavDestino);
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    public Path nwavRecortarMargenes(Path nwavEntrada, String nwavContrasena, double nwavArribaMm, double nwavDerechaMm,
                                     double nwavAbajoMm, double nwavIzquierdaMm, String nwavRangos, Path nwavDestino) {
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasena)) {
            for (int nwavIndice : nwavPaginasElegidas(nwavPd, nwavRangos)) {
                PDPage nwavPagina = nwavPd.getPage(nwavIndice);
                NwavGeometriaPagina nwavGeometria = NwavGeometriaPagina.nwavDe(nwavPagina);
                double nwavIzquierda = nwavIzquierdaMm * NWAV_PUNTOS_POR_MM;
                double nwavArriba = nwavArribaMm * NWAV_PUNTOS_POR_MM;
                double nwavAncho = nwavGeometria.nwavAnchoVisual() - nwavIzquierda - nwavDerechaMm * NWAV_PUNTOS_POR_MM;
                double nwavAlto = nwavGeometria.nwavAltoVisual() - nwavArriba - nwavAbajoMm * NWAV_PUNTOS_POR_MM;
                if (nwavAncho < 36 || nwavAlto < 36) {
                    throw new NwavEditorException("Los márgenes son demasiado grandes para la página " + (nwavIndice + 1) + ".");
                }
                nwavPagina.setCropBox(nwavGeometria.nwavRectanguloPdf(
                        new NwavRectangulo(nwavIzquierda, nwavArriba, nwavAncho, nwavAlto)));
            }
            return nwavGuardarConProteccion(nwavPd, nwavContrasena, nwavDestino);
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    static PDRectangle nwavCajaRecorte(PDPage nwavPagina, NwavRectangulo nwavZonaVisual) {
        NwavGeometriaPagina nwavGeometria = NwavGeometriaPagina.nwavDe(nwavPagina);
        double nwavX = Math.max(0, nwavZonaVisual.nwavX());
        double nwavY = Math.max(0, nwavZonaVisual.nwavY());
        double nwavAncho = Math.min(nwavGeometria.nwavAnchoVisual() - nwavX, nwavZonaVisual.nwavAncho());
        double nwavAlto = Math.min(nwavGeometria.nwavAltoVisual() - nwavY, nwavZonaVisual.nwavAlto());
        if (nwavAncho < 18 || nwavAlto < 18) {
            throw new NwavEditorException("El área de recorte es demasiado pequeña.");
        }
        return nwavGeometria.nwavRectanguloPdf(new NwavRectangulo(nwavX, nwavY, nwavAncho, nwavAlto));
    }

    private static List<Integer> nwavPaginasElegidas(PDDocument nwavPd, String nwavRangos) {
        int nwavTotal = nwavPd.getNumberOfPages();
        return nwavRangos == null || nwavRangos.isBlank()
                ? IntStream.range(0, nwavTotal).boxed().toList()
                : NwavRangosPaginas.nwavIndices(nwavRangos, nwavTotal);
    }

    static Path nwavEscribirPaginas(PDDocument nwavOrigen, List<Integer> nwavIndices, Path nwavDestino) throws IOException {
        if (nwavIndices.isEmpty()) {
            throw new NwavEditorException("No hay páginas para escribir.");
        }
        try (PDDocument nwavNuevo = new PDDocument()) {
            for (int nwavIndice : nwavIndices) {
                PDPage nwavPagina = nwavOrigen.getPage(nwavIndice);
                NwavPaginaService.nwavMaterializar(nwavPagina);
                nwavNuevo.importPage(nwavPagina);
            }
            nwavNuevo.getDocumentInformation().setProducer("NWAV Editor PDF");
            return NwavEscrituraSegura.nwavGuardarPdf(nwavNuevo, nwavDestino);
        }
    }

    private static Path nwavGuardarConProteccion(PDDocument nwavPd, String nwavContrasena, Path nwavDestino) {
        NwavEscrituraSegura.nwavMantenerProteccion(nwavPd, nwavContrasena);
        return NwavEscrituraSegura.nwavGuardarPdf(nwavPd, nwavDestino);
    }

    private static long nwavMedirPagina(PDDocument nwavOrigen, int nwavIndice) throws IOException {
        try (PDDocument nwavSuelta = new PDDocument(); ByteArrayOutputStream nwavSalida = new ByteArrayOutputStream()) {
            PDPage nwavPagina = nwavOrigen.getPage(nwavIndice);
            NwavPaginaService.nwavMaterializar(nwavPagina);
            nwavSuelta.importPage(nwavPagina);
            nwavSuelta.save(nwavSalida, CompressParameters.DEFAULT_COMPRESSION);
            return nwavSalida.size();
        }
    }

    private static void nwavCerrar(PDDocument nwavPd) {
        try {
            nwavPd.close();
        } catch (IOException nwavError) {
        }
    }

    public static boolean nwavEsFaltaDeContrasena(Throwable nwavError) {
        return nwavError instanceof NwavContrasenaRequeridaException;
    }
}
