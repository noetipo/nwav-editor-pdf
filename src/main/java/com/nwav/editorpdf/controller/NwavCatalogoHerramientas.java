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
