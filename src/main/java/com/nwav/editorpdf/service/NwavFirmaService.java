package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.model.NwavFirma;
import com.nwav.editorpdf.repository.NwavFirmaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NwavFirmaService {
    public static final int NWAV_BYTES_MAXIMOS = 2 * 1024 * 1024;
    public static final int NWAV_LADO_MAXIMO = 1600;
    private static final int NWAV_UMBRAL_BLANCO = 235;

    private final NwavFirmaRepository nwavRepositorio;

    @Transactional(readOnly = true)
    public List<NwavFirma> nwavListar() {
        return nwavRepositorio.findAllByOrderByNwavFechaCreacionDesc();
    }

    @Transactional
    public NwavFirma nwavGuardar(String nwavNombre, byte[] nwavPng) {
        String nwavLimpio = nwavNombre == null ? "" : nwavNombre.strip();
        if (nwavLimpio.isEmpty()) {
            throw new NwavEditorException("Ponle un nombre a la firma.");
        }
        if (nwavPng == null || nwavPng.length == 0) {
            throw new NwavEditorException("La firma está vacía.");
        }
        if (nwavPng.length > NWAV_BYTES_MAXIMOS) {
            throw new NwavEditorException("La imagen de la firma es demasiado grande (máximo 2 MB).");
        }
        nwavLeer(nwavPng);
        NwavFirma nwavFirma = new NwavFirma();
        nwavFirma.setNwavNombre(nwavLimpio.length() > 120 ? nwavLimpio.substring(0, 120) : nwavLimpio);
        nwavFirma.setNwavImagen(nwavPng);
        nwavFirma.setNwavFechaCreacion(LocalDateTime.now());
        return nwavRepositorio.save(nwavFirma);
    }

    @Transactional
    public void nwavEliminar(Long nwavId) {
        nwavRepositorio.deleteById(nwavId);
    }

    public static java.util.List<String> nwavFuentesManuscritas() {
        java.util.Set<String> nwavInstaladas = new java.util.HashSet<>(java.util.Arrays.asList(
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        java.util.List<String> nwavElegidas = new java.util.ArrayList<>();
        for (String nwavCandidata : java.util.List.of("Snell Roundhand", "Brush Script MT", "Segoe Script", "Apple Chancery",
                "Lucida Handwriting", "Bradley Hand", "Savoye LET", "Ink Free", "Zapfino", "URW Chancery L",
                "Z003", "Comic Sans MS")) {
            if (nwavInstaladas.contains(nwavCandidata)) {
                nwavElegidas.add(nwavCandidata);
            }
        }
        nwavElegidas.add("Serif");
        return nwavElegidas;
    }

    public byte[] nwavFirmaEscrita(String nwavTexto, String nwavFamilia, java.awt.Color nwavColor) {
        String nwavLimpio = nwavTexto == null ? "" : nwavTexto.strip();
        if (nwavLimpio.isEmpty()) {
            throw new NwavEditorException("Escribe tu nombre para crear la firma.");
        }
        int nwavEstilo = "Serif".equals(nwavFamilia) ? java.awt.Font.ITALIC : java.awt.Font.PLAIN;
        java.awt.Font nwavFuente = new java.awt.Font(nwavFamilia == null ? "Serif" : nwavFamilia, nwavEstilo, 96);
        java.awt.image.BufferedImage nwavMedida = new java.awt.image.BufferedImage(1, 1,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D nwavG = nwavMedida.createGraphics();
        java.awt.font.FontRenderContext nwavContexto = nwavG.getFontRenderContext();
        java.awt.geom.Rectangle2D nwavCaja = new java.awt.font.TextLayout(nwavLimpio, nwavFuente, nwavContexto).getBounds();
        nwavG.dispose();
        int nwavMargen = 24;
        int nwavAncho = Math.min(NWAV_LADO_MAXIMO, (int) Math.ceil(nwavCaja.getWidth()) + 2 * nwavMargen);
        int nwavAlto = (int) Math.ceil(nwavCaja.getHeight()) + 2 * nwavMargen;
        java.awt.image.BufferedImage nwavImagen = new java.awt.image.BufferedImage(nwavAncho, nwavAlto,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D nwavLienzo = nwavImagen.createGraphics();
        nwavLienzo.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        nwavLienzo.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        nwavLienzo.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS,
                java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        nwavLienzo.setColor(nwavColor == null ? new java.awt.Color(20, 40, 110) : nwavColor);
        nwavLienzo.setFont(nwavFuente);
        nwavLienzo.drawString(nwavLimpio, (float) (nwavMargen - nwavCaja.getX()), (float) (nwavMargen - nwavCaja.getY()));
        nwavLienzo.dispose();
        try {
            java.io.ByteArrayOutputStream nwavBytes = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(nwavImagen, "png", nwavBytes);
            return nwavBytes.toByteArray();
        } catch (java.io.IOException nwavError) {
            throw new NwavEditorException("No se pudo crear la firma escrita.", nwavError);
        }
    }

    public byte[] nwavPrepararImagen(byte[] nwavOriginal, boolean nwavQuitarFondo) {
        BufferedImage nwavFuente = nwavLeer(nwavOriginal);
        int nwavAncho = nwavFuente.getWidth();
        int nwavAlto = nwavFuente.getHeight();
        BufferedImage nwavArgb = new BufferedImage(nwavAncho, nwavAlto, BufferedImage.TYPE_INT_ARGB);
        int nwavMinX = nwavAncho;
        int nwavMinY = nwavAlto;
        int nwavMaxX = -1;
        int nwavMaxY = -1;
        for (int nwavY = 0; nwavY < nwavAlto; nwavY++) {
            for (int nwavX = 0; nwavX < nwavAncho; nwavX++) {
                int nwavPixel = nwavFuente.getRGB(nwavX, nwavY);
                int nwavAlfa = (nwavPixel >>> 24) & 0xFF;
                int nwavBrillo = (((nwavPixel >> 16) & 0xFF) + ((nwavPixel >> 8) & 0xFF) + (nwavPixel & 0xFF)) / 3;
                if (nwavQuitarFondo && nwavBrillo >= NWAV_UMBRAL_BLANCO) {
                    nwavAlfa = 0;
                }
                nwavArgb.setRGB(nwavX, nwavY, (nwavAlfa << 24) | (nwavPixel & 0xFFFFFF));
                if (nwavAlfa > 16) {
                    nwavMinX = Math.min(nwavMinX, nwavX);
                    nwavMinY = Math.min(nwavMinY, nwavY);
                    nwavMaxX = Math.max(nwavMaxX, nwavX);
                    nwavMaxY = Math.max(nwavMaxY, nwavY);
                }
            }
        }
        if (nwavMaxX < 0) {
            throw new NwavEditorException("La firma está vacía: no hay trazos visibles.");
        }
        int nwavMargen = 4;
        int nwavX0 = Math.max(0, nwavMinX - nwavMargen);
        int nwavY0 = Math.max(0, nwavMinY - nwavMargen);
        int nwavX1 = Math.min(nwavAncho - 1, nwavMaxX + nwavMargen);
        int nwavY1 = Math.min(nwavAlto - 1, nwavMaxY + nwavMargen);
        BufferedImage nwavRecorte = nwavArgb.getSubimage(nwavX0, nwavY0, nwavX1 - nwavX0 + 1, nwavY1 - nwavY0 + 1);
        double nwavFactor = Math.min(1.0, (double) NWAV_LADO_MAXIMO
                / Math.max(nwavRecorte.getWidth(), nwavRecorte.getHeight()));
        if (nwavFactor < 1.0) {
            int nwavNuevoAncho = Math.max(1, (int) Math.round(nwavRecorte.getWidth() * nwavFactor));
            int nwavNuevoAlto = Math.max(1, (int) Math.round(nwavRecorte.getHeight() * nwavFactor));
            BufferedImage nwavReducida = new BufferedImage(nwavNuevoAncho, nwavNuevoAlto, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D nwavLienzo = nwavReducida.createGraphics();
            nwavLienzo.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            nwavLienzo.drawImage(nwavRecorte, 0, 0, nwavNuevoAncho, nwavNuevoAlto, null);
            nwavLienzo.dispose();
            nwavRecorte = nwavReducida;
        }
        return nwavAPng(nwavRecorte);
    }

    static BufferedImage nwavLeer(byte[] nwavBytes) {
        try {
            BufferedImage nwavImagen = ImageIO.read(new ByteArrayInputStream(nwavBytes));
            if (nwavImagen == null) {
                throw new NwavEditorException("El archivo no es una imagen compatible (usa PNG, JPG, GIF o BMP).");
            }
            return nwavImagen;
        } catch (IOException nwavError) {
            throw new NwavEditorException("No se pudo leer la imagen.", nwavError);
        }
    }

    static byte[] nwavAPng(BufferedImage nwavImagen) {
        try (ByteArrayOutputStream nwavSalida = new ByteArrayOutputStream()) {
            ImageIO.write(nwavImagen, "png", nwavSalida);
            return nwavSalida.toByteArray();
        } catch (IOException nwavError) {
            throw new NwavEditorException("No se pudo convertir la imagen.", nwavError);
        }
    }
}
