package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class NwavConversionAPdfService {
    public static final List<String> NWAV_EXTENSIONES_IMAGEN = List.of("jpg", "jpeg", "png", "bmp", "gif", "tif", "tiff");

    public enum NwavTamanoHoja { NWAV_AJUSTAR, NWAV_A4, NWAV_CARTA }

    public enum NwavOrientacion { NWAV_AUTO, NWAV_VERTICAL, NWAV_HORIZONTAL }

    public record NwavOpcionesImagenes(NwavTamanoHoja nwavTamano, NwavOrientacion nwavOrientacion, double nwavMargenMm,
                                       int nwavPorPagina) {
        public NwavOpcionesImagenes {
            if (nwavPorPagina != 1 && nwavPorPagina != 2 && nwavPorPagina != 4) {
                throw new NwavEditorException("Se admiten 1, 2 o 4 imágenes por página.");
            }
            nwavMargenMm = Math.max(0, Math.min(60, nwavMargenMm));
        }
    }

    public Path nwavImagenesAPdf(List<Path> nwavImagenes, NwavOpcionesImagenes nwavOpciones, Path nwavDestino,
                                 NwavAvance nwavAvance, NwavCancelacion nwavCancelacion) {
        if (nwavImagenes == null || nwavImagenes.isEmpty()) {
            throw new NwavEditorException("Elige al menos una imagen.");
        }
        try (PDDocument nwavPd = new PDDocument()) {
            int nwavPorPagina = nwavOpciones.nwavPorPagina();
            for (int nwavInicio = 0; nwavInicio < nwavImagenes.size(); nwavInicio += nwavPorPagina) {
                List<PDImageXObject> nwavGrupo = new ArrayList<>();
                for (int nwavIndice = nwavInicio; nwavIndice < Math.min(nwavImagenes.size(), nwavInicio + nwavPorPagina);
                     nwavIndice++) {
                    nwavCancelacion.nwavVerificar();
                    Path nwavImagen = nwavImagenes.get(nwavIndice);
                    nwavAvance.nwavInformar(nwavIndice, nwavImagenes.size() + 1, "Añadiendo «" + nwavImagen.getFileName() + "»…");
                    nwavGrupo.add(nwavCargarImagen(nwavPd, nwavImagen));
                }
                nwavColocar(nwavPd, nwavGrupo, nwavOpciones);
            }
            nwavPd.getDocumentInformation().setProducer("NWAV Editor PDF");
            nwavAvance.nwavInformar(nwavImagenes.size(), nwavImagenes.size() + 1, "Guardando…");
            return NwavEscrituraSegura.nwavGuardarPdf(nwavPd, nwavDestino);
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavDestino.getFileName()));
        }
    }

    private void nwavColocar(PDDocument nwavPd, List<PDImageXObject> nwavGrupo, NwavOpcionesImagenes nwavOpciones)
            throws IOException {
        PDImageXObject nwavPrimera = nwavGrupo.get(0);
        boolean nwavApaisadaImagen = nwavPrimera.getWidth() > nwavPrimera.getHeight();
        PDRectangle nwavHoja = switch (nwavOpciones.nwavTamano()) {
            case NWAV_A4 -> PDRectangle.A4;
            case NWAV_CARTA -> PDRectangle.LETTER;
            default -> {
                double nwavMargen = nwavOpciones.nwavMargenMm() * NwavOrganizarService.NWAV_PUNTOS_POR_MM * 2;
                float nwavAncho = (float) (nwavPrimera.getWidth() * 0.75 * (nwavGrupo.size() == 2 && !nwavApaisadaImagen ? 2 : 1)
                        * (nwavGrupo.size() == 4 ? 2 : 1) + nwavMargen);
                float nwavAlto = (float) (nwavPrimera.getHeight() * 0.75 * (nwavGrupo.size() == 2 && nwavApaisadaImagen ? 2 : 1)
                        * (nwavGrupo.size() == 4 ? 2 : 1) + nwavMargen);
                yield new PDRectangle(nwavAncho, nwavAlto);
            }
        };
        boolean nwavHorizontal = switch (nwavOpciones.nwavOrientacion()) {
            case NWAV_HORIZONTAL -> true;
            case NWAV_VERTICAL -> false;
            default -> nwavApaisadaImagen;
        };
        if (nwavOpciones.nwavTamano() != NwavTamanoHoja.NWAV_AJUSTAR
                && nwavHorizontal != (nwavHoja.getWidth() > nwavHoja.getHeight())) {
            nwavHoja = new PDRectangle(nwavHoja.getHeight(), nwavHoja.getWidth());
        }
        PDPage nwavPagina = new PDPage(nwavHoja);
        nwavPd.addPage(nwavPagina);

        double nwavMargen = nwavOpciones.nwavMargenMm() * NwavOrganizarService.NWAV_PUNTOS_POR_MM;
        double nwavAnchoUtil = nwavHoja.getWidth() - 2 * nwavMargen;
        double nwavAltoUtil = nwavHoja.getHeight() - 2 * nwavMargen;
        int nwavColumnas;
        int nwavFilas;
        if (nwavOpciones.nwavPorPagina() == 4) {
            nwavColumnas = 2;
            nwavFilas = 2;
        } else if (nwavOpciones.nwavPorPagina() == 2) {
            nwavColumnas = nwavHoja.getWidth() > nwavHoja.getHeight() ? 2 : 1;
            nwavFilas = nwavColumnas == 2 ? 1 : 2;
        } else {
            nwavColumnas = 1;
            nwavFilas = 1;
        }
        double nwavHueco = nwavOpciones.nwavPorPagina() > 1 ? Math.max(6, nwavMargen / 2) : 0;
        double nwavCeldaAncho = (nwavAnchoUtil - nwavHueco * (nwavColumnas - 1)) / nwavColumnas;
        double nwavCeldaAlto = (nwavAltoUtil - nwavHueco * (nwavFilas - 1)) / nwavFilas;

        try (PDPageContentStream nwavFlujo = new PDPageContentStream(nwavPd, nwavPagina)) {
            for (int nwavIndice = 0; nwavIndice < nwavGrupo.size(); nwavIndice++) {
                PDImageXObject nwavImagen = nwavGrupo.get(nwavIndice);
                int nwavColumna = nwavIndice % nwavColumnas;
                int nwavFila = nwavIndice / nwavColumnas;
                double nwavEscala = Math.min(nwavCeldaAncho / nwavImagen.getWidth(), nwavCeldaAlto / nwavImagen.getHeight());
                double nwavAncho = nwavImagen.getWidth() * nwavEscala;
                double nwavAlto = nwavImagen.getHeight() * nwavEscala;
                double nwavX = nwavMargen + nwavColumna * (nwavCeldaAncho + nwavHueco) + (nwavCeldaAncho - nwavAncho) / 2;
                double nwavYArriba = nwavMargen + nwavFila * (nwavCeldaAlto + nwavHueco) + (nwavCeldaAlto - nwavAlto) / 2;
                nwavFlujo.drawImage(nwavImagen, (float) nwavX, (float) (nwavHoja.getHeight() - nwavYArriba - nwavAlto),
                        (float) nwavAncho, (float) nwavAlto);
            }
        }
    }

    static PDImageXObject nwavCargarImagen(PDDocument nwavPd, Path nwavRuta) throws IOException {
        String nwavNombre = nwavRuta.getFileName().toString().toLowerCase(Locale.ROOT);
        boolean nwavJpeg = nwavNombre.endsWith(".jpg") || nwavNombre.endsWith(".jpeg");
        int nwavOrientacion = nwavJpeg ? nwavOrientacionExif(nwavRuta) : 1;
        if (nwavJpeg && nwavOrientacion == 1) {
            try (InputStream nwavEntrada = Files.newInputStream(nwavRuta)) {
                return JPEGFactory.createFromStream(nwavPd, nwavEntrada);
            } catch (IOException | IllegalArgumentException nwavError) {
            }
        }
        BufferedImage nwavImagen = ImageIO.read(nwavRuta.toFile());
        if (nwavImagen == null) {
            throw new NwavEditorException("No se pudo leer la imagen «" + nwavRuta.getFileName()
                    + "». Formatos admitidos: JPG, PNG, BMP, GIF y TIFF.");
        }
        nwavImagen = nwavAplicarOrientacion(nwavImagen, nwavOrientacion);
        return nwavJpeg ? JPEGFactory.createFromImage(nwavPd, nwavImagen, 0.92f)
                : LosslessFactory.createFromImage(nwavPd, nwavImagen);
    }

    static int nwavOrientacionExif(Path nwavRuta) {
        try (DataInputStream nwavEntrada = new DataInputStream(new java.io.BufferedInputStream(Files.newInputStream(nwavRuta)))) {
            if (nwavEntrada.readUnsignedShort() != 0xFFD8) {
                return 1;
            }
            while (true) {
                int nwavMarcador = nwavEntrada.readUnsignedShort();
                int nwavLongitud = nwavEntrada.readUnsignedShort() - 2;
                if (nwavMarcador == 0xFFE1) {
                    byte[] nwavDatos = nwavEntrada.readNBytes(nwavLongitud);
                    return nwavOrientacionDeApp1(nwavDatos);
                }
                if (nwavMarcador == 0xFFDA || (nwavMarcador & 0xFF00) != 0xFF00) {
                    return 1;
                }
                nwavEntrada.skipNBytes(nwavLongitud);
            }
        } catch (IOException | RuntimeException nwavError) {
            return 1;
        }
    }

    private static int nwavOrientacionDeApp1(byte[] nwavDatos) {
        if (nwavDatos.length < 14 || !new String(nwavDatos, 0, 4, StandardCharsets.ISO_8859_1).equals("Exif")) {
            return 1;
        }
        int nwavBase = 6;
        boolean nwavIntel = nwavDatos[nwavBase] == 'I';
        int nwavIfd = nwavBase + nwavLeer(nwavDatos, nwavBase + 4, 4, nwavIntel);
        int nwavEntradas = nwavLeer(nwavDatos, nwavIfd, 2, nwavIntel);
        for (int nwavIndice = 0; nwavIndice < nwavEntradas; nwavIndice++) {
            int nwavPos = nwavIfd + 2 + nwavIndice * 12;
            if (nwavPos + 12 > nwavDatos.length) {
                break;
            }
            if (nwavLeer(nwavDatos, nwavPos, 2, nwavIntel) == 0x0112) {
                int nwavValor = nwavLeer(nwavDatos, nwavPos + 8, 2, nwavIntel);
                return nwavValor >= 1 && nwavValor <= 8 ? nwavValor : 1;
            }
        }
        return 1;
    }

    private static int nwavLeer(byte[] nwavDatos, int nwavPos, int nwavBytes, boolean nwavIntel) {
        int nwavValor = 0;
        for (int nwavIndice = 0; nwavIndice < nwavBytes; nwavIndice++) {
            int nwavByte = nwavDatos[nwavPos + (nwavIntel ? nwavBytes - 1 - nwavIndice : nwavIndice)] & 0xFF;
            nwavValor = (nwavValor << 8) | nwavByte;
        }
        return nwavValor;
    }

    static BufferedImage nwavAplicarOrientacion(BufferedImage nwavImagen, int nwavOrientacion) {
        if (nwavOrientacion <= 1 || nwavOrientacion > 8) {
            return nwavImagen;
        }
        int nwavAncho = nwavImagen.getWidth();
        int nwavAlto = nwavImagen.getHeight();
        boolean nwavIntercambia = nwavOrientacion >= 5;
        AffineTransform nwavT = new AffineTransform();
        switch (nwavOrientacion) {
            case 2 -> { nwavT.translate(nwavAncho, 0); nwavT.scale(-1, 1); }
            case 3 -> { nwavT.translate(nwavAncho, nwavAlto); nwavT.rotate(Math.PI); }
            case 4 -> { nwavT.translate(0, nwavAlto); nwavT.scale(1, -1); }
            case 5 -> { nwavT.rotate(Math.PI / 2); nwavT.scale(1, -1); }
            case 6 -> { nwavT.translate(nwavAlto, 0); nwavT.rotate(Math.PI / 2); }
            case 7 -> { nwavT.scale(-1, 1); nwavT.translate(-nwavAlto, 0); nwavT.translate(0, nwavAncho);
                nwavT.rotate(3 * Math.PI / 2); }
            default -> { nwavT.translate(0, nwavAncho); nwavT.rotate(3 * Math.PI / 2); }
        }
        BufferedImage nwavGirada = new BufferedImage(nwavIntercambia ? nwavAlto : nwavAncho,
                nwavIntercambia ? nwavAncho : nwavAlto,
                nwavImagen.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D nwavG = nwavGirada.createGraphics();
        nwavG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        nwavG.drawImage(nwavImagen, nwavT, null);
        nwavG.dispose();
        return nwavGirada;
    }

    static byte[] nwavPng(BufferedImage nwavImagen) throws IOException {
        ByteArrayOutputStream nwavSalida = new ByteArrayOutputStream();
        ImageIO.write(nwavImagen, "png", nwavSalida);
        return nwavSalida.toByteArray();
    }
}
