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
    private final com.nwav.editorpdf.service.NwavOrganizarService nwavOrganizar;
    private final NwavDependenciasService nwavDependencias;
    private final com.nwav.editorpdf.service.NwavConversionAPdfService nwavAPdf;
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
        nwavAgregarEditar(nwavLista);
        nwavAgregarSeguridad(nwavLista);
        return nwavLista;
    }

    // ================================================================ Organizar

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

    // ========================================================== Convertir a PDF

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
