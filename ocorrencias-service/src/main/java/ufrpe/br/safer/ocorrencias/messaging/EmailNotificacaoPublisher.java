package ufrpe.br.safer.ocorrencias.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import ufrpe.br.safer.ocorrencias.config.RabbitConfig;
import ufrpe.br.safer.ocorrencias.messaging.command.EnviarEmailCommand;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaCanceladaEvent;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaRegistradaEvent;
import ufrpe.br.safer.ocorrencias.messaging.event.StatusOcorrenciaAlteradoEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificacaoPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void notificarRegistro(OcorrenciaRegistradaEvent evento) {
        publicar(EmailOcorrenciaBuilder.registro(evento));
    }

    public void notificarStatusAlterado(StatusOcorrenciaAlteradoEvent evento) {
        publicar(EmailOcorrenciaBuilder.statusAlterado(evento));
    }

    public void notificarCancelamento(OcorrenciaCanceladaEvent evento) {
        publicar(EmailOcorrenciaBuilder.cancelamento(evento));
    }

    private void publicar(EnviarEmailCommand comando) {
        if (comando.destinatario() == null || comando.destinatario().isBlank()) {
            log.warn("Comunicante sem e-mail, notificação {} não enviada", comando.chaveIdempotencia());
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
