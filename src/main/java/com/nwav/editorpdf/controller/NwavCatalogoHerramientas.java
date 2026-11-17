package com.nwav.editorpdf.controller;

import com.nwav.editorpdf.model.NwavNivelCompresion;
import com.nwav.editorpdf.model.NwavOpcionesCompresion;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.NwavCompresorService;
import com.nwav.editorpdf.service.NwavEscrituraSegura;
import com.nwav.editorpdf.service.NwavOcrService;
import com.nwav.editorpdf.service.NwavReparacionService;
import com.nwav.editorpdf.service.herramienta.NwavDependenciasService;
import com.nwav.editorpdf.service.herramienta.NwavEjecutorHerramientasService.NwavTrabajo;
import com.nwav.editorpdf.service.herramienta.NwavEntradaProceso;
import com.nwav.editorpdf.service.herramienta.NwavResultadoProceso;
import com.nwav.editorpdf.ui.NwavCampoOpcion;
import com.nwav.editorpdf.ui.NwavCategoriaHerramienta;
import com.nwav.editorpdf.ui.NwavDefinicionHerramienta;
import com.nwav.editorpdf.ui.NwavHerramienta;
import com.nwav.editorpdf.ui.NwavNotificador;
import com.nwav.editorpdf.ui.NwavTareaFondo;
import com.nwav.editorpdf.ui.NwavVistaPreviaCompresion;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Catalogo de herramientas de la pantalla de inicio. Cada herramienta declara sus opciones
 * y su proceso (un servicio sin JavaFX); la interfaz comun se encarga del resto.
 */
@Component
@RequiredArgsConstructor
public class NwavCatalogoHerramientas {

    public static final String NWAV_COMPRIMIR = "comprimir";
    public static final String NWAV_OCR = "ocr";
    public static final String NWAV_REPARAR = "reparar";

    private final NwavCompresorService nwavCompresor;
    private final NwavOcrService nwavOcr;
    private final NwavReparacionService nwavReparacion;
    private final NwavDependenciasService nwavDependencias;
    private final NwavTareaFondo nwavTareaFondo;
    private final NwavNotificador nwavNotificador;
    private final NwavVisorController nwavVisor;

    private List<NwavDefinicionHerramienta> nwavDefiniciones;

    public synchronized List<NwavDefinicionHerramienta> nwavTodas() {
        if (nwavDefiniciones == null) {
            nwavDefiniciones = nwavCrear();
        }
        return nwavDefiniciones;
    }

    public Optional<NwavDefinicionHerramienta> nwavBuscar(String nwavId) {
        return nwavTodas().stream().filter(nwavDefinicion -> nwavDefinicion.getNwavId().equals(nwavId)).findFirst();
    }

    private List<NwavDefinicionHerramienta> nwavCrear() {
        List<NwavDefinicionHerramienta> nwavLista = new ArrayList<>();
        nwavAgregarOrganizar(nwavLista);
        nwavAgregarOptimizar(nwavLista);
        nwavAgregarEditar(nwavLista);
        nwavAgregarSeguridad(nwavLista);
        return nwavLista;
    }

    // ================================================================ Organizar

    private void nwavAgregarOrganizar(List<NwavDefinicionHerramienta> nwavLista) {
        nwavLista.add(new NwavDefinicionHerramienta("unir", "Unir PDF",
                "Combina varios PDF en uno, en el orden que elijas.", NwavCategoriaHerramienta.NWAV_ORGANIZAR,
                Feather.GIT_MERGE).nwavEditor(nwavVisor::nwavOnUnir).nwavSinDocumento()
                .nwavConPalabrasClave("juntar combinar fusionar"));
        nwavLista.add(new NwavDefinicionHerramienta("dividir", "Dividir PDF",
                "Separa un PDF en varios archivos por rangos o cada N páginas.", NwavCategoriaHerramienta.NWAV_ORGANIZAR,
                Feather.SCISSORS).nwavEditor(nwavVisor::nwavOnDividir).nwavConPalabrasClave("separar partir"));
        nwavLista.add(new NwavDefinicionHerramienta("organizar", "Organizar páginas",
                "Reordena, rota, duplica o elimina páginas arrastrando las miniaturas.",
                NwavCategoriaHerramienta.NWAV_ORGANIZAR, Feather.LAYERS).nwavEditor(() -> {
                    nwavVisor.nwavMiniaturasVisiblesProperty().set(true);
                    nwavNotificador.nwavInfo("Arrastra las miniaturas para reordenar; clic derecho para rotar o eliminar.");
                }).nwavConPalabrasClave("ordenar rotar eliminar borrar paginas"));
        nwavLista.add(new NwavDefinicionHerramienta("extraer", "Extraer páginas",
                "Copia las páginas que elijas a un PDF nuevo.", NwavCategoriaHerramienta.NWAV_ORGANIZAR,
                Feather.EXTERNAL_LINK).nwavEditor(nwavVisor::nwavOnExtraer));
    }

    // ================================================================ Optimizar

    private void nwavAgregarOptimizar(List<NwavDefinicionHerramienta> nwavLista) {
        NwavVistaPreviaCompresion nwavVistaPrevia = new NwavVistaPreviaCompresion(nwavCompresor, nwavTareaFondo,
                nwavNotificador, NwavCatalogoHerramientas::nwavOpcionesCompresion);
        nwavLista.add(new NwavDefinicionHerramienta(NWAV_COMPRIMIR, "Comprimir PDF",
                "Reduce el tamaño optimizando las imágenes, sin tocar el texto ni los vectores.",
                NwavCategoriaHerramienta.NWAV_OPTIMIZAR, Feather.MINIMIZE_2)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_COMPRIMIR, true, this::nwavProcesoComprimir))
                .nwavConOpciones(
                        NwavCampoOpcion.nwavEleccion("nivel", "Nivel de compresión", NwavNivelCompresion.NWAV_RECOMENDADA.name(),
                                Arrays.stream(NwavNivelCompresion.values())
                                        .map(nwavNivel -> nwavNivel.name() + "=" + nwavNivel.getNwavDescripcion())
                                        .toArray(String[]::new)),
                        NwavCampoOpcion.nwavEntero("dpi", "Resolución máxima de las imágenes (DPI)", 150, 36, 600)
                                .nwavSi("nivel", NwavNivelCompresion.NWAV_PERSONALIZADA.name()),
                        NwavCampoOpcion.nwavEntero("calidad", "Calidad JPEG (%)", 75, 10, 95)
                                .nwavSi("nivel", NwavNivelCompresion.NWAV_PERSONALIZADA.name()),
                        NwavCampoOpcion.nwavCasilla("grises", "Convertir a escala de grises", false),
                        NwavCampoOpcion.nwavCasilla("metadatos", "Quitar metadatos y miniaturas incrustadas", false)
                                .nwavConAyuda("El nivel extremo siempre los quita."))
                .nwavConExtra(nwavVistaPrevia::nwavCrear)
                .nwavConPalabrasClave("reducir tamaño optimizar peso imagenes"));

        nwavLista.add(new NwavDefinicionHerramienta(NWAV_OCR, "OCR PDF",
                "Convierte PDF escaneados en documentos con texto seleccionable y buscable.",
                NwavCategoriaHerramienta.NWAV_OPTIMIZAR, Feather.TYPE)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_OCR, true, this::nwavProcesoOcr))
                .nwavRequiere(nwavDependencias::nwavTesseract)
                .nwavConOpciones(
                        NwavCampoOpcion.nwavMultiple("idiomas", "Idiomas del documento", "spa,eng",
                                () -> nwavDependencias.nwavIdiomasDisponibles().stream()
                                        .map(nwavCodigo -> nwavCodigo + "=" + NwavOcrService.nwavNombreIdioma(nwavCodigo))
                                        .toList())
                                .nwavConAyuda("Puedes elegir varios. Otros idiomas: instala sus datos (tessdata) e indica "
                                        + "la carpeta en Preferencias."),
                        NwavCampoOpcion.nwavEleccion("salida", "Resultado", "pdf",
                                "pdf=PDF buscable", "txt=Texto plano (.txt)", "ambos=PDF buscable y texto"),
                        NwavCampoOpcion.nwavCasilla("forzar", "Aplicar OCR también a las páginas que ya tienen texto", false),
                        NwavCampoOpcion.nwavCasilla("enderezar", "Enderezar páginas torcidas", true),
                        NwavCampoOpcion.nwavCasilla("grises", "Pasar a escala de grises para reconocer", false),
                        NwavCampoOpcion.nwavCasilla("contraste", "Mejorar el contraste", false))
                .nwavOfrecer(NWAV_COMPRIMIR, "Comprimir")
                .nwavConPalabrasClave("escaneado reconocer texto buscar seleccionable tesseract"));

        nwavLista.add(new NwavDefinicionHerramienta(NWAV_REPARAR, "Reparar PDF",
                "Recupera PDF dañados reconstruyendo su estructura.", NwavCategoriaHerramienta.NWAV_OPTIMIZAR,
                Feather.TOOL)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_REPARAR, true, this::nwavProcesoReparar))
                .nwavConPalabrasClave("corrupto dañado recuperar arreglar"));
    }

    public static NwavOpcionesCompresion nwavOpcionesCompresion(Map<String, String> nwavOpciones) {
        NwavNivelCompresion nwavNivel;
        try {
            nwavNivel = NwavNivelCompresion.valueOf(nwavOpciones.getOrDefault("nivel", "NWAV_RECOMENDADA"));
        } catch (RuntimeException nwavError) {
            nwavNivel = NwavNivelCompresion.NWAV_RECOMENDADA;
        }
        boolean nwavGrises = Boolean.parseBoolean(nwavOpciones.get("grises"));
        boolean nwavMetadatos = Boolean.parseBoolean(nwavOpciones.get("metadatos"));
        if (nwavNivel == NwavNivelCompresion.NWAV_PERSONALIZADA) {
            int nwavDpi = nwavEntero(nwavOpciones.get("dpi"), 150);
            int nwavCalidad = nwavEntero(nwavOpciones.get("calidad"), 75);
            return new NwavOpcionesCompresion(nwavDpi, nwavCalidad / 100f, nwavGrises, nwavMetadatos);
        }
        return NwavOpcionesCompresion.nwavDe(nwavNivel, nwavGrises, nwavMetadatos);
    }

    private static int nwavEntero(String nwavTexto, int nwavPorDefecto) {
        try {
            return Integer.parseInt(nwavTexto.trim());
        } catch (RuntimeException nwavError) {
            return nwavPorDefecto;
        }
    }

    private NwavResultadoProceso nwavProcesoComprimir(NwavEntradaProceso nwavEntrada) {
        Path nwavArchivo = nwavEntrada.nwavArchivo();
        Path nwavDestino = NwavEscrituraSegura.nwavRutaSalida(nwavArchivo, nwavEntrada.nwavCarpetaSalida(),
                "comprimido", "pdf");
        NwavCompresorService.NwavResultadoCompresion nwavResultado = nwavCompresor.nwavComprimir(nwavArchivo,
                nwavEntrada.nwavContrasena(nwavArchivo), nwavOpcionesCompresion(nwavEntrada.nwavOpciones()), nwavDestino,
                nwavEntrada.nwavAvance(), nwavEntrada.nwavCancelacion());
        String nwavDetalle = "Antes: " + NwavCompresorService.nwavTamano(nwavResultado.nwavAntes()) + "; después: "
                + NwavCompresorService.nwavTamano(nwavResultado.nwavDespues())
                + String.format(java.util.Locale.ROOT, "; reducción: %.1f %%", nwavResultado.nwavReduccion())
                + (nwavResultado.nwavGuardado() ? "" : " (no se guardó: no reducía el tamaño)")
                + "; imágenes optimizadas: " + nwavResultado.nwavImagenesReducidas()
                + "; duplicadas unidas: " + nwavResultado.nwavImagenesDuplicadas();
        return new NwavResultadoProceso(nwavResultado.nwavGuardado() ? List.of(nwavResultado.nwavDestino()) : List.of(),
                nwavResultado.nwavResumen(), nwavDetalle);
    }

    private NwavResultadoProceso nwavProcesoOcr(NwavEntradaProceso nwavEntrada) {
        Path nwavArchivo = nwavEntrada.nwavArchivo();
        String nwavSalida = nwavEntrada.nwavTexto("salida", "pdf");
        List<String> nwavIdiomas = Arrays.stream(nwavEntrada.nwavTexto("idiomas", "spa,eng").split(","))
                .map(String::strip).filter(nwavCodigo -> !nwavCodigo.isEmpty()).toList();
        NwavOcrService.NwavOpcionesOcr nwavOpciones = new NwavOcrService.NwavOpcionesOcr(nwavIdiomas,
                nwavEntrada.nwavSiNo("forzar", false), nwavEntrada.nwavSiNo("enderezar", true),
                nwavEntrada.nwavSiNo("grises", false), nwavEntrada.nwavSiNo("contraste", false),
                !"txt".equals(nwavSalida), !"pdf".equals(nwavSalida));
        NwavOcrService.NwavResultadoOcr nwavResultado = nwavOcr.nwavAplicar(nwavArchivo, nwavEntrada.nwavContrasena(nwavArchivo),
                nwavOpciones, NwavEscrituraSegura.nwavRutaSalida(nwavArchivo, nwavEntrada.nwavCarpetaSalida(), "ocr", "pdf"),
                NwavEscrituraSegura.nwavRutaSalida(nwavArchivo, nwavEntrada.nwavCarpetaSalida(), "ocr", "txt"),
                nwavEntrada.nwavAvance(), nwavEntrada.nwavCancelacion());
        List<Path> nwavGenerados = new ArrayList<>();
        if (nwavResultado.nwavPdf() != null) {
            nwavGenerados.add(nwavResultado.nwavPdf());
        }
        if (nwavResultado.nwavTexto() != null) {
            nwavGenerados.add(nwavResultado.nwavTexto());
        }
        return new NwavResultadoProceso(nwavGenerados, nwavResultado.nwavResumen(),
                "Páginas procesadas: " + nwavResultado.nwavPaginasReconocidas() + "; idioma: " + nwavResultado.nwavIdiomas()
                        + "; palabras: " + nwavResultado.nwavPalabras());
    }

    private NwavResultadoProceso nwavProcesoReparar(NwavEntradaProceso nwavEntrada) {
        Path nwavArchivo = nwavEntrada.nwavArchivo();
        NwavReparacionService.NwavResultadoReparacion nwavResultado = nwavReparacion.nwavReparar(nwavArchivo,
                nwavEntrada.nwavContrasena(nwavArchivo),
                NwavEscrituraSegura.nwavRutaSalida(nwavArchivo, nwavEntrada.nwavCarpetaSalida(), "reparado", "pdf"),
                nwavEntrada.nwavAvance(), nwavEntrada.nwavCancelacion());
        return NwavResultadoProceso.nwavDe(nwavResultado.nwavDestino(), nwavResultado.nwavResumen());
    }

    // ================================================================== Editar

    private void nwavAgregarEditar(List<NwavDefinicionHerramienta> nwavLista) {
        nwavLista.add(new NwavDefinicionHerramienta("editar", "Editar PDF",
                "Añade texto, imágenes, formas, notas y resaltados.", NwavCategoriaHerramienta.NWAV_EDITAR, Feather.EDIT_3)
                .nwavEditor(() -> nwavVisor.nwavHerramientas().nwavHerramientaProperty().set(NwavHerramienta.NWAV_TEXTO))
                .nwavConPalabrasClave("texto anotar resaltar dibujar formas imagen"));
        nwavLista.add(new NwavDefinicionHerramienta("numeros", "Números de página",
                "Numera las páginas con el formato, la posición y la fuente que quieras.",
                NwavCategoriaHerramienta.NWAV_EDITAR, Feather.HASH).nwavEditor(nwavVisor::nwavOnNumerar)
                .nwavConPalabrasClave("numerar paginacion encabezado pie"));
        nwavLista.add(new NwavDefinicionHerramienta("marca-agua", "Marca de agua",
                "Estampa un texto o una imagen sobre las páginas.", NwavCategoriaHerramienta.NWAV_EDITAR,
                Feather.DROPLET).nwavEditor(nwavVisor::nwavOnMarcaAgua).nwavConPalabrasClave("sello confidencial"));
        nwavLista.add(new NwavDefinicionHerramienta("formulario", "Rellenar formulario",
                "Completa los campos de un formulario PDF.", NwavCategoriaHerramienta.NWAV_EDITAR,
                Feather.CHECK_SQUARE).nwavEditor(nwavVisor::nwavOnFormulario).nwavConPalabrasClave("acroform campos"));
        nwavLista.add(new NwavDefinicionHerramienta("propiedades", "Propiedades del documento",
                "Cambia el título, el autor y otros metadatos.", NwavCategoriaHerramienta.NWAV_EDITAR, Feather.INFO)
                .nwavEditor(nwavVisor::nwavOnMetadatos).nwavConPalabrasClave("metadatos titulo autor"));
    }

    // ================================================================ Seguridad

    private void nwavAgregarSeguridad(List<NwavDefinicionHerramienta> nwavLista) {
        nwavLista.add(new NwavDefinicionHerramienta("proteger", "Proteger o desbloquear",
                "Pon o quita una contraseña y limita lo que se puede hacer con el PDF.",
                NwavCategoriaHerramienta.NWAV_SEGURIDAD, Feather.LOCK).nwavEditor(nwavVisor::nwavOnProteccion)
                .nwavConPalabrasClave("contraseña cifrar permisos desbloquear quitar"));
        nwavLista.add(new NwavDefinicionHerramienta("firmar", "Firmar PDF",
                "Firma con una firma dibujada o cargada desde una imagen.", NwavCategoriaHerramienta.NWAV_SEGURIDAD,
                Feather.FEATHER).nwavEditor(nwavVisor::nwavOnFirma).nwavConPalabrasClave("firma rubrica"));
    }
}
