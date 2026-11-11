package ufrpe.br.safer.ocorrencias.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import ufrpe.br.safer.ocorrencias.config.RabbitConfig;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaCanceladaEvent;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaRegistradaEvent;
import ufrpe.br.safer.ocorrencias.messaging.event.StatusOcorrenciaAlteradoEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class OcorrenciaEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publicarOcorrenciaRegistrada(OcorrenciaRegistradaEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE_NAME,
                    RabbitConfig.ROUTING_KEY_REGISTRADA,
                    event
            );
            log.info("Evento OcorrenciaRegistrada publicado com sucesso para o protocolo {}", event.protocolo());
        } catch (Exception e) {
            log.error("Falha ao publicar evento OcorrenciaRegistrada para o protocolo {}: {}", event.protocolo(), e.getMessage());
        }
    }

    public void publicarStatusAlterado(StatusOcorrenciaAlteradoEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE_NAME,
                    RabbitConfig.ROUTING_KEY_STATUS_ALTERADO,
                    event
            );
            log.info("Evento StatusOcorrenciaAlterado publicado com sucesso para o protocolo {}: {} -> {}",
                    event.protocolo(), event.statusAnterior(), event.novoStatus());
        } catch (Exception e) {
            log.error("Falha ao publicar evento StatusOcorrenciaAlterado para o protocolo {}: {}", event.protocolo(), e.getMessage());
        }
    }

    public void publicarOcorrenciaCancelada(OcorrenciaCanceladaEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE_NAME,
                    RabbitConfig.ROUTING_KEY_CANCELADA,
                    event
            );
            log.info("Evento OcorrenciaCancelada publicado com sucesso para o protocolo {}", event.protocolo());
        } catch (Exception e) {
            log.error("Falha ao publicar evento OcorrenciaCancelada para o protocolo {}: {}", event.protocolo(), e.getMessage());
        }
    }
}
