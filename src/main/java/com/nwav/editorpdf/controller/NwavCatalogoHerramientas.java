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

@Component
@RequiredArgsConstructor
public class NwavCatalogoHerramientas {
    public static final String NWAV_COMPRIMIR = "comprimir";
    public static final String NWAV_OCR = "ocr";
    public static final String NWAV_REPARAR = "reparar";

    private final NwavCompresorService nwavCompresor;
    private final NwavOcrService nwavOcr;
    private final NwavReparacionService nwavReparacion;
    private final com.nwav.editorpdf.service.NwavOrganizarService nwavOrganizar;
    private final NwavDependenciasService nwavDependencias;
    private final com.nwav.editorpdf.service.NwavConversionAPdfService nwavAPdf;
    private final com.nwav.editorpdf.service.NwavConversionDesdePdfService nwavDesdePdf;
    private final com.nwav.editorpdf.service.NwavPdfAService nwavPdfA;
    private final com.nwav.editorpdf.service.NwavEdicionLoteService nwavLote;
    private final com.nwav.editorpdf.service.NwavProteccionLoteService nwavProteccion;
    private final com.nwav.editorpdf.service.NwavFirmaDigitalService nwavFirmaDigital;
    private final com.nwav.editorpdf.service.NwavRedaccionService nwavRedaccion;
    private final com.nwav.editorpdf.service.NwavPdfDocumentoService nwavDocumentosPdf;
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
        nwavAgregarAPdf(nwavLista);
        nwavAgregarDesdePdf(nwavLista);
        nwavAgregarEditar(nwavLista);
        nwavAgregarSeguridad(nwavLista);
        return nwavLista;
    }

    private void nwavAgregarOrganizar(List<NwavDefinicionHerramienta> nwavLista) {
        nwavLista.add(new NwavDefinicionHerramienta("unir", "Unir PDF",
                "Combina varios PDF en uno, en el orden de la lista.", NwavCategoriaHerramienta.NWAV_ORGANIZAR,
                Feather.GIT_MERGE)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_UNIR, false, nwavEntrada -> NwavResultadoProceso.nwavDe(
                        nwavOrganizar.nwavUnir(nwavEntrada.nwavArchivos(), nwavEntrada.nwavContrasenas(),
                                NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(), nwavEntrada.nwavCarpetaSalida(),
                                        "unido", "pdf"), nwavEntrada.nwavAvance(), nwavEntrada.nwavCancelacion()),
                        nwavEntrada.nwavArchivos().size() + " archivos unidos")))
                .nwavCombina(2).nwavConPalabrasClave("juntar combinar fusionar"));

        nwavLista.add(new NwavDefinicionHerramienta("dividir", "Dividir PDF",
                "Separa un PDF en varios: por rangos, cada N páginas o por tamaño.", NwavCategoriaHerramienta.NWAV_ORGANIZAR,
                Feather.SCISSORS)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_DIVIDIR, true, this::nwavProcesoDividir))
                .nwavConOpciones(
                        NwavCampoOpcion.nwavEleccion("modo", "Cómo dividir", "rangos", "rangos=Por rangos (un archivo por rango)",
                                "cada=Cada N páginas", "tamano=Por tamaño máximo"),
                        NwavCampoOpcion.nwavTexto("rangos", "Rangos", "1-3, 4-").nwavSi("modo", "rangos")
                                .nwavConAyuda("Ej.: 1-3, 4-6, 7- (cada rango es un archivo)."),
                        NwavCampoOpcion.nwavEntero("cada", "Páginas por archivo", 10, 1, 10_000).nwavSi("modo", "cada"),
                        NwavCampoOpcion.nwavDecimal("tamano", "Tamaño máximo por archivo (MB)", 5, 0.1, 2000, 0.5)
                                .nwavSi("modo", "tamano").nwavConAyuda("Aproximado: las partes quedan por debajo del límite."))
                .nwavConPalabrasClave("separar partir trocear"));

        nwavLista.add(new NwavDefinicionHerramienta("eliminar", "Eliminar páginas",
                "Quita las páginas que no necesitas.", NwavCategoriaHerramienta.NWAV_ORGANIZAR, Feather.TRASH_2)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_ELIMINAR, true, nwavEntrada -> {
                    String nwavRangos = nwavEntrada.nwavTexto("paginas", "");
                    return NwavResultadoProceso.nwavDe(nwavOrganizar.nwavEliminarPaginas(nwavEntrada.nwavArchivo(),
                            nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()), nwavRangos,
                            NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(), nwavEntrada.nwavCarpetaSalida(),
                                    "sin_paginas", "pdf")), "Páginas eliminadas: " + nwavRangos);
                }))
                .nwavConOpciones(NwavCampoOpcion.nwavTexto("paginas", "Páginas a eliminar", "")
                        .nwavConAyuda("Ej.: 2, 5-7, 10-"))
                .nwavConPalabrasClave("borrar quitar"));

        nwavLista.add(new NwavDefinicionHerramienta("extraer", "Extraer páginas",
                "Copia las páginas que elijas a un PDF nuevo (o una por archivo).", NwavCategoriaHerramienta.NWAV_ORGANIZAR,
                Feather.EXTERNAL_LINK)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_EXTRAER, true, nwavEntrada -> {
                    String nwavRangos = nwavEntrada.nwavTexto("paginas", "1");
                    List<Path> nwavCreados = nwavOrganizar.nwavExtraerPaginas(nwavEntrada.nwavArchivo(),
                            nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()), nwavRangos,
                            nwavEntrada.nwavSiNo("porPagina", false), nwavEntrada.nwavCarpetaSalida(),
                            nwavEntrada.nwavCancelacion());
                    String nwavResumen = "Páginas " + nwavRangos + " → " + nwavCreados.size()
                            + (nwavCreados.size() == 1 ? " archivo" : " archivos");
                    return new NwavResultadoProceso(nwavCreados, nwavResumen, nwavResumen);
                }))
                .nwavConOpciones(NwavCampoOpcion.nwavTexto("paginas", "Páginas a extraer", "1-3")
                                .nwavConAyuda("Ej.: 1-3, 5"),
                        NwavCampoOpcion.nwavCasilla("porPagina", "Crear un PDF por cada página", false)));

        nwavLista.add(new NwavDefinicionHerramienta("ordenar", "Ordenar páginas",
                "Cambia el orden de las páginas con un patrón o invierte el documento.",
                NwavCategoriaHerramienta.NWAV_ORGANIZAR, Feather.SHUFFLE)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_REORDENAR, true, nwavEntrada -> {
                    boolean nwavInvertir = "invertir".equals(nwavEntrada.nwavTexto("modo", "orden"));
                    String nwavOrden = nwavEntrada.nwavTexto("orden", "");
                    return NwavResultadoProceso.nwavDe(nwavOrganizar.nwavOrdenar(nwavEntrada.nwavArchivo(),
                                    nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()), nwavOrden, nwavInvertir,
                                    NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(),
                                            nwavEntrada.nwavCarpetaSalida(), "ordenado", "pdf")),
                            nwavInvertir ? "Orden invertido" : "Nuevo orden: " + nwavOrden);
                }))
                .nwavConOpciones(
                        NwavCampoOpcion.nwavEleccion("modo", "Orden", "orden", "orden=Orden personalizado",
                                "invertir=Invertir (de la última a la primera)"),
                        NwavCampoOpcion.nwavTexto("orden", "Nuevo orden", "1-").nwavSi("modo", "orden")
                                .nwavConAyuda("Ej.: 3, 1, 2, 4- (las páginas que no nombres se quitan)."))
                .nwavConPalabrasClave("reordenar invertir orden"));

        nwavLista.add(new NwavDefinicionHerramienta("rotar", "Rotar PDF",
                "Gira las páginas de uno o varios documentos a la vez.", NwavCategoriaHerramienta.NWAV_ORGANIZAR,
                Feather.ROTATE_CW)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_ROTAR, true, nwavEntrada -> {
                    int nwavGrados = nwavEntrada.nwavEntero("grados", 90);
                    return NwavResultadoProceso.nwavDe(nwavOrganizar.nwavRotar(nwavEntrada.nwavArchivo(),
                            nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()), nwavGrados,
                            nwavEntrada.nwavTexto("paginas", ""), NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(),
                                    nwavEntrada.nwavCarpetaSalida(), "rotado", "pdf")), "Rotado " + nwavGrados + "°");
                }))
                .nwavConOpciones(
                        NwavCampoOpcion.nwavEleccion("grados", "Giro", "90", "90=90° a la derecha", "270=90° a la izquierda",
                                "180=180°"),
                        NwavCampoOpcion.nwavTexto("paginas", "Páginas (vacío = todas)", ""))
                .nwavConPalabrasClave("girar orientacion"));

        nwavLista.add(new NwavDefinicionHerramienta("recortar", "Recortar PDF",
                "Recorta los márgenes de las páginas (el contenido recortado deja de verse).",
                NwavCategoriaHerramienta.NWAV_ORGANIZAR, Feather.CROP)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_RECORTAR, true, nwavEntrada ->
                        NwavResultadoProceso.nwavDe(nwavOrganizar.nwavRecortarMargenes(nwavEntrada.nwavArchivo(),
                                nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()), nwavEntrada.nwavDecimal("arriba", 10),
                                nwavEntrada.nwavDecimal("derecha", 10), nwavEntrada.nwavDecimal("abajo", 10),
                                nwavEntrada.nwavDecimal("izquierda", 10), nwavEntrada.nwavTexto("paginas", ""),
                                NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(), nwavEntrada.nwavCarpetaSalida(),
                                        "recortado", "pdf")), "Márgenes recortados")))
                .nwavConOpciones(
                        NwavCampoOpcion.nwavDecimal("arriba", "Margen superior (mm)", 10, 0, 300, 1),
                        NwavCampoOpcion.nwavDecimal("derecha", "Margen derecho (mm)", 10, 0, 300, 1),
                        NwavCampoOpcion.nwavDecimal("abajo", "Margen inferior (mm)", 10, 0, 300, 1),
                        NwavCampoOpcion.nwavDecimal("izquierda", "Margen izquierdo (mm)", 10, 0, 300, 1),
                        NwavCampoOpcion.nwavTexto("paginas", "Páginas (vacío = todas)", ""))
                .nwavConPalabrasClave("margenes cortar"));

        nwavLista.add(new NwavDefinicionHerramienta("recortar-raton", "Recortar con el ratón",
                "Marca en la página el área que quieres conservar.", NwavCategoriaHerramienta.NWAV_ORGANIZAR, Feather.CROP)
                .nwavEditor(() -> nwavVisor.nwavHerramientas().nwavHerramientaProperty().set(NwavHerramienta.NWAV_RECORTAR))
                .nwavConPalabrasClave("area seleccion recortar"));

        nwavLista.add(new NwavDefinicionHerramienta("organizar", "Organizar visualmente",
                "Reordena, rota, duplica o elimina páginas arrastrando las miniaturas (con deshacer).",
                NwavCategoriaHerramienta.NWAV_ORGANIZAR, Feather.LAYERS).nwavEditor(() -> {
                    nwavVisor.nwavMiniaturasVisiblesProperty().set(true);
                    nwavNotificador.nwavInfo("Arrastra las miniaturas para reordenar; clic derecho para rotar o eliminar.");
                }).nwavConPalabrasClave("ordenar rotar eliminar borrar paginas miniaturas"));
    }

    private NwavResultadoProceso nwavProcesoDividir(NwavEntradaProceso nwavEntrada) {
        Path nwavArchivo = nwavEntrada.nwavArchivo();
        String nwavClave = nwavEntrada.nwavContrasena(nwavArchivo);
        String nwavModo = nwavEntrada.nwavTexto("modo", "rangos");
        List<Path> nwavPartes = switch (nwavModo) {
            case "cada" -> nwavOrganizar.nwavDividirCada(nwavArchivo, nwavClave, nwavEntrada.nwavEntero("cada", 10),
                    nwavEntrada.nwavCarpetaSalida(), nwavEntrada.nwavAvance(), nwavEntrada.nwavCancelacion());
            case "tamano" -> nwavOrganizar.nwavDividirPorTamano(nwavArchivo, nwavClave,
                    (long) (nwavEntrada.nwavDecimal("tamano", 5) * 1024 * 1024), nwavEntrada.nwavCarpetaSalida(),
                    nwavEntrada.nwavAvance(), nwavEntrada.nwavCancelacion());
            default -> nwavOrganizar.nwavDividirPorRangos(nwavArchivo, nwavClave, nwavEntrada.nwavTexto("rangos", "1-"),
                    nwavEntrada.nwavCarpetaSalida(), nwavEntrada.nwavAvance(), nwavEntrada.nwavCancelacion());
        };
        String nwavResumen = nwavPartes.size() + (nwavPartes.size() == 1 ? " parte creada" : " partes creadas");
        return new NwavResultadoProceso(nwavPartes, nwavResumen, nwavResumen + " (" + nwavModo + ")");
    }

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

    private void nwavAgregarAPdf(List<NwavDefinicionHerramienta> nwavLista) {
        String[] nwavImagenes = com.nwav.editorpdf.service.NwavConversionAPdfService.NWAV_EXTENSIONES_IMAGEN
                .toArray(String[]::new);
        nwavLista.add(new NwavDefinicionHerramienta("imagenes-pdf", "Imágenes a PDF",
                "Convierte JPG, PNG, TIFF… en un PDF, con tamaño de hoja, orientación y márgenes.",
                NwavCategoriaHerramienta.NWAV_CONVERTIR_A_PDF, Feather.IMAGE)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_IMAGENES_A_PDF, false, this::nwavProcesoImagenes))
                .nwavConExtensiones(nwavImagenes).nwavCombina(1)
                .nwavConOpciones(nwavOpcionesHoja())
                .nwavConPalabrasClave("jpg png foto fotos tiff convertir"));

        nwavLista.add(new NwavDefinicionHerramienta("escaneos-pdf", "Escaneos a PDF",
                "Importa una carpeta de escaneos (en orden de nombre) y crea un único PDF.",
                NwavCategoriaHerramienta.NWAV_CONVERTIR_A_PDF, Feather.PRINTER)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_IMAGENES_A_PDF, false, this::nwavProcesoImagenes))
                .nwavConExtensiones(nwavImagenes).nwavCombina(1)
                .nwavConOpciones(
                        NwavCampoOpcion.nwavEleccion("hoja", "Tamaño de hoja", "NWAV_A4", "NWAV_A4=A4", "NWAV_CARTA=Carta",
                                "NWAV_AJUSTAR=Igual que cada escaneo"),
                        NwavCampoOpcion.nwavEleccion("orientacion", "Orientación", "NWAV_AUTO", "NWAV_AUTO=Automática",
                                "NWAV_VERTICAL=Vertical", "NWAV_HORIZONTAL=Horizontal"),
                        NwavCampoOpcion.nwavDecimal("margen", "Margen (mm)", 0, 0, 60, 1))
                .nwavConAviso("Pulsa «Carpeta…» o arrastra la carpeta del escáner: se añaden sus imágenes por orden de nombre. "
                        + "Después puedes aplicar OCR para poder buscar el texto.")
                .nwavOfrecer(NWAV_OCR, "Aplicar OCR")
                .nwavConPalabrasClave("escaner escanear carpeta"));

        nwavLista.add(new NwavDefinicionHerramienta("office-pdf", "Office a PDF",
                "Word, Excel, PowerPoint y OpenDocument a PDF, usando LibreOffice en tu equipo.",
                NwavCategoriaHerramienta.NWAV_CONVERTIR_A_PDF, Feather.FILE_TEXT)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_OFFICE_A_PDF, false, this::nwavProcesoOffice))
                .nwavConExtensiones(com.nwav.editorpdf.service.NwavConversionAPdfService.NWAV_EXTENSIONES_OFFICE
                        .toArray(String[]::new))
                .nwavRequiere(nwavDependencias::nwavLibreOffice)
                .nwavConPalabrasClave("word excel powerpoint docx xlsx pptx odt libreoffice"));

        nwavLista.add(new NwavDefinicionHerramienta("html-pdf", "HTML a PDF",
                "Convierte archivos HTML guardados en tu equipo.", NwavCategoriaHerramienta.NWAV_CONVERTIR_A_PDF, Feather.CODE)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_HTML_A_PDF, true, nwavEntrada ->
                        NwavResultadoProceso.nwavDe(nwavAPdf.nwavHtmlAPdf(nwavEntrada.nwavArchivo(), nwavOpcionesHtml(nwavEntrada),
                                NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(), nwavEntrada.nwavCarpetaSalida(),
                                        "", "pdf")), "HTML convertido")))
                .nwavConExtensiones("html", "htm", "xhtml")
                .nwavConOpciones(nwavOpcionesPaginaHtml())
                .nwavConPalabrasClave("web pagina"));

        nwavLista.add(new NwavDefinicionHerramienta("url-pdf", "Página web a PDF",
                "Descarga una página por su dirección y la guarda como PDF.", NwavCategoriaHerramienta.NWAV_CONVERTIR_A_PDF,
                Feather.GLOBE)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_HTML_A_PDF, false, nwavEntrada -> {
                    String nwavUrl = nwavEntrada.nwavTexto("url", "");
                    if (nwavUrl.isBlank()) {
                        throw new com.nwav.editorpdf.exception.NwavEditorException("Escribe la dirección de la página.");
                    }
                    Path nwavCarpeta = nwavEntrada.nwavCarpetaSalida() != null ? nwavEntrada.nwavCarpetaSalida()
                            : nwavCarpetaPorDefecto();
                    Path nwavDestino = NwavEscrituraSegura.nwavSinColision(nwavCarpeta.resolve(
                            com.nwav.editorpdf.service.NwavConversionAPdfService.nwavNombreParaUrl(nwavUrl)), null);
                    return NwavResultadoProceso.nwavDe(nwavAPdf.nwavUrlAPdf(nwavUrl, nwavOpcionesHtml(nwavEntrada), nwavDestino),
                            "Página guardada: " + nwavUrl);
                }))
                .nwavSinArchivos()
                .nwavConOpciones(nwavConUrl(nwavOpcionesPaginaHtml()))
                .nwavConAviso("No necesita archivos: escribe la dirección en «Opciones» y pulsa Procesar. Esta herramienta "
                        + "SÍ usa Internet para descargar la página. Las páginas que se construyen con JavaScript "
                        + "pueden salir incompletas. Si no eliges carpeta, se guarda en Descargas.")
                .nwavConPalabrasClave("url web internet enlace"));
    }

    private static NwavCampoOpcion[] nwavConUrl(NwavCampoOpcion[] nwavResto) {
        NwavCampoOpcion[] nwavTodos = new NwavCampoOpcion[nwavResto.length + 1];
        nwavTodos[0] = NwavCampoOpcion.nwavTexto("url", "Dirección (URL)", "https://");
        System.arraycopy(nwavResto, 0, nwavTodos, 1, nwavResto.length);
        return nwavTodos;
    }

    private static Path nwavCarpetaPorDefecto() {
        Path nwavDescargas = Path.of(System.getProperty("user.home"), "Downloads");
        return java.nio.file.Files.isDirectory(nwavDescargas) ? nwavDescargas : Path.of(System.getProperty("user.home"));
    }

    private static NwavCampoOpcion[] nwavOpcionesHoja() {
        return new NwavCampoOpcion[]{
                NwavCampoOpcion.nwavEleccion("hoja", "Tamaño de hoja", "NWAV_A4", "NWAV_A4=A4", "NWAV_CARTA=Carta",
                        "NWAV_AJUSTAR=Igual que la imagen"),
                NwavCampoOpcion.nwavEleccion("orientacion", "Orientación", "NWAV_AUTO", "NWAV_AUTO=Según la imagen",
                        "NWAV_VERTICAL=Vertical", "NWAV_HORIZONTAL=Horizontal"),
                NwavCampoOpcion.nwavDecimal("margen", "Margen (mm)", 10, 0, 60, 1),
                NwavCampoOpcion.nwavEleccion("porPagina", "Imágenes por página", "1", "1=Una", "2=Dos", "4=Cuatro"),
                NwavCampoOpcion.nwavCasilla("separados", "Un PDF por imagen (en lugar de uno solo)", false)};
    }

    private static NwavCampoOpcion[] nwavOpcionesPaginaHtml() {
        return new NwavCampoOpcion[]{
                NwavCampoOpcion.nwavEleccion("hoja", "Tamaño de hoja", "A4", "A4=A4", "letter=Carta"),
                NwavCampoOpcion.nwavCasilla("horizontal", "Horizontal", false),
                NwavCampoOpcion.nwavDecimal("margen", "Margen (mm)", 15, 0, 60, 1),
                NwavCampoOpcion.nwavCasilla("fondos", "Imprimir colores e imágenes de fondo", true)};
    }

    private static com.nwav.editorpdf.service.NwavConversionAPdfService.NwavOpcionesHtml nwavOpcionesHtml(
            NwavEntradaProceso nwavEntrada) {
        return new com.nwav.editorpdf.service.NwavConversionAPdfService.NwavOpcionesHtml(nwavEntrada.nwavTexto("hoja", "A4"),
                nwavEntrada.nwavSiNo("horizontal", false), nwavEntrada.nwavDecimal("margen", 15),
                nwavEntrada.nwavSiNo("fondos", true));
    }

    private NwavResultadoProceso nwavProcesoImagenes(NwavEntradaProceso nwavEntrada) {
        var nwavOpciones = new com.nwav.editorpdf.service.NwavConversionAPdfService.NwavOpcionesImagenes(
                com.nwav.editorpdf.service.NwavConversionAPdfService.NwavTamanoHoja.valueOf(
                        nwavEntrada.nwavTexto("hoja", "NWAV_A4")),
                com.nwav.editorpdf.service.NwavConversionAPdfService.NwavOrientacion.valueOf(
                        nwavEntrada.nwavTexto("orientacion", "NWAV_AUTO")),
                nwavEntrada.nwavDecimal("margen", 10), nwavEntrada.nwavEntero("porPagina", 1));
        List<Path> nwavImagenes = nwavEntrada.nwavArchivos();
        if (nwavEntrada.nwavSiNo("separados", false)) {
            List<Path> nwavCreados = new ArrayList<>();
            for (int nwavIndice = 0; nwavIndice < nwavImagenes.size(); nwavIndice++) {
                Path nwavImagen = nwavImagenes.get(nwavIndice);
                nwavCreados.add(nwavAPdf.nwavImagenesAPdf(List.of(nwavImagen), nwavOpciones,
                        NwavEscrituraSegura.nwavRutaSalida(nwavImagen, nwavEntrada.nwavCarpetaSalida(), "", "pdf"),
                        com.nwav.editorpdf.service.NwavAvance.NWAV_NINGUNO, nwavEntrada.nwavCancelacion()));
                nwavEntrada.nwavAvance().nwavInformar(nwavIndice + 1, nwavImagenes.size(), null);
            }
            String nwavResumen = nwavCreados.size() + " PDF creados";
            return new NwavResultadoProceso(nwavCreados, nwavResumen, nwavResumen);
        }
        Path nwavDestino = NwavEscrituraSegura.nwavRutaSalida(nwavImagenes.get(0), nwavEntrada.nwavCarpetaSalida(),
                nwavImagenes.size() > 1 ? "imagenes" : "", "pdf");
        return NwavResultadoProceso.nwavDe(nwavAPdf.nwavImagenesAPdf(nwavImagenes, nwavOpciones, nwavDestino,
                nwavEntrada.nwavAvance(), nwavEntrada.nwavCancelacion()), nwavImagenes.size() + " imágenes en un PDF");
    }

    private NwavResultadoProceso nwavProcesoOffice(NwavEntradaProceso nwavEntrada) {
        var nwavResultados = nwavAPdf.nwavOfficeAPdf(nwavEntrada.nwavArchivos(), nwavEntrada.nwavCarpetaSalida(),
                nwavEntrada.nwavAvance(), nwavEntrada.nwavCancelacion());
        List<Path> nwavCreados = nwavResultados.stream().filter(nwavR -> nwavR.nwavPdf() != null)
                .map(com.nwav.editorpdf.service.NwavConversionAPdfService.NwavConvertido::nwavPdf).toList();
        List<String> nwavFallos = nwavResultados.stream().filter(nwavR -> nwavR.nwavError() != null)
                .map(nwavR -> nwavR.nwavOrigen().getFileName() + ": " + nwavR.nwavError().getMessage()).toList();
        if (nwavCreados.isEmpty()) {
            throw new com.nwav.editorpdf.exception.NwavEditorException("No se pudo convertir ningún documento. "
                    + String.join(" · ", nwavFallos));
        }
        String nwavResumen = nwavCreados.size() + " de " + nwavResultados.size() + " documentos convertidos"
                + (nwavFallos.isEmpty() ? "" : ". Fallaron: " + String.join(" · ", nwavFallos));
        return new NwavResultadoProceso(nwavCreados, nwavResumen, nwavResumen);
    }

    private void nwavAgregarDesdePdf(List<NwavDefinicionHerramienta> nwavLista) {
        nwavLista.add(new NwavDefinicionHerramienta("pdf-imagen", "PDF a imagen",
                "Convierte cada página en una imagen JPG o PNG con la resolución que elijas.",
                NwavCategoriaHerramienta.NWAV_CONVERTIR_DESDE_PDF, Feather.IMAGE)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_PDF_A_IMAGEN, true, nwavEntrada -> {
                    List<Path> nwavCreadas = nwavDesdePdf.nwavAImagenes(nwavEntrada.nwavArchivo(),
                            nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()), nwavEntrada.nwavTexto("formato", "jpg"),
                            nwavEntrada.nwavEntero("dpi", 150), (float) nwavEntrada.nwavDecimal("calidad", 0.9),
                            nwavEntrada.nwavTexto("paginas", ""), nwavEntrada.nwavCarpetaSalida(), nwavEntrada.nwavAvance(),
                            nwavEntrada.nwavCancelacion());
                    String nwavResumen = nwavCreadas.size() + " imágenes " + nwavEntrada.nwavTexto("formato", "jpg")
                            .toUpperCase(java.util.Locale.ROOT) + " a " + nwavEntrada.nwavEntero("dpi", 150) + " ppp";
                    return new NwavResultadoProceso(nwavCreadas, nwavResumen, nwavResumen);
                }))
                .nwavConOpciones(
                        NwavCampoOpcion.nwavEleccion("formato", "Formato", "jpg", "jpg=JPG (fotos, menor tamaño)",
                                "png=PNG (sin pérdida, admite transparencia)"),
                        NwavCampoOpcion.nwavEleccion("dpi", "Resolución", "150", "72=72 ppp (pantalla)", "150=150 ppp (normal)",
                                "300=300 ppp (impresión)", "600=600 ppp (máxima)"),
                        NwavCampoOpcion.nwavDecimal("calidad", "Calidad JPG", 0.9, 0.3, 1, 0.05).nwavSi("formato", "jpg"),
                        NwavCampoOpcion.nwavTexto("paginas", "Páginas (vacío = todas)", ""))
                .nwavConPalabrasClave("jpg png jpeg foto convertir"));

        nwavLista.add(new NwavDefinicionHerramienta("extraer-imagenes", "Extraer imágenes",
                "Guarda las imágenes incrustadas en el PDF tal como están, sin recomprimirlas.",
                NwavCategoriaHerramienta.NWAV_CONVERTIR_DESDE_PDF, Feather.DOWNLOAD)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_EXTRAER_IMAGENES, true, nwavEntrada -> {
                    List<Path> nwavCreadas = nwavDesdePdf.nwavExtraerImagenes(nwavEntrada.nwavArchivo(),
                            nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()), nwavEntrada.nwavEntero("minimo", 50),
                            nwavEntrada.nwavCarpetaSalida(), nwavEntrada.nwavAvance(), nwavEntrada.nwavCancelacion());
                    String nwavResumen = nwavCreadas.size() + " imágenes extraídas";
                    return new NwavResultadoProceso(nwavCreadas, nwavResumen, nwavResumen);
                }))
                .nwavConOpciones(NwavCampoOpcion.nwavEntero("minimo", "Ignorar imágenes menores de (px)", 50, 1, 2000)
                        .nwavConAyuda("Evita iconos, líneas y adornos."))
                .nwavConPalabrasClave("fotos imagenes sacar"));

        nwavLista.add(new NwavDefinicionHerramienta("pdf-word", "PDF a Word",
                "Crea un documento .docx editable con títulos, listas, tablas, enlaces e imágenes.",
                NwavCategoriaHerramienta.NWAV_CONVERTIR_DESDE_PDF, Feather.FILE_TEXT)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_PDF_A_OFFICE, true, nwavEntrada ->
                        NwavResultadoProceso.nwavDe(nwavDesdePdf.nwavAWord(nwavEntrada.nwavArchivo(),
                                nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()), nwavEntrada.nwavSiNo("imagenes", true),
                                NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(), nwavEntrada.nwavCarpetaSalida(),
                                        "", "docx"), nwavEntrada.nwavAvance(), nwavEntrada.nwavCancelacion()),
                                "Convertido a Word")))
                .nwavConOpciones(NwavCampoOpcion.nwavCasilla("imagenes", "Incluir las imágenes", true))
                .nwavConAviso(com.nwav.editorpdf.service.NwavConversionDesdePdfService.NWAV_AVISO_OFFICE
                        + " Si el PDF es un escaneo, aplica antes OCR.")
                .nwavConPalabrasClave("docx doc word editar office"));

        nwavLista.add(new NwavDefinicionHerramienta("pdf-excel", "PDF a Excel",
                "Pasa las tablas del PDF a una hoja de cálculo .xlsx, con los números como números.",
                NwavCategoriaHerramienta.NWAV_CONVERTIR_DESDE_PDF, Feather.GRID)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_PDF_A_OFFICE, true, nwavEntrada ->
                        NwavResultadoProceso.nwavDe(nwavDesdePdf.nwavAExcel(nwavEntrada.nwavArchivo(),
                                nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()), nwavEntrada.nwavSiNo("unaHoja", false),
                                nwavEntrada.nwavSiNo("soloTablas", true),
                                NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(), nwavEntrada.nwavCarpetaSalida(),
                                        "", "xlsx"), nwavEntrada.nwavAvance(), nwavEntrada.nwavCancelacion()),
                                "Convertido a Excel")))
                .nwavConOpciones(NwavCampoOpcion.nwavCasilla("soloTablas", "Solo las tablas (sin el resto del texto)", true),
                        NwavCampoOpcion.nwavCasilla("unaHoja", "Todo en una sola hoja (si no, una hoja por página)", false))
                .nwavConAviso("Las tablas se detectan por la alineación del texto en columnas. Revisa el resultado: "
                        + "las celdas combinadas o las tablas sin separación clara pueden necesitar ajustes.")
                .nwavConPalabrasClave("xlsx xls hoja calculo tabla tablas office"));

        nwavLista.add(new NwavDefinicionHerramienta("pdf-powerpoint", "PDF a PowerPoint",
                "Una diapositiva por página, con el texto editable sobre el diseño original.",
                NwavCategoriaHerramienta.NWAV_CONVERTIR_DESDE_PDF, Feather.MONITOR)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_PDF_A_OFFICE, true, nwavEntrada ->
                        NwavResultadoProceso.nwavDe(nwavDesdePdf.nwavAPowerPoint(nwavEntrada.nwavArchivo(),
                                nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()),
                                "editable".equals(nwavEntrada.nwavTexto("modo", "editable")),
                                NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(), nwavEntrada.nwavCarpetaSalida(),
                                        "", "pptx"), nwavEntrada.nwavAvance(), nwavEntrada.nwavCancelacion()),
                                "Convertido a PowerPoint")))
                .nwavConOpciones(NwavCampoOpcion.nwavEleccion("modo", "Diapositivas", "editable",
                        "editable=Texto editable sobre el diseño", "imagen=Imagen de cada página (idéntica, no editable)"))
                .nwavConAviso(com.nwav.editorpdf.service.NwavConversionDesdePdfService.NWAV_AVISO_OFFICE)
                .nwavConPalabrasClave("pptx ppt presentacion diapositivas office"));

        nwavLista.add(new NwavDefinicionHerramienta("pdf-texto", "PDF a texto o Markdown",
                "Extrae el texto; en Markdown conserva títulos, listas, tablas y enlaces.",
                NwavCategoriaHerramienta.NWAV_CONVERTIR_DESDE_PDF, Feather.ALIGN_LEFT)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_PDF_A_TEXTO, true, nwavEntrada -> {
                    boolean nwavMd = "md".equals(nwavEntrada.nwavTexto("formato", "md"));
                    Path nwavDestino = NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(),
                            nwavEntrada.nwavCarpetaSalida(), "", nwavMd ? "md" : "txt");
                    String nwavClave = nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo());
                    return NwavResultadoProceso.nwavDe(nwavMd
                            ? nwavDesdePdf.nwavAMarkdown(nwavEntrada.nwavArchivo(), nwavClave, nwavDestino,
                            nwavEntrada.nwavCancelacion())
                            : nwavDesdePdf.nwavATexto(nwavEntrada.nwavArchivo(), nwavClave, nwavDestino),
                            nwavMd ? "Convertido a Markdown" : "Texto extraído");
                }))
                .nwavConOpciones(NwavCampoOpcion.nwavEleccion("formato", "Formato", "md", "md=Markdown (.md)",
                        "txt=Texto plano (.txt)"))
                .nwavConPalabrasClave("txt md markdown extraer texto"));

        nwavLista.add(new NwavDefinicionHerramienta("pdf-a", "PDF a PDF/A",
                "Convierte a PDF/A-1b para archivo a largo plazo y lo valida.",
                NwavCategoriaHerramienta.NWAV_CONVERTIR_DESDE_PDF, Feather.ARCHIVE)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_PDF_A, true, nwavEntrada -> {
                    var nwavResultado = nwavPdfA.nwavConvertir(nwavEntrada.nwavArchivo(),
                            nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()),
                            "rasterizar".equals(nwavEntrada.nwavTexto("modo", "conservar")),
                            NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(), nwavEntrada.nwavCarpetaSalida(),
                                    "pdfa", "pdf"), nwavEntrada.nwavAvance(), nwavEntrada.nwavCancelacion());
                    if (!nwavResultado.nwavValido()) {
                        throw new com.nwav.editorpdf.exception.NwavEditorException(nwavResultado.nwavResumen()
                                + ". Elige «Si no cumple: páginas como imagen» para obtener igualmente un PDF/A.");
                    }
                    return NwavResultadoProceso.nwavDe(nwavResultado.nwavDestino(), nwavResultado.nwavResumen());
                }))
                .nwavConOpciones(NwavCampoOpcion.nwavEleccion("modo", "Si el documento no cumple", "conservar",
                        "conservar=Explicar los motivos y no guardar",
                        "rasterizar=Rehacer las páginas como imagen (pierde el texto seleccionable)"))
                .nwavConAviso("Se valida con Apache PDFBox Preflight (PDF/A-1b). Si las páginas se rehacen como imagen, "
                        + "puedes aplicar después OCR, aunque el resultado del OCR ya no será PDF/A.")
                .nwavConPalabrasClave("pdfa archivo largo plazo preflight validar"));
    }

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

        nwavLista.add(new NwavDefinicionHerramienta("crear-formulario", "Crear formulario",
                "Añade campos rellenables: texto, casillas, listas, opciones y firma.", NwavCategoriaHerramienta.NWAV_EDITAR,
                Feather.PLUS_SQUARE)
                .nwavEditor(() -> nwavVisor.nwavHerramientas().nwavHerramientaProperty().set(NwavHerramienta.NWAV_CAMPO))
                .nwavConPalabrasClave("campos rellenable formulario casilla lista firma acroform"));

        nwavLista.add(new NwavDefinicionHerramienta("marca-agua-lote", "Marca de agua en varios PDF",
                "El mismo texto en todos los archivos: centrado, en una esquina o en mosaico.",
                NwavCategoriaHerramienta.NWAV_EDITAR, Feather.DROPLET)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_MARCA_AGUA, true, nwavEntrada -> {
                    var nwavOpciones = new com.nwav.editorpdf.model.NwavOpcionesMarcaAgua(
                            com.nwav.editorpdf.model.NwavTipoMarcaAgua.NWAV_TEXTO, nwavEntrada.nwavTexto("texto", "BORRADOR"),
                            null, com.nwav.editorpdf.model.NwavFuente.NWAV_HELVETICA, nwavEntrada.nwavDecimal("tamano", 48),
                            nwavColor(nwavEntrada.nwavTexto("color", "#9CA3AF")), nwavEntrada.nwavDecimal("opacidad", 0.3),
                            nwavEntrada.nwavDecimal("giro", 45), 0.4, nwavEntrada.nwavTexto("paginas", ""),
                            com.nwav.editorpdf.model.NwavPosicionMarca.valueOf(nwavEntrada.nwavTexto("posicion", "NWAV_CENTRO")));
                    return NwavResultadoProceso.nwavDe(nwavLote.nwavMarcaAgua(nwavEntrada.nwavArchivo(),
                            nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()), nwavOpciones,
                            NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(), nwavEntrada.nwavCarpetaSalida(),
                                    "marca", "pdf")), "Marca «" + nwavOpciones.nwavTexto() + "»");
                }))
                .nwavConOpciones(
                        NwavCampoOpcion.nwavTexto("texto", "Texto", "BORRADOR"),
                        NwavCampoOpcion.nwavEleccion("posicion", "Posición", "NWAV_CENTRO",
                                Arrays.stream(com.nwav.editorpdf.model.NwavPosicionMarca.values())
                                        .map(nwavP -> nwavP.name() + "=" + nwavP.getNwavDescripcion()).toArray(String[]::new)),
                        NwavCampoOpcion.nwavDecimal("tamano", "Tamaño de letra", 48, 8, 200, 2),
                        NwavCampoOpcion.nwavColor("color", "Color", "#9CA3AF"),
                        NwavCampoOpcion.nwavDecimal("opacidad", "Opacidad", 0.3, 0.05, 1, 0.05),
                        NwavCampoOpcion.nwavDecimal("giro", "Giro (grados)", 45, -90, 90, 5),
                        NwavCampoOpcion.nwavTexto("paginas", "Páginas (vacío = todas)", ""))
                .nwavConPalabrasClave("sello confidencial borrador lote varios mosaico"));

        nwavLista.add(new NwavDefinicionHerramienta("numeros-lote", "Numerar varios PDF",
                "Añade «Página n de total» (o tu formato) a muchos archivos a la vez.", NwavCategoriaHerramienta.NWAV_EDITAR,
                Feather.HASH)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_ENCABEZADO, true, nwavEntrada -> {
                    String nwavPosicion = nwavEntrada.nwavTexto("posicion", "pie");
                    String nwavFormato = nwavEntrada.nwavTexto("formato", "Página {n} de {total}");
                    var nwavAlineacion = com.nwav.editorpdf.model.NwavAlineacion.valueOf(
                            nwavEntrada.nwavTexto("alineacion", "NWAV_CENTRO"));
                    var nwavOpciones = new com.nwav.editorpdf.model.NwavOpcionesEncabezado(
                            "encabezado".equals(nwavPosicion) ? nwavFormato : "", nwavAlineacion,
                            "pie".equals(nwavPosicion) ? nwavFormato : "", nwavAlineacion,
                            com.nwav.editorpdf.model.NwavFuente.NWAV_HELVETICA, nwavEntrada.nwavDecimal("tamano", 10), null,
                            nwavEntrada.nwavDecimal("margen", 28), nwavEntrada.nwavEntero("inicio", 1),
                            nwavEntrada.nwavTexto("paginas", ""));
                    return NwavResultadoProceso.nwavDe(nwavLote.nwavEncabezado(nwavEntrada.nwavArchivo(),
                            nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()), nwavOpciones,
                            NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(), nwavEntrada.nwavCarpetaSalida(),
                                    "numerado", "pdf")), "Numerado: " + nwavFormato);
                }))
                .nwavConOpciones(
                        NwavCampoOpcion.nwavTexto("formato", "Formato", "Página {n} de {total}")
                                .nwavConAyuda("{n} = número, {total} = páginas, {archivo} = nombre, {fecha} = hoy"),
                        NwavCampoOpcion.nwavEleccion("posicion", "Dónde", "pie", "pie=Pie de página", "encabezado=Encabezado"),
                        NwavCampoOpcion.nwavEleccion("alineacion", "Alineación", "NWAV_CENTRO", "NWAV_IZQUIERDA=Izquierda",
                                "NWAV_CENTRO=Centro", "NWAV_DERECHA=Derecha"),
                        NwavCampoOpcion.nwavDecimal("tamano", "Tamaño de letra", 10, 5, 72, 1),
                        NwavCampoOpcion.nwavDecimal("margen", "Distancia al borde (pt)", 28, 0, 200, 2),
                        NwavCampoOpcion.nwavEntero("inicio", "Empezar a contar en", 1, 0, 100_000),
                        NwavCampoOpcion.nwavTexto("paginas", "Páginas (vacío = todas)", ""))
                .nwavConPalabrasClave("numerar paginacion pie encabezado lote varios"));

        nwavLista.add(new NwavDefinicionHerramienta("comparar", "Comparar PDF",
                "Muestra dos versiones lado a lado con las diferencias resaltadas.", NwavCategoriaHerramienta.NWAV_EDITAR,
                Feather.COLUMNS)
                .nwavVistaPropia("nwav-comparar-view")
                .nwavConPalabrasClave("diferencias versiones cambios diff"));
    }

    private static com.nwav.editorpdf.model.NwavColorRgb nwavColor(String nwavHex) {
        String nwavDigitos = nwavHex == null ? "" : nwavHex.replace("#", "");
        if (!nwavDigitos.matches("[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) {
            return new com.nwav.editorpdf.model.NwavColorRgb(156, 163, 175);
        }
        return new com.nwav.editorpdf.model.NwavColorRgb(Integer.parseInt(nwavDigitos.substring(0, 2), 16),
                Integer.parseInt(nwavDigitos.substring(2, 4), 16), Integer.parseInt(nwavDigitos.substring(4, 6), 16));
    }

    private void nwavAgregarSeguridad(List<NwavDefinicionHerramienta> nwavLista) {
        nwavLista.add(new NwavDefinicionHerramienta("proteger", "Proteger o desbloquear",
                "Pon o quita una contraseña y limita lo que se puede hacer con el PDF.",
                NwavCategoriaHerramienta.NWAV_SEGURIDAD, Feather.LOCK).nwavEditor(nwavVisor::nwavOnProteccion)
                .nwavConPalabrasClave("contraseña cifrar permisos desbloquear quitar"));
        nwavLista.add(new NwavDefinicionHerramienta("firmar", "Firmar PDF",
                "Firma con una firma dibujada o cargada desde una imagen.", NwavCategoriaHerramienta.NWAV_SEGURIDAD,
                Feather.FEATHER).nwavEditor(nwavVisor::nwavOnFirma).nwavConPalabrasClave("firma rubrica escrita dibujada"));

        nwavLista.add(new NwavDefinicionHerramienta("proteger-lote", "Proteger varios PDF",
                "Pon la misma contraseña y los mismos permisos a muchos archivos (AES-256).",
                NwavCategoriaHerramienta.NWAV_SEGURIDAD, Feather.LOCK)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_PROTEGER, true, nwavEntrada -> {
                    String nwavClave = nwavEntrada.nwavTexto("clave", "");
                    if (!nwavClave.equals(nwavEntrada.nwavTexto("repetir", ""))) {
                        throw new com.nwav.editorpdf.exception.NwavEditorException("Las contraseñas no coinciden.");
                    }
                    boolean nwavRestringir = nwavEntrada.nwavSiNo("restringir", false);
                    var nwavOpciones = new com.nwav.editorpdf.model.NwavOpcionesProteccion(nwavClave,
                            nwavRestringir ? nwavEntrada.nwavTexto("propietario", "") : "", nwavEntrada.nwavSiNo("imprimir", true),
                            nwavEntrada.nwavSiNo("copiar", false), nwavEntrada.nwavSiNo("modificar", false),
                            nwavEntrada.nwavSiNo("anotar", false), nwavEntrada.nwavSiNo("rellenar", true));
                    if (nwavRestringir && !nwavOpciones.nwavTienePropietario()) {
                        throw new com.nwav.editorpdf.exception.NwavEditorException(
                                "Escribe la contraseña de propietario para limitar los permisos.");
                    }
                    return NwavResultadoProceso.nwavDe(nwavProteccion.nwavProteger(nwavEntrada.nwavArchivo(),
                            nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()), nwavOpciones,
                            NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(), nwavEntrada.nwavCarpetaSalida(),
                                    "protegido", "pdf")), nwavRestringir ? "Protegido con permisos limitados" : "Protegido");
                }))
                .nwavConOpciones(
                        NwavCampoOpcion.nwavContrasena("clave", "Contraseña para abrir"),
                        NwavCampoOpcion.nwavContrasena("repetir", "Repite la contraseña"),
                        NwavCampoOpcion.nwavCasilla("restringir", "Limitar lo que se puede hacer (contraseña de propietario)",
                                false),
                        NwavCampoOpcion.nwavContrasena("propietario", "Contraseña de propietario").nwavSi("restringir", "true"),
                        NwavCampoOpcion.nwavCasilla("imprimir", "Permitir imprimir", true).nwavSi("restringir", "true"),
                        NwavCampoOpcion.nwavCasilla("copiar", "Permitir copiar texto e imágenes", false)
                                .nwavSi("restringir", "true"),
                        NwavCampoOpcion.nwavCasilla("modificar", "Permitir modificar", false).nwavSi("restringir", "true"),
                        NwavCampoOpcion.nwavCasilla("anotar", "Permitir comentarios", false).nwavSi("restringir", "true"),
                        NwavCampoOpcion.nwavCasilla("rellenar", "Permitir rellenar formularios", true)
                                .nwavSi("restringir", "true"))
                .nwavConAviso("Las contraseñas no se guardan en ningún sitio: si las olvidas, no se podrán recuperar.")
                .nwavConPalabrasClave("contraseña cifrar encriptar permisos lote"));

        nwavLista.add(new NwavDefinicionHerramienta("desbloquear", "Quitar contraseña",
                "Crea una copia sin contraseña de los PDF protegidos (necesitas la contraseña).",
                NwavCategoriaHerramienta.NWAV_SEGURIDAD, Feather.UNLOCK)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_DESBLOQUEAR, true, nwavEntrada -> {
                    String nwavClave = nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo());
                    if (nwavClave == null || nwavClave.isEmpty()) {
                        nwavClave = nwavEntrada.nwavTexto("clave", "");
                    }
                    return NwavResultadoProceso.nwavDe(nwavProteccion.nwavDesbloquear(nwavEntrada.nwavArchivo(),
                            nwavClave.isEmpty() ? null : nwavClave, NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(),
                                    nwavEntrada.nwavCarpetaSalida(), "sin_clave", "pdf")), "Contraseña quitada");
                }))
                .nwavConOpciones(NwavCampoOpcion.nwavContrasena("clave", "Contraseña (de propietario)")
                        .nwavConAyuda("Si cada archivo tiene una distinta, pulsa «Escribir contraseña» en sus resultados."))
                .nwavConPalabrasClave("desproteger quitar contraseña desbloquear descifrar"));

        nwavLista.add(new NwavDefinicionHerramienta("firma-digital", "Firma digital",
                "Firma con tu certificado (.p12/.pfx); cualquier cambio posterior se detecta.",
                NwavCategoriaHerramienta.NWAV_SEGURIDAD, Feather.AWARD)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_FIRMA_DIGITAL, true, this::nwavProcesoFirmaDigital))
                .nwavConOpciones(
                        NwavCampoOpcion.nwavArchivo("certificado", "Certificado (.p12 o .pfx)", "p12", "pfx"),
                        NwavCampoOpcion.nwavContrasena("claveCertificado", "Contraseña del certificado"),
                        NwavCampoOpcion.nwavTexto("motivo", "Motivo (opcional)", ""),
                        NwavCampoOpcion.nwavTexto("lugar", "Lugar (opcional)", ""),
                        NwavCampoOpcion.nwavCasilla("visible", "Mostrar un sello de firma en la página", true),
                        NwavCampoOpcion.nwavEleccion("pagina", "Página del sello", "ultima", "primera=Primera",
                                "ultima=Última").nwavSi("visible", "true"),
                        NwavCampoOpcion.nwavEleccion("esquina", "Posición del sello", "abajo-derecha",
                                "abajo-derecha=Abajo a la derecha", "abajo-izquierda=Abajo a la izquierda",
                                "arriba-derecha=Arriba a la derecha", "arriba-izquierda=Arriba a la izquierda")
                                .nwavSi("visible", "true"))
                .nwavConAviso("¿No tienes certificado? Crea uno personal con «Crear certificado». Los certificados "
                        + "autofirmados prueban que el documento no cambió, pero no tienen validez legal como los de una "
                        + "autoridad (FNMT, DNIe…).")
                .nwavConPalabrasClave("certificado p12 pfx pkcs12 firmar digitalmente electronica"));

        nwavLista.add(new NwavDefinicionHerramienta("verificar-firmas", "Verificar firmas",
                "Comprueba si las firmas digitales de un PDF son válidas y si se cambió después.",
                NwavCategoriaHerramienta.NWAV_SEGURIDAD, Feather.CHECK_CIRCLE)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_VERIFICAR_FIRMAS, true, nwavEntrada -> {
                    var nwavFirmas = nwavFirmaDigital.nwavVerificar(nwavEntrada.nwavArchivo(),
                            nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()));
                    if (nwavFirmas.isEmpty()) {
                        return new NwavResultadoProceso(List.of(), "No tiene firmas digitales", "Sin firmas");
                    }
                    String nwavDetalle = nwavFirmas.stream().map(
                            com.nwav.editorpdf.service.NwavFirmaDigitalService.NwavFirmaVerificada::nwavResumen)
                            .collect(java.util.stream.Collectors.joining(" · "));
                    long nwavValidas = nwavFirmas.stream().filter(
                            com.nwav.editorpdf.service.NwavFirmaDigitalService.NwavFirmaVerificada::nwavValida).count();
                    String nwavResumen = (nwavValidas == nwavFirmas.size() ? "✓ " : "✗ ") + nwavValidas + " de "
                            + nwavFirmas.size() + " firmas válidas. " + nwavDetalle;
                    return new NwavResultadoProceso(List.of(), nwavResumen, nwavResumen);
                }))
                .nwavConPalabrasClave("comprobar validar firma certificado"));

        nwavLista.add(new NwavDefinicionHerramienta("crear-certificado", "Crear certificado",
                "Genera un certificado personal (.p12) para firmar digitalmente.", NwavCategoriaHerramienta.NWAV_SEGURIDAD,
                Feather.KEY)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_CERTIFICADO, false, nwavEntrada -> {
                    String nwavClave = nwavEntrada.nwavTexto("clave", "");
                    if (!nwavClave.equals(nwavEntrada.nwavTexto("repetir", ""))) {
                        throw new com.nwav.editorpdf.exception.NwavEditorException("Las contraseñas no coinciden.");
                    }
                    String nwavNombre = nwavEntrada.nwavTexto("nombre", "");
                    Path nwavCarpeta = nwavEntrada.nwavCarpetaSalida() != null ? nwavEntrada.nwavCarpetaSalida()
                            : Path.of(System.getProperty("user.home"));
                    Path nwavDestino = NwavEscrituraSegura.nwavSinColision(nwavCarpeta.resolve(
                            (nwavNombre.isBlank() ? "certificado" : nwavNombre.strip().replaceAll("[^\\p{L}\\p{N}._-]+", "_"))
                                    + ".p12"), null);
                    Path nwavCreado = nwavFirmaDigital.nwavCrearCertificado(nwavNombre, nwavEntrada.nwavTexto("organizacion", ""),
                            nwavEntrada.nwavTexto("correo", ""), nwavClave.toCharArray(), nwavEntrada.nwavEntero("anios", 3),
                            nwavDestino);
                    return new NwavResultadoProceso(List.of(nwavCreado), "Certificado creado. Guárdalo en un lugar "
                            + "seguro y no compartas su contraseña.", "Certificado de " + nwavNombre);
                }))
                .nwavSinArchivos()
                .nwavConOpciones(
                        NwavCampoOpcion.nwavTexto("nombre", "Nombre y apellidos", ""),
                        NwavCampoOpcion.nwavTexto("organizacion", "Organización (opcional)", ""),
                        NwavCampoOpcion.nwavTexto("correo", "Correo (opcional)", ""),
                        NwavCampoOpcion.nwavContrasena("clave", "Contraseña del certificado (mín. 6)"),
                        NwavCampoOpcion.nwavContrasena("repetir", "Repite la contraseña"),
                        NwavCampoOpcion.nwavEntero("anios", "Años de validez", 3, 1, 10))
                .nwavConAviso("No necesita archivos. Si no eliges carpeta, se guarda en tu carpeta personal.")
                .nwavConPalabrasClave("p12 pfx clave certificado generar"));

        nwavLista.add(new NwavDefinicionHerramienta("redactar", "Redactar",
                "Elimina de verdad la información sensible que marques (no solo la tapa).",
                NwavCategoriaHerramienta.NWAV_SEGURIDAD, Feather.SLASH)
                .nwavEditor(() -> nwavVisor.nwavHerramientas().nwavHerramientaProperty().set(NwavHerramienta.NWAV_REDACTAR))
                .nwavConPalabrasClave("censurar tachar ocultar anonimizar eliminar datos sensibles"));

        nwavLista.add(new NwavDefinicionHerramienta("redactar-busqueda", "Redactar por búsqueda",
                "Busca palabras o datos personales (correos, DNI, IBAN…) y los elimina en muchos PDF.",
                NwavCategoriaHerramienta.NWAV_SEGURIDAD, Feather.SEARCH)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_REDACTAR, true, nwavEntrada -> {
                    List<String> nwavTerminos = Arrays.stream(nwavEntrada.nwavTexto("terminos", "").split("[,;\\n]"))
                            .map(String::strip).filter(nwavT -> !nwavT.isEmpty()).toList();
                    java.util.Set<com.nwav.editorpdf.service.NwavRedaccionService.NwavPatron> nwavPatrones =
                            Arrays.stream(nwavEntrada.nwavTexto("patrones", "").split(",")).map(String::strip)
                                    .filter(nwavP -> !nwavP.isEmpty())
                                    .map(com.nwav.editorpdf.service.NwavRedaccionService.NwavPatron::valueOf)
                                    .collect(java.util.stream.Collectors.toSet());
                    var nwavResultado = nwavRedaccion.nwavRedactarBusqueda(nwavEntrada.nwavArchivo(),
                            nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()), nwavTerminos, nwavPatrones,
                            nwavEntrada.nwavSiNo("limpiar", true), NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(),
                                    nwavEntrada.nwavCarpetaSalida(), "redactado", "pdf"), nwavEntrada.nwavCancelacion());
                    return NwavResultadoProceso.nwavDe(nwavResultado.nwavDestino(), nwavResultado.nwavZonas()
                            + " coincidencias eliminadas en " + nwavResultado.nwavPaginas() + " páginas");
                }))
                .nwavConOpciones(
                        NwavCampoOpcion.nwavTexto("terminos", "Palabras o frases (separadas por comas)", ""),
                        NwavCampoOpcion.nwavMultiple("patrones", "Datos personales", "NWAV_CORREOS,NWAV_DNI",
                                () -> Arrays.stream(com.nwav.editorpdf.service.NwavRedaccionService.NwavPatron.values())
                                        .map(nwavP -> nwavP.name() + "=" + nwavP.getNwavDescripcion()).toList()),
                        NwavCampoOpcion.nwavCasilla("limpiar", "Quitar también metadatos y datos ocultos", true))
                .nwavConAviso("Revisa el resultado: la búsqueda de patrones (teléfonos, tarjetas…) es automática y puede "
                        + "dejarse alguno o marcar de más. Los escaneos sin OCR no tienen texto que buscar.")
                .nwavConPalabrasClave("anonimizar rgpd datos personales censurar correos dni iban"));

        nwavLista.add(new NwavDefinicionHerramienta("limpiar-metadatos", "Quitar metadatos",
                "Borra autor, programa, fechas, XMP, adjuntos, JavaScript y miniaturas ocultas.",
                NwavCategoriaHerramienta.NWAV_SEGURIDAD, Feather.WIND)
                .nwavLote(new NwavTrabajo(NwavTipoOperacion.NWAV_LIMPIAR, true, nwavEntrada ->
                        NwavResultadoProceso.nwavDe(nwavRedaccion.nwavLimpiarArchivo(nwavEntrada.nwavArchivo(),
                                nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()),
                                NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(), nwavEntrada.nwavCarpetaSalida(),
                                        "limpio", "pdf")), "Metadatos y datos ocultos eliminados")))
                .nwavConPalabrasClave("privacidad anonimizar metadatos autor xmp limpiar"));
    }

    private NwavResultadoProceso nwavProcesoFirmaDigital(NwavEntradaProceso nwavEntrada) {
        String nwavCertificado = nwavEntrada.nwavTexto("certificado", "");
        if (nwavCertificado.isBlank()) {
            throw new com.nwav.editorpdf.exception.NwavEditorException("Elige el archivo del certificado (.p12 o .pfx).");
        }
        com.nwav.editorpdf.model.NwavRectangulo nwavZona = null;
        int nwavPagina = "primera".equals(nwavEntrada.nwavTexto("pagina", "ultima")) ? 0 : -1;
        if (nwavEntrada.nwavSiNo("visible", true)) {
            Path nwavArchivo = nwavEntrada.nwavArchivo();
            float[] nwavTamano = nwavTamanoPagina(nwavArchivo, nwavEntrada.nwavContrasena(nwavArchivo), nwavPagina);
            String nwavEsquina = nwavEntrada.nwavTexto("esquina", "abajo-derecha");
            double nwavX = nwavEsquina.endsWith("derecha") ? nwavTamano[0] - 36 - 200 : 36;
            double nwavY = nwavEsquina.startsWith("abajo") ? nwavTamano[1] - 36 - 60 : 36;
            nwavZona = new com.nwav.editorpdf.model.NwavRectangulo(nwavX, nwavY, 200, 60);
        }
        Path nwavFirmado = nwavFirmaDigital.nwavFirmar(nwavEntrada.nwavArchivo(),
                nwavEntrada.nwavContrasena(nwavEntrada.nwavArchivo()), Path.of(nwavCertificado),
                nwavEntrada.nwavTexto("claveCertificado", "").toCharArray(),
                new com.nwav.editorpdf.service.NwavFirmaDigitalService.NwavOpcionesFirma(nwavEntrada.nwavTexto("motivo", ""),
                        nwavEntrada.nwavTexto("lugar", ""), null, nwavPagina, nwavZona, null),
                NwavEscrituraSegura.nwavRutaSalida(nwavEntrada.nwavArchivo(), nwavEntrada.nwavCarpetaSalida(), "firmado", "pdf"));
        return NwavResultadoProceso.nwavDe(nwavFirmado, "Firmado digitalmente");
    }

    private float[] nwavTamanoPagina(Path nwavArchivo, String nwavClave, int nwavPagina) {
        try (var nwavDocumento = nwavDocumentosPdf.nwavAbrir(nwavArchivo, nwavClave)) {
            int nwavIndice = nwavPagina < 0 ? nwavDocumento.getNwavNumeroPaginas() - 1 : nwavPagina;
            var nwavTamano = nwavDocumento.nwavTamano(nwavIndice);
            return new float[]{nwavTamano.nwavAncho(), nwavTamano.nwavAlto()};
        }
    }
}
