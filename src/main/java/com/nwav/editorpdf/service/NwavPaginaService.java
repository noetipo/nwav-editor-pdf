package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones de pagina sobre el documento abierto, en memoria (no escriben disco).
 * Son las primitivas que usan los comandos de deshacer/rehacer. Todas reconstruyen el
 * arbol de paginas con un orden explicito, lo que hace triviales deshacer y rehacer.
 */
@Service
@RequiredArgsConstructor
public class NwavPaginaService {

    /** Margen alrededor de una imagen insertada como pagina, en puntos. */
    public static final float NWAV_MARGEN_IMAGEN = 24f;

    private final NwavPdfDocumentoService nwavDocumentos;

    // ------------------------------------------------------------------ orden

    public NwavOrdenPaginas nwavCapturarOrden(NwavDocumentoPdf nwavDocumento) {
        synchronized (nwavDocumento) {
            nwavDocumento.nwavVerificarAbierto();
            return new NwavOrdenPaginas(nwavPaginas(nwavDocumento.nwavPdDocument()));
        }
    }

    public void nwavRestaurarOrden(NwavDocumentoPdf nwavDocumento, NwavOrdenPaginas nwavOrden) {
        synchronized (nwavDocumento) {
            nwavDocumento.nwavVerificarAbierto();
            nwavEstablecerOrden(nwavDocumento, nwavOrden.nwavPaginas());
        }
    }

    // -------------------------------------------------------------- rotacion

    /** Suma {@code nwavGrados} (multiplo de 90, puede ser negativo) a la rotacion de cada pagina. */
    public void nwavRotar(NwavDocumentoPdf nwavDocumento, List<Integer> nwavIndices, int nwavGrados) {
        if (nwavGrados % 90 != 0) {
            throw new IllegalArgumentException("La rotación debe ser múltiplo de 90°");
        }
        synchronized (nwavDocumento) {
            nwavDocumento.nwavVerificarAbierto();
            PDDocument nwavPd = nwavDocumento.nwavPdDocument();
            for (int nwavIndice : nwavValidar(nwavIndices, nwavPd.getNumberOfPages())) {
                PDPage nwavPagina = nwavPd.getPage(nwavIndice);
                nwavPagina.setRotation((((nwavPagina.getRotation() + nwavGrados) % 360) + 360) % 360);
            }
            nwavDocumento.nwavActualizarEstructura();
        }
    }

    // ----------------------------------------------------- operaciones de orden

    public void nwavEliminar(NwavDocumentoPdf nwavDocumento, List<Integer> nwavIndices) {
        synchronized (nwavDocumento) {
            nwavDocumento.nwavVerificarAbierto();
            List<PDPage> nwavOrden = nwavPaginas(nwavDocumento.nwavPdDocument());
            List<Integer> nwavValidos = nwavValidar(nwavIndices, nwavOrden.size());
            if (nwavValidos.size() >= nwavOrden.size()) {
                throw new NwavEditorException("No se pueden eliminar todas las páginas: un PDF necesita al menos una.");
            }
            List<PDPage> nwavNuevo = new ArrayList<>();
            for (int nwavIndice = 0; nwavIndice < nwavOrden.size(); nwavIndice++) {
                if (!nwavValidos.contains(nwavIndice)) {
                    nwavNuevo.add(nwavOrden.get(nwavIndice));
                }
            }
            nwavEstablecerOrden(nwavDocumento, nwavNuevo);
        }
    }

    /** Inserta una copia justo despues de cada pagina. Devuelve el indice de la primera copia. */
    public int nwavDuplicar(NwavDocumentoPdf nwavDocumento, List<Integer> nwavIndices) {
        synchronized (nwavDocumento) {
            nwavDocumento.nwavVerificarAbierto();
            List<PDPage> nwavOrden = nwavPaginas(nwavDocumento.nwavPdDocument());
            List<Integer> nwavValidos = nwavValidar(nwavIndices, nwavOrden.size());
            List<PDPage> nwavNuevo = new ArrayList<>();
            int nwavPrimeraCopia = -1;
            for (int nwavIndice = 0; nwavIndice < nwavOrden.size(); nwavIndice++) {
                PDPage nwavPagina = nwavOrden.get(nwavIndice);
                nwavNuevo.add(nwavPagina);
                if (nwavValidos.contains(nwavIndice)) {
                    if (nwavPrimeraCopia < 0) {
                        nwavPrimeraCopia = nwavNuevo.size();
                    }
                    nwavNuevo.add(nwavCopiar(nwavPagina));
                }
            }
            nwavEstablecerOrden(nwavDocumento, nwavNuevo);
            return nwavPrimeraCopia;
        }
    }

    /**
     * Mueve las paginas indicadas, como bloque y en su orden, para que queden antes de la
     * pagina que ahora ocupa {@code nwavDestino} ({@code nwavDestino == total} = al final).
     * Devuelve el nuevo indice de la primera pagina movida.
     */
    public int nwavMover(NwavDocumentoPdf nwavDocumento, List<Integer> nwavIndices, int nwavDestino) {
        synchronized (nwavDocumento) {
            nwavDocumento.nwavVerificarAbierto();
            List<PDPage> nwavOrden = nwavPaginas(nwavDocumento.nwavPdDocument());
            List<Integer> nwavValidos = nwavValidar(nwavIndices, nwavOrden.size());
            int nwavDestinoAcotado = Math.max(0, Math.min(nwavOrden.size(), nwavDestino));
            List<PDPage> nwavMovidas = nwavValidos.stream().map(nwavOrden::get).toList();
            List<PDPage> nwavResto = new ArrayList<>();
            for (int nwavIndice = 0; nwavIndice < nwavOrden.size(); nwavIndice++) {
                if (!nwavValidos.contains(nwavIndice)) {
                    nwavResto.add(nwavOrden.get(nwavIndice));
                }
            }
            long nwavAntesDelDestino = nwavValidos.stream().filter(nwavIndice -> nwavIndice < nwavDestinoAcotado).count();
            int nwavPosicion = (int) (nwavDestinoAcotado - nwavAntesDelDestino);
            nwavResto.addAll(nwavPosicion, nwavMovidas);
            nwavEstablecerOrden(nwavDocumento, nwavResto);
            return nwavPosicion;
        }
    }

    /**
     * Inserta paginas de otro PDF en {@code nwavPosicion}. El PDF de origen queda abierto
     * (las paginas importadas lo referencian) hasta que se guarde o cierre el documento.
     *
     * @param nwavRangos paginas del origen ("1-3, 5"); vacio o {@code null} = todas.
     * @return indice de la primera pagina insertada.
     */
    public int nwavInsertarPdf(NwavDocumentoPdf nwavDocumento, Path nwavOrigen, String nwavContrasena,
                               String nwavRangos, int nwavPosicion) {
        PDDocument nwavOtro = nwavDocumentos.nwavCargarDocumento(nwavOrigen, nwavContrasena);
        synchronized (nwavDocumento) {
            List<PDPage> nwavAntes = null;
            try {
                nwavDocumento.nwavVerificarAbierto();
                PDDocument nwavPd = nwavDocumento.nwavPdDocument();
                nwavAntes = nwavPaginas(nwavPd);
                List<Integer> nwavSeleccion = nwavRangos == null || nwavRangos.isBlank()
                        ? java.util.stream.IntStream.range(0, nwavOtro.getNumberOfPages()).boxed().toList()
                        : NwavRangosPaginas.nwavIndices(nwavRangos, nwavOtro.getNumberOfPages());
                List<PDPage> nwavImportadas = new ArrayList<>();
                for (int nwavIndice : nwavSeleccion) {
                    PDPage nwavFuente = nwavOtro.getPage(nwavIndice);
                    nwavMaterializar(nwavFuente);
                    nwavImportadas.add(nwavPd.importPage(nwavFuente));
                }
                int nwavDonde = Math.max(0, Math.min(nwavAntes.size(), nwavPosicion));
                List<PDPage> nwavNuevo = new ArrayList<>(nwavAntes);
                nwavNuevo.addAll(nwavDonde, nwavImportadas);
                nwavEstablecerOrden(nwavDocumento, nwavNuevo);
                nwavDocumento.nwavRetenerAuxiliar(nwavOtro);
                return nwavDonde;
            } catch (IOException | RuntimeException nwavError) {
                if (nwavAntes != null) {
                    nwavEstablecerOrden(nwavDocumento, nwavAntes); // deja el documento como estaba
                }
                nwavCerrarSilencioso(nwavOtro);
                throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavOrigen.getFileName()));
            }
        }
    }

    /**
     * Inserta cada imagen como una pagina A4 (vertical u horizontal segun la imagen),
     * centrada y sin agrandarla mas alla de su tamano natural.
     *
     * @return indice de la primera pagina insertada.
     */
    public int nwavInsertarImagenes(NwavDocumentoPdf nwavDocumento, List<Path> nwavImagenes, int nwavPosicion) {
        if (nwavImagenes == null || nwavImagenes.isEmpty()) {
            throw new NwavEditorException("Elige al menos una imagen.");
        }
        synchronized (nwavDocumento) {
            nwavDocumento.nwavVerificarAbierto();
            PDDocument nwavPd = nwavDocumento.nwavPdDocument();
            List<PDPage> nwavAntes = nwavPaginas(nwavPd);
            List<PDPage> nwavNuevas = new ArrayList<>();
            for (Path nwavImagen : nwavImagenes) {
                nwavNuevas.add(nwavCrearPaginaConImagen(nwavPd, nwavImagen));
            }
            int nwavDonde = Math.max(0, Math.min(nwavAntes.size(), nwavPosicion));
            List<PDPage> nwavNuevo = new ArrayList<>(nwavAntes);
            nwavNuevo.addAll(nwavDonde, nwavNuevas);
            nwavEstablecerOrden(nwavDocumento, nwavNuevo);
            return nwavDonde;
        }
    }

    // ---------------------------------------------------------------- recortar

    /**
     * Recorta (CropBox) las paginas indicadas al area visual dada (medida sobre como se ve
     * la pagina). Cambia el tamano visible, asi que es un cambio estructural. Deshacer
     * restaura la CropBox anterior de cada pagina.
     */
    public com.nwav.editorpdf.service.comando.NwavComando nwavComandoRecortar(NwavDocumentoPdf nwavDocumento,
                                                                             List<Integer> nwavIndices,
                                                                             com.nwav.editorpdf.model.NwavRectangulo nwavZona) {
        List<Integer> nwavPaginas = List.copyOf(nwavIndices);
        return new com.nwav.editorpdf.service.comando.NwavComando() {
            private final java.util.Map<PDPage, org.apache.pdfbox.cos.COSBase> nwavAntes = new java.util.LinkedHashMap<>();
            private final java.util.Map<PDPage, PDRectangle> nwavDespues = new java.util.LinkedHashMap<>();

            @Override
            public void nwavEjecutar() {
                synchronized (nwavDocumento) {
                    nwavDocumento.nwavVerificarAbierto();
                    PDDocument nwavPd = nwavDocumento.nwavPdDocument();
                    if (nwavDespues.isEmpty()) {
                        for (int nwavIndice : nwavValidar(nwavPaginas, nwavPd.getNumberOfPages())) {
                            PDPage nwavPagina = nwavPd.getPage(nwavIndice);
                            nwavMaterializar(nwavPagina);
                            nwavAntes.put(nwavPagina, nwavPagina.getCOSObject().getDictionaryObject(COSName.CROP_BOX));
                            nwavDespues.put(nwavPagina, NwavOrganizarService.nwavCajaRecorte(nwavPagina, nwavZona));
                        }
                    }
                    nwavDespues.forEach(PDPage::setCropBox);
                    nwavDocumento.nwavActualizarEstructura();
                }
            }

            @Override
            public void nwavDeshacer() {
                synchronized (nwavDocumento) {
                    nwavAntes.forEach((nwavPagina, nwavCaja) -> {
                        if (nwavCaja == null) {
                            nwavPagina.getCOSObject().removeItem(COSName.CROP_BOX);
                        } else {
                            nwavPagina.getCOSObject().setItem(COSName.CROP_BOX, nwavCaja);
                        }
                    });
                    nwavDocumento.nwavActualizarEstructura();
                }
            }

            @Override
            public com.nwav.editorpdf.model.NwavTipoOperacion nwavTipo() {
                return com.nwav.editorpdf.model.NwavTipoOperacion.NWAV_RECORTAR;
            }

            @Override
            public String nwavDescripcion() {
                return nwavPaginas.size() == 1 ? "Recortar la página " + (nwavPaginas.get(0) + 1)
                        : "Recortar " + nwavPaginas.size() + " páginas";
            }

            @Override
            public int nwavPaginaDestacada() {
                return nwavPaginas.get(0);
            }
        };
    }

    // --------------------------------------------------------------- internos

    /** Carga una imagen (PNG, JPG, GIF, BMP…) para usarla en el documento. */
    static PDImageXObject nwavCargarImagen(PDDocument nwavPd, Path nwavRuta) {
        String nwavNombre = String.valueOf(nwavRuta.getFileName());
        if (!java.nio.file.Files.isRegularFile(nwavRuta)) {
            throw new com.nwav.editorpdf.exception.NwavArchivoNoEncontradoException(nwavNombre);
        }
        try {
            return PDImageXObject.createFromFileByContent(nwavRuta.toFile(), nwavPd);
        } catch (IOException | IllegalArgumentException nwavError) {
            return nwavLeerConImageIo(nwavPd, nwavRuta, nwavNombre);
        }
    }

    private static PDPage nwavCrearPaginaConImagen(PDDocument nwavPd, Path nwavRuta) {
        String nwavNombre = String.valueOf(nwavRuta.getFileName());
        PDImageXObject nwavImagen = nwavCargarImagen(nwavPd, nwavRuta);
        float nwavAncho = nwavImagen.getWidth();
        float nwavAlto = nwavImagen.getHeight();
        PDRectangle nwavHoja = nwavAncho > nwavAlto
                ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())
                : PDRectangle.A4;
        float nwavEscala = Math.min(1f, Math.min(
                (nwavHoja.getWidth() - 2 * NWAV_MARGEN_IMAGEN) / nwavAncho,
                (nwavHoja.getHeight() - 2 * NWAV_MARGEN_IMAGEN) / nwavAlto));
        float nwavAnchoFinal = nwavAncho * nwavEscala;
        float nwavAltoFinal = nwavAlto * nwavEscala;
        PDPage nwavPagina = new PDPage(nwavHoja);
        try (PDPageContentStream nwavContenido = new PDPageContentStream(nwavPd, nwavPagina)) {
            nwavContenido.drawImage(nwavImagen, (nwavHoja.getWidth() - nwavAnchoFinal) / 2,
                    (nwavHoja.getHeight() - nwavAltoFinal) / 2, nwavAnchoFinal, nwavAltoFinal);
        } catch (IOException nwavError) {
            throw new NwavEditorException("No se pudo insertar la imagen «" + nwavNombre + "».", nwavError);
        }
        return nwavPagina;
    }

    private static PDImageXObject nwavLeerConImageIo(PDDocument nwavPd, Path nwavRuta, String nwavNombre) {
        try {
            BufferedImage nwavLeida = ImageIO.read(nwavRuta.toFile());
            if (nwavLeida == null) {
                throw new NwavEditorException("«" + nwavNombre
                        + "» no es una imagen compatible. Usa PNG, JPG, GIF o BMP.");
            }
            return LosslessFactory.createFromImage(nwavPd, nwavLeida);
        } catch (IOException nwavError) {
            throw new NwavEditorException("No se pudo leer la imagen «" + nwavNombre + "».", nwavError);
        }
    }

    static List<PDPage> nwavPaginas(PDDocument nwavPd) {
        List<PDPage> nwavLista = new ArrayList<>(nwavPd.getNumberOfPages());
        nwavPd.getPages().forEach(nwavLista::add);
        return nwavLista;
    }

    /**
     * Reconstruye el arbol de paginas como una lista plana con el orden dado. Antes copia
     * a cada pagina los atributos que heredaba de nodos intermedios, que dejan de existir.
     */
    private static void nwavEstablecerOrden(NwavDocumentoPdf nwavDocumento, List<PDPage> nwavOrden) {
        PDDocument nwavPd = nwavDocumento.nwavPdDocument();
        COSDictionary nwavRaiz = nwavPd.getDocumentCatalog().getPages().getCOSObject();
        for (PDPage nwavPagina : nwavOrden) {
            nwavMaterializar(nwavPagina);
        }
        COSArray nwavHijos = new COSArray();
        for (PDPage nwavPagina : nwavOrden) {
            nwavPagina.getCOSObject().setItem(COSName.PARENT, nwavRaiz);
            nwavHijos.add(nwavPagina.getCOSObject());
        }
        nwavRaiz.setItem(COSName.KIDS, nwavHijos);
        nwavRaiz.setInt(COSName.COUNT, nwavOrden.size());
        nwavDocumento.nwavActualizarEstructura();
    }

    /** Copia en la propia pagina los atributos heredables (recursos, cajas y rotacion). */
    static void nwavMaterializar(PDPage nwavPagina) {
        COSDictionary nwavDiccionario = nwavPagina.getCOSObject();
        if (!nwavDiccionario.containsKey(COSName.RESOURCES)) {
            nwavPagina.setResources(nwavPagina.getResources());
        }
        if (!nwavDiccionario.containsKey(COSName.MEDIA_BOX)) {
            nwavPagina.setMediaBox(nwavPagina.getMediaBox());
        }
        if (!nwavDiccionario.containsKey(COSName.CROP_BOX)) {
            nwavPagina.setCropBox(nwavPagina.getCropBox());
        }
        if (!nwavDiccionario.containsKey(COSName.ROTATE)) {
            nwavPagina.setRotation(nwavPagina.getRotation());
        }
    }

    /**
     * Copia superficial de la pagina: comparte contenido y recursos (no se modifican), pero
     * tiene su propio diccionario y su propia lista de anotaciones.
     */
    private static PDPage nwavCopiar(PDPage nwavOriginal) {
        nwavMaterializar(nwavOriginal);
        COSDictionary nwavCopia = new COSDictionary(nwavOriginal.getCOSObject());
        nwavCopia.removeItem(COSName.PARENT);
        COSArray nwavAnotaciones = nwavCopia.getCOSArray(COSName.ANNOTS);
        if (nwavAnotaciones != null) {
            COSArray nwavNuevas = new COSArray();
            for (int nwavIndice = 0; nwavIndice < nwavAnotaciones.size(); nwavIndice++) {
                nwavNuevas.add(nwavAnotaciones.get(nwavIndice));
            }
            nwavCopia.setItem(COSName.ANNOTS, nwavNuevas);
        }
        return new PDPage(nwavCopia);
    }

    private static List<Integer> nwavValidar(List<Integer> nwavIndices, int nwavTotal) {
        if (nwavIndices == null || nwavIndices.isEmpty()) {
            throw new NwavEditorException("Selecciona al menos una página.");
        }
        List<Integer> nwavOrdenados = nwavIndices.stream().distinct().sorted().toList();
        if (nwavOrdenados.get(0) < 0 || nwavOrdenados.get(nwavOrdenados.size() - 1) >= nwavTotal) {
            throw new NwavEditorException("La selección incluye páginas que no existen.");
        }
        return nwavOrdenados;
    }

    private static void nwavCerrarSilencioso(PDDocument nwavPd) {
        try {
            nwavPd.close();
        } catch (IOException nwavError) {
            // ya se informa el error original
        }
    }
}
