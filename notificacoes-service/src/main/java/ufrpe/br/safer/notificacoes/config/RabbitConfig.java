package ufrpe.br.safer.notificacoes.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Mesmo exchange declarado pelo ocorrencias-service
    public static final String EXCHANGE_NAME = "safer.ocorrencias.exchange";

    public static final String QUEUE_ENVIAR_EMAIL = "notificacoes.email.enviar";
    public static final String QUEUE_ENVIAR_EMAIL_DLQ = "notificacoes.email.enviar.dlq";
    public static final String ROUTING_KEY_ENVIAR_EMAIL = "notificacao.email.enviar";

    @Bean
    public DirectExchange ocorrenciasExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue enviarEmailQueue() {
        // Mensagens rejeitadas vão para a DLQ pelo exchange padrão, usando o nome da fila como chave
        return QueueBuilder.durable(QUEUE_ENVIAR_EMAIL)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_ENVIAR_EMAIL_DLQ)
                .build();
    }

    @Bean
    public Queue enviarEmailDlq() {
        return QueueBuilder.durable(QUEUE_ENVIAR_EMAIL_DLQ).build();
    }

    @Bean
    public Binding bindingEnviarEmail(Queue enviarEmailQueue, DirectExchange ocorrenciasExchange) {
        return BindingBuilder.bind(enviarEmailQueue).to(ocorrenciasExchange).with(ROUTING_KEY_ENVIAR_EMAIL);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
