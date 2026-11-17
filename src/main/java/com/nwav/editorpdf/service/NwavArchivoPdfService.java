package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavArchivoEnUsoException;
import com.nwav.editorpdf.exception.NwavContrasenaRequeridaException;
import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.exception.NwavSinPermisoException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.NwavRangosPaginas.NwavRango;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class NwavArchivoPdfService {
    private final NwavPdfDocumentoService nwavDocumentos;
    private final NwavHistorialService nwavHistorial;

    public NwavDocumentoPdf nwavGuardarComo(NwavDocumentoPdf nwavDocumento, Path nwavDestino) {
        Path nwavFinal = nwavDestino.toAbsolutePath().normalize();
        boolean nwavSobrescribe = nwavFinal.equals(nwavDocumento.getNwavRuta());
        Path nwavTemporal = null;
        try {
            nwavValidarEscritura(nwavFinal);
            synchronized (nwavDocumento) {
                nwavDocumento.nwavVerificarAbierto();
                nwavAplicarProteccion(nwavDocumento);
                nwavTemporal = nwavTemporalJunto(nwavFinal);
                nwavDocumento.nwavPdDocument().save(nwavTemporal.toFile());
            }
            try {
                nwavMoverAlDestino(nwavTemporal, nwavFinal);
            } catch (FileSystemException nwavBloqueo) {
                if (!nwavSobrescribe) {
                    throw nwavBloqueo;
                }
                nwavDocumento.close();
                try {
                    nwavMoverAlDestino(nwavTemporal, nwavFinal);
                } catch (IOException nwavError) {
                    throw new NwavEditorException("No se pudo reemplazar «" + nwavFinal.getFileName()
                            + "». Tus cambios se guardaron en «" + nwavTemporal + "».", nwavError);
                }
            }
            nwavTemporal = null;
            String nwavContrasena = nwavContrasenaTrasGuardar(nwavDocumento);
            nwavDocumento.close();
            NwavDocumentoPdf nwavGuardado = nwavDocumentos.nwavAbrir(nwavFinal, nwavContrasena);
            NwavCambioProteccion nwavCambio = nwavDocumento.getNwavProteccionPendiente();
            nwavHistorial.nwavRegistrarExito(NwavTipoOperacion.NWAV_GUARDAR, nwavDocumento.getNwavRuta(), nwavFinal,
                    (nwavSobrescribe ? "Se sobrescribió el archivo original" : "Guardado como archivo nuevo")
                            + (nwavCambio == null ? "" : nwavCambio.nwavQuitar() ? "; protección quitada"
                            : "; protegido con contraseña"));
            return nwavGuardado;
        } catch (IOException | RuntimeException nwavError) {
            nwavBorrarSilencioso(nwavTemporal);
            nwavHistorial.nwavRegistrarError(NwavTipoOperacion.NWAV_GUARDAR, nwavDocumento.getNwavRuta(),
                    nwavFinal, nwavError);
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavFinal.getFileName()));
        }
    }

    private static void nwavAplicarProteccion(NwavDocumentoPdf nwavDocumento) throws IOException {
        NwavCambioProteccion nwavCambio = nwavDocumento.getNwavProteccionPendiente();
        PDDocument nwavPd = nwavDocumento.nwavPdDocument();
        if (nwavCambio == null) {
            nwavMantenerProteccion(nwavDocumento);
        } else if (nwavCambio.nwavQuitar()) {
            nwavPd.setAllSecurityToBeRemoved(true);
        } else {
            nwavPd.setAllSecurityToBeRemoved(false);
            nwavPd.protect(NwavSeguridadService.nwavPolitica(nwavCambio.nwavOpciones()));
        }
    }

    private static String nwavContrasenaTrasGuardar(NwavDocumentoPdf nwavDocumento) {
        NwavCambioProteccion nwavCambio = nwavDocumento.getNwavProteccionPendiente();
        if (nwavCambio == null) {
            return nwavDocumento.nwavContrasena();
        }
        return nwavCambio.nwavQuitar() ? null : nwavCambio.nwavOpciones().nwavUsuario();
    }

    private static void nwavMantenerProteccion(NwavDocumentoPdf nwavDocumento) throws IOException {
        PDDocument nwavPd = nwavDocumento.nwavPdDocument();
        if (!nwavPd.isEncrypted()) {
            return;
        }
        AccessPermission nwavActuales = nwavPd.getCurrentAccessPermission();
        String nwavUsuario = nwavDocumento.nwavContrasena() == null ? "" : nwavDocumento.nwavContrasena();
        String nwavPropietario = nwavActuales.isOwnerPermission() && !nwavUsuario.isEmpty()
                ? nwavUsuario
                : java.util.UUID.randomUUID().toString();
        StandardProtectionPolicy nwavPolitica = new StandardProtectionPolicy(nwavPropietario, nwavUsuario,
                new AccessPermission(nwavActuales.getPermissionBytes()));
        int nwavBits = nwavPd.getEncryption() == null ? 128 : nwavPd.getEncryption().getLength();
        nwavPolitica.setEncryptionKeyLength(nwavBits >= 256 ? 256 : 128);
        nwavPd.protect(nwavPolitica);
    }

    public Path nwavExtraer(NwavDocumentoPdf nwavDocumento, List<Integer> nwavIndices, Path nwavDestino) {
        Path nwavFinal = nwavDestino.toAbsolutePath().normalize();
        try {
            nwavEvitarOriginal(nwavDocumento, nwavFinal);
            nwavEscribirPaginas(nwavDocumento, nwavIndices, nwavFinal);
            nwavHistorial.nwavRegistrarExito(NwavTipoOperacion.NWAV_EXTRAER, nwavDocumento.getNwavRuta(), nwavFinal,
                    "Páginas " + NwavRangosPaginas.nwavDescribir(nwavIndices));
            return nwavFinal;
        } catch (RuntimeException nwavError) {
            nwavHistorial.nwavRegistrarError(NwavTipoOperacion.NWAV_EXTRAER, nwavDocumento.getNwavRuta(),
                    nwavFinal, nwavError);
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavFinal.getFileName()));
        }
    }

    public List<Path> nwavDividir(NwavDocumentoPdf nwavDocumento, List<NwavRango> nwavRangos, Path nwavCarpeta,
                                  String nwavBase, NwavAvance nwavAvance) {
        Path nwavDirectorio = nwavCarpeta.toAbsolutePath().normalize();
        List<Path> nwavCreados = new ArrayList<>();
        try {
            if (nwavRangos.isEmpty()) {
                throw new NwavEditorException("No hay rangos para dividir.");
            }
            if (!Files.isDirectory(nwavDirectorio)) {
                throw new NwavEditorException("La carpeta «" + nwavDirectorio + "» no existe.");
            }
            String nwavPrefijo = nwavLimpiarNombre(nwavBase == null || nwavBase.isBlank()
                    ? nwavSinExtension(nwavDocumento.getNwavNombre()) : nwavBase);
            int nwavCifras = Math.max(2, String.valueOf(nwavRangos.size()).length());
            for (int nwavParte = 0; nwavParte < nwavRangos.size(); nwavParte++) {
                NwavRango nwavRango = nwavRangos.get(nwavParte);
                nwavAvance.nwavInformar(nwavParte, nwavRangos.size(),
                        "Creando parte " + (nwavParte + 1) + " de " + nwavRangos.size() + "…");
                String nwavNombre = String.format(Locale.ROOT, "%s - parte %0" + nwavCifras + "d (pág. %s).pdf",
                        nwavPrefijo, nwavParte + 1, nwavRango.nwavDescripcion());
                Path nwavArchivo = nwavSinColision(nwavDirectorio.resolve(nwavNombre));
                nwavEscribirPaginas(nwavDocumento, nwavRango.nwavIndices(), nwavArchivo);
                nwavCreados.add(nwavArchivo);
            }
            nwavAvance.nwavInformar(nwavRangos.size(), nwavRangos.size(), "Listo");
            nwavHistorial.nwavRegistrarExito(NwavTipoOperacion.NWAV_DIVIDIR, nwavDocumento.getNwavRuta(),
                    nwavDirectorio, nwavCreados.size() + " archivos: " + String.join(", ",
                            nwavRangos.stream().map(NwavRango::nwavDescripcion).toList()));
            return nwavCreados;
        } catch (RuntimeException nwavError) {
            nwavHistorial.nwavRegistrarError(NwavTipoOperacion.NWAV_DIVIDIR, nwavDocumento.getNwavRuta(),
                    nwavDirectorio, nwavError);
            throw NwavTraductorErrores.nwavTraducir(nwavError, nwavDocumento.getNwavNombre());
        }
    }

    public Path nwavUnir(List<Path> nwavOrigenes, Path nwavDestino, NwavAvance nwavAvance) {
        Path nwavFinal = nwavDestino.toAbsolutePath().normalize();
        List<PDDocument> nwavAbiertos = new ArrayList<>();
        Path nwavTemporal = null;
        try {
            if (nwavOrigenes == null || nwavOrigenes.size() < 2) {
                throw new NwavEditorException("Elige al menos dos PDF para unir.");
            }
            nwavValidarEscritura(nwavFinal);
            PDFMergerUtility nwavUnion = new PDFMergerUtility();
            try (PDDocument nwavResultado = new PDDocument()) {
                for (int nwavIndice = 0; nwavIndice < nwavOrigenes.size(); nwavIndice++) {
                    Path nwavOrigen = nwavOrigenes.get(nwavIndice);
                    nwavAvance.nwavInformar(nwavIndice, nwavOrigenes.size() + 1,
                            "Añadiendo «" + nwavOrigen.getFileName() + "»…");
                    PDDocument nwavFuente = nwavCargarParaUnir(nwavOrigen);
                    nwavAbiertos.add(nwavFuente);
                    nwavUnion.appendDocument(nwavResultado, nwavFuente);
                }
                nwavAvance.nwavInformar(nwavOrigenes.size(), nwavOrigenes.size() + 1, "Guardando…");
                nwavTemporal = nwavTemporalJunto(nwavFinal);
                nwavResultado.save(nwavTemporal.toFile());
            }
            nwavCerrarTodos(nwavAbiertos);
            nwavMoverAlDestino(nwavTemporal, nwavFinal);
            nwavTemporal = null;
            nwavAvance.nwavInformar(1, 1, "Listo");
            nwavHistorial.nwavRegistrarExito(NwavTipoOperacion.NWAV_UNIR, nwavOrigenes, nwavFinal,
                    nwavOrigenes.size() + " archivos unidos");
            return nwavFinal;
        } catch (IOException | RuntimeException nwavError) {
            nwavBorrarSilencioso(nwavTemporal);
            nwavHistorial.nwavRegistrarError(NwavTipoOperacion.NWAV_UNIR, nwavOrigenes, nwavFinal, nwavError);
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavFinal.getFileName()));
        } finally {
            nwavCerrarTodos(nwavAbiertos);
        }
    }

    private PDDocument nwavCargarParaUnir(Path nwavOrigen) {
        try {
            return nwavDocumentos.nwavCargarDocumento(nwavOrigen, null);
        } catch (NwavContrasenaRequeridaException nwavError) {
            throw new NwavEditorException("«" + nwavOrigen.getFileName()
                    + "» está protegido con contraseña. Ábrelo y guarda una copia sin protección antes de unirlo.",
                    nwavError);
        }
    }

    private void nwavEscribirPaginas(NwavDocumentoPdf nwavDocumento, List<Integer> nwavIndices, Path nwavFinal) {
        if (nwavIndices == null || nwavIndices.isEmpty()) {
            throw new NwavEditorException("Selecciona al menos una página.");
        }
        Path nwavTemporal = null;
        try {
            nwavValidarEscritura(nwavFinal);
            synchronized (nwavDocumento) {
                nwavDocumento.nwavVerificarAbierto();
                PDDocument nwavOrigen = nwavDocumento.nwavPdDocument();
                try (PDDocument nwavNuevo = new PDDocument()) {
                    for (int nwavIndice : nwavIndices) {
                        if (nwavIndice < 0 || nwavIndice >= nwavOrigen.getNumberOfPages()) {
                            throw new NwavEditorException("La página " + (nwavIndice + 1) + " no existe.");
                        }
                        PDPage nwavPagina = nwavOrigen.getPage(nwavIndice);
                        NwavPaginaService.nwavMaterializar(nwavPagina);
                        nwavNuevo.importPage(nwavPagina);
                    }
                    nwavTemporal = nwavTemporalJunto(nwavFinal);
                    nwavNuevo.save(nwavTemporal.toFile());
                }
            }
            nwavMoverAlDestino(nwavTemporal, nwavFinal);
        } catch (IOException nwavError) {
            nwavBorrarSilencioso(nwavTemporal);
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavFinal.getFileName()));
        } catch (RuntimeException nwavError) {
            nwavBorrarSilencioso(nwavTemporal);
            throw nwavError;
        }
    }

    private static void nwavEvitarOriginal(NwavDocumentoPdf nwavDocumento, Path nwavFinal) {
        if (nwavFinal.equals(nwavDocumento.getNwavRuta())) {
            throw new NwavEditorException("Elige otro nombre: no se puede extraer sobre el documento abierto.");
        }
    }

    static void nwavValidarEscritura(Path nwavFinal) {
        Path nwavCarpeta = nwavFinal.getParent();
        String nwavNombre = String.valueOf(nwavFinal.getFileName());
        if (nwavCarpeta == null || !Files.isDirectory(nwavCarpeta)) {
            throw new NwavEditorException("La carpeta de destino de «" + nwavNombre + "» no existe.");
        }
        if (!Files.isWritable(nwavCarpeta)) {
            throw new NwavSinPermisoException(nwavCarpeta.toString());
        }
        if (Files.exists(nwavFinal) && !Files.isWritable(nwavFinal)) {
            throw new NwavSinPermisoException(nwavNombre);
        }
    }

    private static Path nwavTemporalJunto(Path nwavFinal) throws IOException {
        return Files.createTempFile(nwavFinal.getParent(), ".nwav-guardando-", ".pdf");
    }

    private static void nwavMoverAlDestino(Path nwavTemporal, Path nwavFinal) throws IOException {
        try {
            Files.move(nwavTemporal, nwavFinal, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException nwavError) {
            Files.move(nwavTemporal, nwavFinal, StandardCopyOption.REPLACE_EXISTING);
        } catch (FileSystemException nwavError) {
            if (NwavTraductorErrores.nwavEsArchivoEnUso(nwavError.getReason())) {
                throw new NwavArchivoEnUsoException(String.valueOf(nwavFinal.getFileName()), nwavError);
            }
            throw nwavError;
        }
    }

    static Path nwavSinColision(Path nwavRuta) {
        if (!Files.exists(nwavRuta)) {
            return nwavRuta;
        }
        String nwavBase = nwavSinExtension(nwavRuta.getFileName().toString());
        for (int nwavNumero = 2; ; nwavNumero++) {
            Path nwavAlternativa = nwavRuta.resolveSibling(nwavBase + " (" + nwavNumero + ").pdf");
            if (!Files.exists(nwavAlternativa)) {
                return nwavAlternativa;
            }
        }
    }

    public static String nwavSinExtension(String nwavNombre) {
        return nwavNombre.toLowerCase(Locale.ROOT).endsWith(".pdf")
                ? nwavNombre.substring(0, nwavNombre.length() - 4) : nwavNombre;
    }

    private static String nwavLimpiarNombre(String nwavNombre) {
        return nwavNombre.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private static void nwavBorrarSilencioso(Path nwavRuta) {
        if (nwavRuta == null) {
            return;
        }
        try {
            Files.deleteIfExists(nwavRuta);
        } catch (IOException nwavError) {
            nwavLog.debug("No se pudo borrar el temporal {}", nwavRuta, nwavError);
        }
    }

    private static void nwavCerrarTodos(List<PDDocument> nwavDocumentosAbiertos) {
        for (PDDocument nwavAbierto : nwavDocumentosAbiertos) {
            try {
                nwavAbierto.close();
            } catch (IOException nwavError) {
                nwavLog.debug("Error al cerrar un origen de la union", nwavError);
            }
        }
        nwavDocumentosAbiertos.clear();
    }
}
