package ufrpe.br.safer.notificacoes.messaging;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ufrpe.br.safer.notificacoes.config.RabbitConfig;
import ufrpe.br.safer.notificacoes.service.NotificacaoService;

@Component
public class EnviarEmailListener {

    private final NotificacaoService notificacaoService;
    private final Validator validator;

    public EnviarEmailListener(NotificacaoService notificacaoService, Validator validator) {
        this.notificacaoService = notificacaoService;
        this.validator = validator;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_ENVIAR_EMAIL)
    public void receber(EnviarEmailCommand comando) {
        Set<ConstraintViolation<EnviarEmailCommand>> violacoes = validator.validate(comando);
        if (!violacoes.isEmpty()) {
            String detalhes = violacoes.stream()
                    .map(ConstraintViolation::getPropertyPath)
                    .map(Object::toString)
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new AmqpRejectAndDontRequeueException("Comando de e-mail inválido, campos: " + detalhes);
        }
        notificacaoService.enviar(comando);
    }
}
