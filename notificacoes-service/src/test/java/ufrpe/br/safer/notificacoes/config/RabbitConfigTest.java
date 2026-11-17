package ufrpe.br.safer.notificacoes.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RabbitConfigTest {

    private final RabbitConfig config = new RabbitConfig();

    @Test
    void filaPrincipalEncaminhaRejeitadasParaDlq() {
        var fila = config.enviarEmailQueue();

        assertEquals(RabbitConfig.QUEUE_ENVIAR_EMAIL, fila.getName());
        assertTrue(fila.isDurable());
        assertEquals("", fila.getArguments().get("x-dead-letter-exchange"));
        assertEquals(RabbitConfig.QUEUE_ENVIAR_EMAIL_DLQ, fila.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void dlqEhDuravel() {
        var dlq = config.enviarEmailDlq();

        assertEquals(RabbitConfig.QUEUE_ENVIAR_EMAIL_DLQ, dlq.getName());
        assertTrue(dlq.isDurable());
    }

    @Test
    void bindingLigaFilaAoExchangeDeOcorrenciasComARoutingKeyDoComando() {
        var binding = config.bindingEnviarEmail(config.enviarEmailQueue(), config.ocorrenciasExchange());

        assertEquals(RabbitConfig.QUEUE_ENVIAR_EMAIL, binding.getDestination());
        assertEquals(RabbitConfig.EXCHANGE_NAME, binding.getExchange());
        assertEquals("notificacao.email.enviar", binding.getRoutingKey());
    }
}
