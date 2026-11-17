package com.nwav.editorpdf.service.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.InternalServerException;
import com.anthropic.errors.NotFoundException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.beta.messages.BetaBase64PdfSource;
import com.anthropic.models.beta.messages.BetaContentBlockParam;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaRawMessageStreamEvent;
import com.anthropic.models.beta.messages.BetaRequestDocumentBlock;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.BetaTextBlockParam;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.service.NwavCancelacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

/**
 * Llamada a Claude con el SDK oficial de Anthropic, en streaming (los documentos largos no
 * agotan el tiempo de espera). Activa la alternativa del servidor ("fallbacks": "default"):
 * si el modelo rechaza la peticion por sus filtros de seguridad, el servidor reintenta con
 * otro modelo adecuado en lugar de devolver solo el rechazo.
 */
@Component
@RequiredArgsConstructor
public class NwavClienteClaude implements NwavClienteIa {

    private static final long NWAV_MAX_TOKENS = 64_000L;
    private static final String NWAV_BETA_ALTERNATIVA = "server-side-fallback-2026-07-01";

    private final NwavConfiguracionIaService nwavConfiguracion;

    @Override
    public String nwavGenerar(String nwavSistema, byte[] nwavPdf, String nwavTexto, NwavCancelacion nwavCancelacion) {
        if (!nwavConfiguracion.nwavActiva() || !nwavConfiguracion.nwavTieneClave()) {
            throw new NwavEditorException(nwavConfiguracion.nwavEstado().nwavInstrucciones());
        }
        List<BetaContentBlockParam> nwavContenido = new ArrayList<>();
        if (nwavPdf != null) {
            nwavContenido.add(BetaContentBlockParam.ofDocument(BetaRequestDocumentBlock.builder()
                    .source(BetaBase64PdfSource.builder().data(Base64.getEncoder().encodeToString(nwavPdf)).build())
                    .build()));
        }
        nwavContenido.add(BetaContentBlockParam.ofText(BetaTextBlockParam.builder().text(nwavTexto).build()));
        MessageCreateParams nwavParametros = MessageCreateParams.builder()
                .model(nwavConfiguracion.nwavModelo())
                .maxTokens(NWAV_MAX_TOKENS)
                .system(nwavSistema)
                .addUserMessageOfContentBlockParams(nwavContenido)
                .outputConfig(BetaOutputConfig.builder().effort(BetaOutputConfig.Effort.MEDIUM).build())
                .addBeta(NWAV_BETA_ALTERNATIVA)
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .build();

        AnthropicClient nwavCliente = AnthropicOkHttpClient.builder()
                .apiKey(nwavConfiguracion.nwavClaveApi())
                .timeout(Duration.ofMinutes(15))
                .build();
        StringBuilder nwavSalida = new StringBuilder();
        BetaStopReason nwavMotivo = null;
        try (StreamResponse<BetaRawMessageStreamEvent> nwavFlujo = nwavCliente.beta().messages()
                .createStreaming(nwavParametros)) {
            Iterator<BetaRawMessageStreamEvent> nwavEventos = nwavFlujo.stream().iterator();
            while (nwavEventos.hasNext()) {
                nwavCancelacion.nwavVerificar();
                BetaRawMessageStreamEvent nwavEvento = nwavEventos.next();
                nwavEvento.contentBlockDelta().flatMap(nwavDelta -> nwavDelta.delta().text())
                        .ifPresent(nwavTrozo -> nwavSalida.append(nwavTrozo.text()));
                if (nwavEvento.messageDelta().isPresent()) {
                    nwavMotivo = nwavEvento.messageDelta().get().delta().stopReason().orElse(nwavMotivo);
                }
            }
        } catch (UnauthorizedException nwavError) {
            throw new NwavEditorException("La clave de API no es válida. Revísala en Preferencias.", nwavError);
        } catch (PermissionDeniedException nwavError) {
            throw new NwavEditorException("Tu clave de API no tiene permiso para usar este modelo.", nwavError);
        } catch (NotFoundException nwavError) {
            throw new NwavEditorException("El modelo «" + nwavConfiguracion.nwavModelo() + "» no existe. Revisa el "
                    + "modelo en Preferencias (por defecto: " + NwavConfiguracionIaService.NWAV_MODELO_DEFECTO + ").",
                    nwavError);
        } catch (RateLimitException nwavError) {
            throw new NwavEditorException("Se alcanzó el límite de uso de la API. Espera un poco y vuelve a intentarlo.",
                    nwavError);
        } catch (BadRequestException nwavError) {
            throw new NwavEditorException("El servicio rechazó la petición: " + nwavError.getMessage(), nwavError);
        } catch (InternalServerException nwavError) {
            throw new NwavEditorException("El servicio de IA no está disponible ahora mismo. Inténtalo más tarde.",
                    nwavError);
        } catch (AnthropicServiceException nwavError) {
            throw new NwavEditorException("Error del servicio de IA (" + nwavError.statusCode() + "): "
                    + nwavError.getMessage(), nwavError);
        } catch (AnthropicIoException nwavError) {
            throw new NwavEditorException("No se pudo conectar con el servicio de IA. ¿Hay conexión a Internet?",
                    nwavError);
        } finally {
            nwavCliente.close();
        }
        if (nwavMotivo != null && BetaStopReason.REFUSAL.equals(nwavMotivo)) {
            throw new NwavEditorException("El modelo no quiso procesar este contenido.");
        }
        if (nwavMotivo != null && BetaStopReason.MAX_TOKENS.equals(nwavMotivo)) {
            throw new NwavEditorException("La respuesta se cortó por ser demasiado larga. Prueba con menos páginas.");
        }
        return nwavSalida.toString().strip();
    }
}
