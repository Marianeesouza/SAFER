package ufrpe.br.safer.notificacoes.service;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ufrpe.br.safer.notificacoes.adapter.EmailEnvioException;
import ufrpe.br.safer.notificacoes.adapter.EmailSender;
import ufrpe.br.safer.notificacoes.messaging.EnviarEmailCommand;
import ufrpe.br.safer.notificacoes.model.Notificacao;
import ufrpe.br.safer.notificacoes.repository.NotificacaoRepository;

@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    private final EmailSender emailSender;
    private final NotificacaoRepository repository;

    public NotificacaoService(EmailSender emailSender, NotificacaoRepository repository) {
        this.emailSender = emailSender;
        this.repository = repository;
    }

    public void enviar(EnviarEmailCommand comando) {
        Optional<Notificacao> existente = repository.findByChaveIdempotencia(comando.chaveIdempotencia());
        if (existente.isPresent() && existente.get().foiEnviada()) {
            log.info("E-mail {} já enviado, ignorando mensagem repetida", comando.chaveIdempotencia());
            return;
        }

        EmailEnvioException falha = null;
        try {
            emailSender.enviar(comando.destinatario(), comando.assunto(), comando.corpo());
        } catch (EmailEnvioException e) {
            falha = e;
        }
        boolean enviada = falha == null;

        if (existente.isPresent()) {
            Notificacao notificacao = existente.get();
            notificacao.registrarTentativa(enviada);
            repository.save(notificacao);
        } else {
            repository.save(Notificacao.primeiraTentativa(
                    comando.chaveIdempotencia(), comando.destinatario(), comando.assunto(), enviada));
        }

        if (falha != null) {
            throw falha;
        }
    }
}
