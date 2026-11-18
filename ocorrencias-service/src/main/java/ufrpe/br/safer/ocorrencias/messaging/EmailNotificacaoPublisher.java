package ufrpe.br.safer.ocorrencias.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ufrpe.br.safer.ocorrencias.client.UsuariosClient;
import ufrpe.br.safer.ocorrencias.config.RabbitConfig;
import ufrpe.br.safer.ocorrencias.dto.DestinatarioDTO;
import ufrpe.br.safer.ocorrencias.messaging.command.EnviarEmailCommand;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaCanceladaEvent;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaRegistradaEvent;
import ufrpe.br.safer.ocorrencias.messaging.event.StatusOcorrenciaAlteradoEvent;

@Component
@Slf4j
public class EmailNotificacaoPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final UsuariosClient usuariosClient;
    private final String urlSistema;

    public EmailNotificacaoPublisher(
            RabbitTemplate rabbitTemplate,
            UsuariosClient usuariosClient,
            @Value("${safer.frontend-url:http://localhost:5500}") String urlSistema) {
        this.rabbitTemplate = rabbitTemplate;
        this.usuariosClient = usuariosClient;
        this.urlSistema = urlSistema;
    }

    public void notificarRegistro(OcorrenciaRegistradaEvent evento) {
        publicar(EmailOcorrenciaBuilder.registro(evento));
    }

    // Avisa analistas e administradores de que há uma nova ocorrência aguardando atendimento
    public void notificarAnalistas(OcorrenciaRegistradaEvent evento) {
        for (DestinatarioDTO analista : usuariosClient.listarDestinatariosDeNovasOcorrencias()) {
            publicar(EmailOcorrenciaBuilder.novaOcorrenciaParaAnalista(evento, analista, urlSistema));
        }
    }

    public void notificarStatusAlterado(StatusOcorrenciaAlteradoEvent evento) {
        publicar(EmailOcorrenciaBuilder.statusAlterado(evento));
    }

    public void notificarCancelamento(OcorrenciaCanceladaEvent evento) {
        publicar(EmailOcorrenciaBuilder.cancelamento(evento));
    }

    private void publicar(EnviarEmailCommand comando) {
        if (comando.destinatario() == null || comando.destinatario().isBlank()) {
            log.warn("Destinatário sem e-mail, notificação {} não enviada", comando.chaveIdempotencia());
            return;
        }
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, RabbitConfig.ROUTING_KEY_ENVIAR_EMAIL, comando);
            log.info("Pedido de e-mail {} publicado", comando.chaveIdempotencia());
        } catch (Exception e) {
            log.error("Falha ao publicar pedido de e-mail {}: {}", comando.chaveIdempotencia(), e.getMessage());
        }
    }
}
