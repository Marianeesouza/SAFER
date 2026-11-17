package ufrpe.br.safer.notificacoes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ufrpe.br.safer.notificacoes.adapter.EmailEnvioException;
import ufrpe.br.safer.notificacoes.adapter.EmailSender;
import ufrpe.br.safer.notificacoes.messaging.EnviarEmailCommand;
import ufrpe.br.safer.notificacoes.model.Notificacao;
import ufrpe.br.safer.notificacoes.model.StatusNotificacao;
import ufrpe.br.safer.notificacoes.repository.NotificacaoRepository;

class NotificacaoServiceTest {

    private final EmailSender emailSender = mock(EmailSender.class);
    private final NotificacaoRepository repository = mock(NotificacaoRepository.class);
    private final NotificacaoService service = new NotificacaoService(emailSender, repository);

    private final EnviarEmailCommand comando =
            new EnviarEmailCommand("chave-1", "ana@ufrpe.br", "Assunto", "Corpo");

    @Test
    void enviaEmailEGravaNotificacaoEnviada() {
        when(repository.findByChaveIdempotencia("chave-1")).thenReturn(Optional.empty());

        service.enviar(comando);

        verify(emailSender).enviar("ana@ufrpe.br", "Assunto", "Corpo");
        var salva = capturarSalva();
        assertEquals(StatusNotificacao.ENVIADA, salva.getStatus());
        assertEquals(1, salva.getTentativas());
        assertEquals("chave-1", salva.getChaveIdempotencia());
    }

    @Test
    void ignoraComandoJaEnviado() {
        when(repository.findByChaveIdempotencia("chave-1"))
                .thenReturn(Optional.of(Notificacao.primeiraTentativa("chave-1", "ana@ufrpe.br", "Assunto", true)));

        service.enviar(comando);

        verify(emailSender, never()).enviar(any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void registraFalhaERelancaParaReentrega() {
        var causa = new EmailEnvioException("smtp fora do ar", null);
        when(repository.findByChaveIdempotencia("chave-1")).thenReturn(Optional.empty());
        doThrow(causa).when(emailSender).enviar(any(), any(), any());

        var erro = assertThrows(EmailEnvioException.class, () -> service.enviar(comando));

        assertSame(causa, erro);
        var salva = capturarSalva();
        assertEquals(StatusNotificacao.FALHA, salva.getStatus());
        assertEquals(1, salva.getTentativas());
    }

    @Test
    void reentregaAposFalhaReaproveitaRegistroEMarcaEnviada() {
        var comFalha = Notificacao.primeiraTentativa("chave-1", "ana@ufrpe.br", "Assunto", false);
        when(repository.findByChaveIdempotencia("chave-1")).thenReturn(Optional.of(comFalha));

        service.enviar(comando);

        verify(emailSender).enviar("ana@ufrpe.br", "Assunto", "Corpo");
        var salva = capturarSalva();
        assertSame(comFalha, salva);
        assertEquals(StatusNotificacao.ENVIADA, salva.getStatus());
        assertEquals(2, salva.getTentativas());
    }

    private Notificacao capturarSalva() {
        var captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}
