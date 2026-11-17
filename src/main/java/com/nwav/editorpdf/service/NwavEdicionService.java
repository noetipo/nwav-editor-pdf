package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.model.NwavColorRgb;
import com.nwav.editorpdf.model.NwavEstiloTexto;
import com.nwav.editorpdf.model.NwavPunto;
import com.nwav.editorpdf.model.NwavRectangulo;
import com.nwav.editorpdf.model.NwavTipoForma;
import com.nwav.editorpdf.model.NwavTipoMarca;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.comando.NwavComando;
import com.nwav.editorpdf.service.comando.NwavComandoDuplicar;
import com.nwav.editorpdf.service.comando.NwavComandoEliminar;
import com.nwav.editorpdf.service.comando.NwavComandoInsertarImagenes;
import com.nwav.editorpdf.service.comando.NwavComandoInsertarPdf;
import com.nwav.editorpdf.service.comando.NwavComandoMover;
import com.nwav.editorpdf.service.comando.NwavComandoRotar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class NwavEdicionService {
    private static final String NWAV_EN_MEMORIA = " (en memoria, sin guardar)";

    private final NwavPaginaService nwavPaginas;
    private final NwavContenidoService nwavContenido;
    private final NwavSelloService nwavSellos;
    private final NwavSeguridadService nwavSeguridad;
    private final NwavFormularioService nwavFormularios;
    private final NwavMetadatosService nwavMetadatos;
    private final NwavHistorialService nwavHistorial;
    private final NwavCreacionCamposService nwavCreacionCampos;
    private final NwavRedaccionService nwavRedaccion;

    public record NwavResultadoEdicion(String nwavDescripcion, int nwavPaginaDestacada, boolean nwavTuvoEfecto,
                                       boolean nwavEstructural) {
    }

    public NwavResultadoEdicion nwavRotar(NwavDocumentoPdf nwavDocumento, List<Integer> nwavIndices, int nwavGrados) {
        return nwavAplicar(nwavDocumento, new NwavComandoRotar(nwavPaginas, nwavDocumento, nwavIndices, nwavGrados));
    }

    public NwavResultadoEdicion nwavEliminar(NwavDocumentoPdf nwavDocumento, List<Integer> nwavIndices) {
        return nwavAplicar(nwavDocumento, new NwavComandoEliminar(nwavPaginas, nwavDocumento, nwavIndices));
    }

    public NwavResultadoEdicion nwavDuplicar(NwavDocumentoPdf nwavDocumento, List<Integer> nwavIndices) {
        return nwavAplicar(nwavDocumento, new NwavComandoDuplicar(nwavPaginas, nwavDocumento, nwavIndices));
    }

    public NwavResultadoEdicion nwavMover(NwavDocumentoPdf nwavDocumento, List<Integer> nwavIndices, int nwavDestino) {
        return nwavAplicar(nwavDocumento, new NwavComandoMover(nwavPaginas, nwavDocumento, nwavIndices, nwavDestino));
    }

    public NwavResultadoEdicion nwavInsertarPdf(NwavDocumentoPdf nwavDocumento, Path nwavOrigen, String nwavContrasena,
                                                String nwavRangos, int nwavPosicion) {
        return nwavAplicar(nwavDocumento, new NwavComandoInsertarPdf(nwavPaginas, nwavDocumento, nwavOrigen,
                nwavContrasena, nwavRangos, nwavPosicion));
    }

    public NwavResultadoEdicion nwavInsertarImagenes(NwavDocumentoPdf nwavDocumento, List<Path> nwavImagenes,
                                                     int nwavPosicion) {
        return nwavAplicar(nwavDocumento,
                new NwavComandoInsertarImagenes(nwavPaginas, nwavDocumento, nwavImagenes, nwavPosicion));
    }

    public NwavResultadoEdicion nwavAgregarTexto(NwavDocumentoPdf nwavDocumento, int nwavPagina,
                                                 NwavRectangulo nwavZona, String nwavTexto, NwavEstiloTexto nwavEstilo) {
        return nwavAplicar(nwavDocumento,
                nwavContenido.nwavComandoTexto(nwavDocumento, nwavPagina, nwavZona, nwavTexto, nwavEstilo));
    }

    public NwavResultadoEdicion nwavInsertarImagen(NwavDocumentoPdf nwavDocumento, int nwavPagina, Path nwavImagen,
                                                   NwavRectangulo nwavZona) {
        return nwavAplicar(nwavDocumento,
                nwavContenido.nwavComandoImagen(nwavDocumento, nwavPagina, nwavImagen, nwavZona));
    }

    public NwavResultadoEdicion nwavCubrir(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavRectangulo nwavZona,
                                           NwavColorRgb nwavColor) {
        return nwavAplicar(nwavDocumento,
                nwavContenido.nwavComandoCubrir(nwavDocumento, nwavPagina, nwavZona, nwavColor));
    }

    public NwavResultadoEdicion nwavMarcar(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavTipoMarca nwavTipo,
                                           NwavRectangulo nwavZona, NwavColorRgb nwavColor) {
        return nwavAplicar(nwavDocumento,
                nwavContenido.nwavComandoMarca(nwavDocumento, nwavPagina, nwavTipo, nwavZona, nwavColor));
    }

    public NwavResultadoEdicion nwavAgregarNota(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavPunto nwavPunto,
                                                String nwavTexto, NwavColorRgb nwavColor) {
        return nwavAplicar(nwavDocumento,
                nwavContenido.nwavComandoNota(nwavDocumento, nwavPagina, nwavPunto, nwavTexto, nwavColor));
    }

    public NwavResultadoEdicion nwavDibujar(NwavDocumentoPdf nwavDocumento, int nwavPagina, List<NwavPunto> nwavTrazo,
                                            NwavColorRgb nwavColor, double nwavGrosor) {
        return nwavAplicar(nwavDocumento,
                nwavContenido.nwavComandoDibujo(nwavDocumento, nwavPagina, nwavTrazo, nwavColor, nwavGrosor));
    }

    public NwavResultadoEdicion nwavAgregarForma(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavTipoForma nwavTipo,
                                                 NwavPunto nwavDesde, NwavPunto nwavHasta, NwavColorRgb nwavColor,
                                                 double nwavGrosor) {
        return nwavAplicar(nwavDocumento, nwavContenido.nwavComandoForma(nwavDocumento, nwavPagina, nwavTipo,
                nwavDesde, nwavHasta, nwavColor, nwavGrosor));
    }

    public NwavResultadoEdicion nwavCrearCampo(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavRectangulo nwavZona,
                                               NwavCreacionCamposService.NwavCampoNuevo nwavCampo) {
        return nwavAplicar(nwavDocumento, nwavCreacionCampos.nwavComandoCrear(nwavDocumento, nwavPagina, nwavZona, nwavCampo));
    }

    public NwavResultadoEdicion nwavRedactar(NwavDocumentoPdf nwavDocumento, int nwavPagina, NwavRectangulo nwavZona) {
        return nwavAplicar(nwavDocumento, nwavRedaccion.nwavComandoRedactar(nwavDocumento,
                List.of(new NwavRedaccionService.NwavZona(nwavPagina, nwavZona))));
    }

    public NwavResultadoEdicion nwavRecortar(NwavDocumentoPdf nwavDocumento, List<Integer> nwavPaginasElegidas,
                                             NwavRectangulo nwavZona) {
        return nwavAplicar(nwavDocumento, nwavPaginas.nwavComandoRecortar(nwavDocumento, nwavPaginasElegidas, nwavZona));
    }

    public NwavResultadoEdicion nwavRellenarFormulario(NwavDocumentoPdf nwavDocumento,
                                                       java.util.Map<String, String> nwavValores) {
        return nwavAplicar(nwavDocumento, nwavFormularios.nwavComandoRellenar(nwavDocumento, nwavValores));
    }

    public NwavResultadoEdicion nwavMarcaAgua(NwavDocumentoPdf nwavDocumento,
                                              com.nwav.editorpdf.model.NwavOpcionesMarcaAgua nwavOpciones) {
        return nwavAplicar(nwavDocumento, nwavSellos.nwavComandoMarcaAgua(nwavDocumento, nwavOpciones));
    }

    public NwavResultadoEdicion nwavEncabezado(NwavDocumentoPdf nwavDocumento,
                                               com.nwav.editorpdf.model.NwavOpcionesEncabezado nwavOpciones) {
        return nwavAplicar(nwavDocumento, nwavSellos.nwavComandoEncabezado(nwavDocumento, nwavOpciones));
    }

    public NwavResultadoEdicion nwavProteger(NwavDocumentoPdf nwavDocumento,
                                             com.nwav.editorpdf.model.NwavOpcionesProteccion nwavOpciones) {
        return nwavAplicar(nwavDocumento, nwavSeguridad.nwavComandoProteger(nwavDocumento, nwavOpciones));
    }

    public NwavResultadoEdicion nwavQuitarProteccion(NwavDocumentoPdf nwavDocumento, String nwavContrasenaPropietario) {
        return nwavAplicar(nwavDocumento,
                nwavSeguridad.nwavComandoQuitarProteccion(nwavDocumento, nwavContrasenaPropietario));
    }

    public NwavResultadoEdicion nwavInsertarFirma(NwavDocumentoPdf nwavDocumento, int nwavPagina, byte[] nwavPng,
                                                  NwavRectangulo nwavZona) {
        return nwavAplicar(nwavDocumento, nwavContenido.nwavComandoImagenBytes(nwavDocumento, nwavPagina, nwavPng,
                nwavZona, NwavTipoOperacion.NWAV_FIRMA, "Insertar firma"));
    }

    public NwavResultadoEdicion nwavEditarMetadatos(NwavDocumentoPdf nwavDocumento,
                                                    com.nwav.editorpdf.model.NwavMetadatos nwavNuevos) {
        return nwavAplicar(nwavDocumento, nwavMetadatos.nwavComandoEditar(nwavDocumento, nwavNuevos));
    }

    public NwavResultadoEdicion nwavAplicar(NwavDocumentoPdf nwavDocumento, NwavComando nwavComando) {
        try {
            boolean nwavApilado;
            synchronized (nwavDocumento) {
                nwavDocumento.nwavVerificarAbierto();
                nwavApilado = nwavDocumento.getNwavGestor().nwavEjecutar(nwavComando);
            }
            if (nwavApilado) {
                nwavHistorial.nwavRegistrarExito(nwavComando.nwavTipo(), nwavOrigenes(nwavDocumento, nwavComando),
                        null, nwavComando.nwavDescripcion() + NWAV_EN_MEMORIA);
            }
            return new NwavResultadoEdicion(nwavComando.nwavDescripcion(), nwavComando.nwavPaginaDestacada(),
                    nwavApilado, nwavComando.nwavCambiaEstructura());
        } catch (RuntimeException nwavError) {
            nwavHistorial.nwavRegistrarError(nwavComando.nwavTipo(), nwavOrigenes(nwavDocumento, nwavComando),
                    null, nwavError);
            throw NwavTraductorErrores.nwavTraducir(nwavError, nwavDocumento.getNwavNombre());
        }
    }

    public Optional<NwavResultadoEdicion> nwavDeshacer(NwavDocumentoPdf nwavDocumento) {
        return nwavDeshacerORehacer(nwavDocumento, NwavTipoOperacion.NWAV_DESHACER,
                nwavGestor -> nwavGestor.nwavDeshacer());
    }

    public Optional<NwavResultadoEdicion> nwavRehacer(NwavDocumentoPdf nwavDocumento) {
        return nwavDeshacerORehacer(nwavDocumento, NwavTipoOperacion.NWAV_REHACER,
                nwavGestor -> nwavGestor.nwavRehacer());
    }

    private Optional<NwavResultadoEdicion> nwavDeshacerORehacer(
            NwavDocumentoPdf nwavDocumento, NwavTipoOperacion nwavTipo,
            Function<com.nwav.editorpdf.service.comando.NwavGestorComandos, Optional<NwavComando>> nwavAccion) {
        try {
            Optional<NwavComando> nwavComando;
            synchronized (nwavDocumento) {
                nwavDocumento.nwavVerificarAbierto();
                nwavComando = nwavAccion.apply(nwavDocumento.getNwavGestor());
            }
            nwavComando.ifPresent(nwavHecho -> nwavHistorial.nwavRegistrarExito(nwavTipo,
                    nwavDocumento.getNwavRuta(), null, nwavHecho.nwavDescripcion() + NWAV_EN_MEMORIA));
            return nwavComando.map(nwavHecho -> new NwavResultadoEdicion(nwavHecho.nwavDescripcion(),
                    nwavHecho.nwavCambiaEstructura() && nwavTipo == NwavTipoOperacion.NWAV_DESHACER
                            ? -1 : nwavHecho.nwavPaginaDestacada(),
                    true, nwavHecho.nwavCambiaEstructura()));
        } catch (RuntimeException nwavError) {
            nwavHistorial.nwavRegistrarError(nwavTipo, nwavDocumento.getNwavRuta(), null, nwavError);
            throw NwavTraductorErrores.nwavTraducir(nwavError, nwavDocumento.getNwavNombre());
        }
    }

    private static Object nwavOrigenes(NwavDocumentoPdf nwavDocumento, NwavComando nwavComando) {
        if (nwavComando instanceof NwavComandoInsertarPdf nwavInsercion) {
            return List.of(nwavDocumento.getNwavRuta(), nwavInsercion.getNwavOrigen());
        }
        if (nwavComando instanceof NwavComandoInsertarImagenes nwavImagenes) {
            List<Object> nwavTodos = new java.util.ArrayList<>();
            nwavTodos.add(nwavDocumento.getNwavRuta());
            nwavTodos.addAll(nwavImagenes.getNwavImagenes());
            return nwavTodos;
        }
        return nwavDocumento.getNwavRuta();
    }
}
