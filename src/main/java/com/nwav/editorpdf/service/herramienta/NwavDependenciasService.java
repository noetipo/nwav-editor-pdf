package com.nwav.editorpdf.service.herramienta;

import com.nwav.editorpdf.service.NwavConfiguracionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Detecta las dependencias externas opcionales: el motor de OCR (Tesseract) y LibreOffice.
 * Si faltan, solo se deshabilitan las herramientas que las usan; el resto de la app
 * funciona igual. Las rutas se pueden indicar en Preferencias.
 */
@Service
@Slf4j
public class NwavDependenciasService {

    public static final String NWAV_CLAVE_RUTA_TESSERACT = "nwav.ocr.ruta.libreria";
    public static final String NWAV_CLAVE_RUTA_TESSDATA = "nwav.ocr.ruta.idiomas";
    public static final String NWAV_CLAVE_RUTA_LIBREOFFICE = "nwav.office.ruta";
    /** Idiomas incluidos en la aplicacion (modelos rapidos, licencia Apache 2.0). */
    public static final List<String> NWAV_IDIOMAS_INCLUIDOS = List.of("spa", "eng");

    /** Estado de una dependencia. */
    public record NwavEstado(boolean nwavDisponible, String nwavDetalle, String nwavInstrucciones) {
    }

    private static final List<String> NWAV_LIBRERIAS_TESSERACT = List.of(
            "/opt/homebrew/lib", "/usr/local/lib", "/usr/lib", "/usr/lib/x86_64-linux-gnu", "/usr/lib/aarch64-linux-gnu");
    private static final List<String> NWAV_IDIOMAS_SISTEMA = List.of(
            "/opt/homebrew/share/tessdata", "/usr/local/share/tessdata", "/usr/share/tesseract-ocr/5/tessdata",
            "/usr/share/tesseract-ocr/4.00/tessdata", "/usr/share/tessdata", "C:\\Program Files\\Tesseract-OCR\\tessdata");
    private static final List<String> NWAV_RUTAS_LIBREOFFICE = List.of(
            "/Applications/LibreOffice.app/Contents/MacOS/soffice", "/usr/bin/soffice", "/usr/bin/libreoffice",
            "/usr/local/bin/soffice", "/opt/libreoffice/program/soffice", "/snap/bin/libreoffice",
            "C:\\Program Files\\LibreOffice\\program\\soffice.exe",
            "C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe");

    private final NwavConfiguracionService nwavConfiguracion;
    private final Path nwavCarpetaIdiomas;
    private volatile NwavEstado nwavTesseract;
    private volatile NwavEstado nwavLibreOffice;

    public NwavDependenciasService(NwavConfiguracionService nwavConfiguracion,
                                   @Value("${nwav.datos.carpeta}") String nwavCarpetaDatos) {
        this.nwavConfiguracion = nwavConfiguracion;
        this.nwavCarpetaIdiomas = Path.of(nwavCarpetaDatos).toAbsolutePath().resolve("tessdata");
        // JNA busca la libreria nativa en jna.library.path: debe fijarse antes del primer uso.
        Set<String> nwavRutas = new LinkedHashSet<>();
        nwavConfiguracion.nwavObtener(NWAV_CLAVE_RUTA_TESSERACT).ifPresent(nwavRutas::add);
        nwavRutas.addAll(NWAV_LIBRERIAS_TESSERACT);
        String nwavActual = System.getProperty("jna.library.path");
        if (nwavActual != null && !nwavActual.isBlank()) {
            nwavRutas.add(nwavActual);
        }
        System.setProperty("jna.library.path", String.join(java.io.File.pathSeparator, nwavRutas));
        // La comprobacion de Tesseract puede tardar: se hace en segundo plano al arrancar.
        Thread nwavComprobacion = new Thread(() -> {
            nwavTesseract();
            nwavLibreOffice();
        }, "nwav-dependencias");
        nwavComprobacion.setDaemon(true);
        nwavComprobacion.start();
    }

    /** {@code true} cuando ya se sabe si las dependencias estan (para no bloquear la interfaz). */
    public boolean nwavComprobado() {
        return nwavTesseract != null && nwavLibreOffice != null;
    }

    // ================================================================ Tesseract

    /** Estado del OCR (se comprueba una vez; {@link #nwavRefrescar()} lo repite). */
    public NwavEstado nwavTesseract() {
        NwavEstado nwavEstado = nwavTesseract;
        if (nwavEstado == null) {
            synchronized (this) {
                if (nwavTesseract == null) {
                    nwavTesseract = nwavComprobarTesseract();
                }
                nwavEstado = nwavTesseract;
            }
        }
        return nwavEstado;
    }

    private NwavEstado nwavComprobarTesseract() {
        String nwavInstrucciones = nwavInstruccionesTesseract();
        try {
            Path nwavDatos = nwavCarpetaIdiomasPreparada();
            net.sourceforge.tess4j.Tesseract nwavMotor = new net.sourceforge.tess4j.Tesseract();
            nwavMotor.setDatapath(nwavDatos.toString());
            nwavMotor.setLanguage("eng");
            BufferedImage nwavPrueba = new BufferedImage(120, 40, BufferedImage.TYPE_INT_RGB);
            Graphics2D nwavLienzo = nwavPrueba.createGraphics();
            nwavLienzo.setColor(Color.WHITE);
            nwavLienzo.fillRect(0, 0, 120, 40);
            nwavLienzo.setColor(Color.BLACK);
            nwavLienzo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            nwavLienzo.drawString("OK", 30, 30);
            nwavLienzo.dispose();
            nwavMotor.doOCR(nwavPrueba);
            return new NwavEstado(true, "Tesseract disponible · idiomas: " + String.join(", ", nwavIdiomasDisponibles()),
                    nwavInstrucciones);
        } catch (Throwable nwavError) {
            // UnsatisfiedLinkError / NoClassDefFoundError cuando falta la libreria nativa.
            nwavLog.info("OCR no disponible: {}", nwavError.toString());
            return new NwavEstado(false, "No se encontró el motor de OCR (Tesseract).", nwavInstrucciones);
        }
    }

    private static String nwavInstruccionesTesseract() {
        String nwavSistema = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (nwavSistema.contains("mac")) {
            return "Instala Tesseract con Homebrew: «brew install tesseract» y reinicia la aplicación. "
                    + "Si lo instalaste en otra ubicación, indica la carpeta de la librería en Preferencias.";
        }
        if (nwavSistema.contains("win")) {
            return "El motor de OCR viene incluido en Windows; si no funciona, instala Tesseract "
                    + "(github.com/UB-Mannheim/tesseract) y reinicia la aplicación.";
        }
        return "Instala Tesseract (p. ej. «sudo apt install tesseract-ocr») y reinicia la aplicación.";
    }

    /**
     * Carpeta de idiomas del OCR en los datos del usuario. Copia alli los idiomas incluidos
     * en la aplicacion (espanol e ingles) la primera vez.
     */
    public Path nwavCarpetaIdiomasPreparada() throws IOException {
        Files.createDirectories(nwavCarpetaIdiomas);
        for (String nwavIdioma : NWAV_IDIOMAS_INCLUIDOS) {
            Path nwavDestino = nwavCarpetaIdiomas.resolve(nwavIdioma + ".traineddata");
            if (!Files.exists(nwavDestino)) {
                try (InputStream nwavOrigen = getClass().getResourceAsStream("/tessdata/" + nwavIdioma + ".traineddata")) {
                    if (nwavOrigen != null) {
                        Files.copy(nwavOrigen, nwavDestino, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
        return nwavCarpetaIdiomas;
    }

    /** Carpetas donde buscar idiomas adicionales (la de Preferencias y las del sistema). */
    public List<Path> nwavCarpetasIdiomasSistema() {
        List<Path> nwavCarpetas = new ArrayList<>();
        nwavConfiguracion.nwavObtener(NWAV_CLAVE_RUTA_TESSDATA).map(Path::of).ifPresent(nwavCarpetas::add);
        NWAV_IDIOMAS_SISTEMA.stream().map(Path::of).forEach(nwavCarpetas::add);
        return nwavCarpetas.stream().filter(Files::isDirectory).toList();
    }

    /** Codigos de idioma disponibles (incluidos + sistema), sin "osd" ni "equ". */
    public List<String> nwavIdiomasDisponibles() {
        Set<String> nwavCodigos = new java.util.TreeSet<>(NWAV_IDIOMAS_INCLUIDOS);
        List<Path> nwavCarpetas = new ArrayList<>(nwavCarpetasIdiomasSistema());
        nwavCarpetas.add(nwavCarpetaIdiomas);
        for (Path nwavCarpeta : nwavCarpetas) {
            try (Stream<Path> nwavArchivos = Files.list(nwavCarpeta)) {
                nwavArchivos.map(nwavArchivo -> nwavArchivo.getFileName().toString())
                        .filter(nwavNombre -> nwavNombre.endsWith(".traineddata"))
                        .map(nwavNombre -> nwavNombre.substring(0, nwavNombre.length() - ".traineddata".length()))
                        .filter(nwavCodigo -> !nwavCodigo.equals("osd") && !nwavCodigo.equals("equ"))
                        .forEach(nwavCodigos::add);
            } catch (IOException nwavError) {
                nwavLog.debug("No se pudo listar {}", nwavCarpeta);
            }
        }
        return List.copyOf(nwavCodigos);
    }

    /**
     * Garantiza que los idiomas elegidos esten en la carpeta de datos (copia los que solo
     * existan en el sistema) y la devuelve para usarla como datapath.
     */
    public Path nwavPrepararIdiomas(List<String> nwavIdiomas) throws IOException {
        Path nwavCarpeta = nwavCarpetaIdiomasPreparada();
        for (String nwavIdioma : nwavIdiomas) {
            Path nwavDestino = nwavCarpeta.resolve(nwavIdioma + ".traineddata");
            if (Files.exists(nwavDestino)) {
                continue;
            }
            Optional<Path> nwavOrigen = nwavCarpetasIdiomasSistema().stream()
                    .map(nwavSistema -> nwavSistema.resolve(nwavIdioma + ".traineddata"))
                    .filter(Files::isRegularFile).findFirst();
            if (nwavOrigen.isEmpty()) {
                throw new com.nwav.editorpdf.exception.NwavEditorException("No están instalados los datos del idioma «"
                        + nwavIdioma + "». Descárgalos (tessdata) y elige su carpeta en Preferencias.");
            }
            Files.copy(nwavOrigen.get(), nwavDestino, StandardCopyOption.REPLACE_EXISTING);
        }
        return nwavCarpeta;
    }

    // =============================================================== LibreOffice

    public NwavEstado nwavLibreOffice() {
        NwavEstado nwavEstado = nwavLibreOffice;
        if (nwavEstado == null) {
            Optional<Path> nwavRuta = nwavRutaLibreOffice();
            nwavEstado = nwavRuta.isPresent()
                    ? new NwavEstado(true, "LibreOffice encontrado en " + nwavRuta.get(), "")
                    : new NwavEstado(false, "No se encontró LibreOffice.",
                    "Instala LibreOffice (gratuito, libreoffice.org) o indica la ruta de «soffice» en Preferencias.");
            nwavLibreOffice = nwavEstado;
        }
        return nwavEstado;
    }

    /** Ejecutable "soffice": el de Preferencias o el de una ubicacion habitual. */
    public Optional<Path> nwavRutaLibreOffice() {
        List<String> nwavCandidatos = new ArrayList<>();
        nwavConfiguracion.nwavObtener(NWAV_CLAVE_RUTA_LIBREOFFICE).ifPresent(nwavCandidatos::add);
        nwavCandidatos.addAll(NWAV_RUTAS_LIBREOFFICE);
        return nwavCandidatos.stream().map(Path::of).filter(Files::isRegularFile).filter(Files::isExecutable).findFirst();
    }

    /** Carpeta raiz de LibreOffice (la que espera JODConverter). */
    public Optional<Path> nwavCarpetaLibreOffice() {
        return nwavRutaLibreOffice().map(nwavSoffice -> {
            Path nwavPadre = nwavSoffice.getParent();
            // macOS: .../LibreOffice.app/Contents/MacOS/soffice -> .../LibreOffice.app/Contents
            // Windows/Linux: .../program/soffice -> carpeta de instalacion
            return nwavPadre != null && nwavPadre.getParent() != null ? nwavPadre.getParent() : nwavPadre;
        });
    }

    /** Vuelve a comprobar (tras cambiar rutas en Preferencias). La libreria nativa exige reiniciar. */
    public void nwavRefrescar() {
        nwavLibreOffice = null;
        if (nwavTesseract != null && !nwavTesseract.nwavDisponible()) {
            nwavTesseract = null;
        }
    }
}
