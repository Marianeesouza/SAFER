package ufrpe.br.safer.ocorrencias.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import ufrpe.br.safer.ocorrencias.client.UsuariosClient;
import ufrpe.br.safer.ocorrencias.config.RabbitConfig;
import ufrpe.br.safer.ocorrencias.dto.DestinatarioDTO;
import ufrpe.br.safer.ocorrencias.messaging.command.EnviarEmailCommand;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaCanceladaEvent;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaRegistradaEvent;
import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;
import ufrpe.br.safer.ocorrencias.messaging.event.StatusOcorrenciaAlteradoEvent;

class EmailNotificacaoPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final UsuariosClient usuariosClient = mock(UsuariosClient.class);
    private final EmailNotificacaoPublisher publisher =
            new EmailNotificacaoPublisher(rabbitTemplate, usuariosClient, "http://sistema.local");

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

    @Test
    void notificaCadaAnalistaComSuaPropriaChave() {
        OcorrenciaRegistradaEvent evento = new OcorrenciaRegistradaEvent(7L, "OC-1", "Ana", "ana@ufrpe.br",
                "Sagui ferido", "Bloco A", StatusOcorrencia.EM_ABERTO, AGORA);
        DestinatarioDTO admin = new DestinatarioDTO(1L, "Admin", "admin@ufrpe.br");
        DestinatarioDTO analista = new DestinatarioDTO(2L, "Carlos", "carlos@ufrpe.br");
        when(usuariosClient.listarDestinatariosDeNovasOcorrencias()).thenReturn(List.of(admin, analista));

        publisher.notificarAnalistas(evento);

        verify(rabbitTemplate).convertAndSend(RabbitConfig.EXCHANGE_NAME, "notificacao.email.enviar",
                EmailOcorrenciaBuilder.novaOcorrenciaParaAnalista(evento, admin, "http://sistema.local"));
        verify(rabbitTemplate).convertAndSend(RabbitConfig.EXCHANGE_NAME, "notificacao.email.enviar",
                EmailOcorrenciaBuilder.novaOcorrenciaParaAnalista(evento, analista, "http://sistema.local"));
    }

    @Test
    void semDestinatariosNaoPublicaNada() {
        OcorrenciaRegistradaEvent evento = new OcorrenciaRegistradaEvent(7L, "OC-1", "Ana", "ana@ufrpe.br",
                "Sagui ferido", "Bloco A", StatusOcorrencia.EM_ABERTO, AGORA);
        when(usuariosClient.listarDestinatariosDeNovasOcorrencias()).thenReturn(List.of());

        publisher.notificarAnalistas(evento);

        verifyNoInteractions(rabbitTemplate);
    }
}
