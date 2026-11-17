package ufrpe.br.safer.ocorrencias.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import ufrpe.br.safer.ocorrencias.config.RabbitConfig;
import ufrpe.br.safer.ocorrencias.messaging.command.EnviarEmailCommand;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaCanceladaEvent;
import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;
import ufrpe.br.safer.ocorrencias.messaging.event.StatusOcorrenciaAlteradoEvent;

class EmailNotificacaoPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final EmailNotificacaoPublisher publisher = new EmailNotificacaoPublisher(rabbitTemplate);

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 16, 10, 0);

    @Test
    void publicaComandoNoExchangeComARoutingKeyDoNotificacoes() {
        StatusOcorrenciaAlteradoEvent evento = new StatusOcorrenciaAlteradoEvent(7L, "OC-1", "ana@ufrpe.br",
                StatusOcorrencia.EM_ATENDIMENTO, StatusOcorrencia.FINALIZADO_COM_SUCESSO, 3L, null, AGORA);

        publisher.notificarStatusAlterado(evento);

        verify(rabbitTemplate).convertAndSend(
                RabbitConfig.EXCHANGE_NAME,
                "notificacao.email.enviar",
                EmailOcorrenciaBuilder.statusAlterado(evento));
    }

    @Test
    void naoPublicaQuandoComunicanteNaoTemEmail() {
        OcorrenciaCanceladaEvent evento = new OcorrenciaCanceladaEvent(7L, "OC-1", " ", 1L, "motivo", AGORA);

        publisher.notificarCancelamento(evento);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void falhaNoBrokerNaoPropagaExcecao() {
        OcorrenciaCanceladaEvent evento = new OcorrenciaCanceladaEvent(7L, "OC-1", "ana@ufrpe.br", 1L, "motivo", AGORA);
        doThrow(new AmqpConnectException(new RuntimeException("broker fora")))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(EnviarEmailCommand.class));

        publisher.notificarCancelamento(evento);
    }
}
