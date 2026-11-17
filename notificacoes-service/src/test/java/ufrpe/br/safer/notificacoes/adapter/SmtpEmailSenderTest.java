package ufrpe.br.safer.notificacoes.adapter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpEmailSenderTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final SmtpEmailSender sender = new SmtpEmailSender(mailSender, "safer@ufrpe.br");

    @Test
    void enviaMensagemComRemetenteDestinatarioAssuntoECorpo() {
        sender.enviar("ana@ufrpe.br", "Assunto", "Corpo");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mensagem = captor.getValue();
        assertEquals("safer@ufrpe.br", mensagem.getFrom());
        assertArrayEquals(new String[] {"ana@ufrpe.br"}, mensagem.getTo());
        assertEquals("Assunto", mensagem.getSubject());
        assertEquals("Corpo", mensagem.getText());
    }

    @Test
    void traduzFalhaDoProvedorParaEmailEnvioException() {
        MailSendException causa = new MailSendException("smtp fora do ar");
        doThrow(causa).when(mailSender).send(any(SimpleMailMessage.class));

        EmailEnvioException erro = assertThrows(EmailEnvioException.class, () -> sender.enviar("ana@ufrpe.br", "A", "C"));

        assertSame(causa, erro.getCause());
    }
}
