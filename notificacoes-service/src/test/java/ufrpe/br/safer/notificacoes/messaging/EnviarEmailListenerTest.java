package ufrpe.br.safer.notificacoes.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import ufrpe.br.safer.notificacoes.service.NotificacaoService;

class EnviarEmailListenerTest {

    private final NotificacaoService service = mock(NotificacaoService.class);
    private final EnviarEmailListener listener =
            new EnviarEmailListener(service, Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void delegaComandoValidoAoServico() {
        EnviarEmailCommand comando = new EnviarEmailCommand("chave-1", "ana@ufrpe.br", "Assunto", "Corpo");

        listener.receber(comando);

        verify(service).enviar(comando);
    }

    @Test
    void rejeitaComandoInvalidoSemReenfileirarENaoChamaServico() {
        EnviarEmailCommand comando = new EnviarEmailCommand("chave-1", "nao-e-email", "Assunto", "Corpo");

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.receber(comando));

        verify(service, never()).enviar(any());
    }
}
