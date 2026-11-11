package ufrpe.br.safer.ocorrencias.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "safer.ocorrencias.exchange";

    public static final String QUEUE_REGISTRADA = "safer.ocorrencia.registrada";
    public static final String ROUTING_KEY_REGISTRADA = "ocorrencia.registrada";

    public static final String QUEUE_STATUS_ALTERADO = "safer.ocorrencia.status-alterado";
    public static final String ROUTING_KEY_STATUS_ALTERADO = "ocorrencia.status-alterado";

    public static final String QUEUE_CANCELADA = "safer.ocorrencia.cancelada";
    public static final String ROUTING_KEY_CANCELADA = "ocorrencia.cancelada";

    @Bean
    public DirectExchange ocorrenciasExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue ocorrenciaRegistradaQueue() {
        return new Queue(QUEUE_REGISTRADA, true);
    }

    @Bean
    public Binding bindingOcorrenciaRegistrada(Queue ocorrenciaRegistradaQueue, DirectExchange ocorrenciasExchange) {
        return BindingBuilder.bind(ocorrenciaRegistradaQueue).to(ocorrenciasExchange).with(ROUTING_KEY_REGISTRADA);
    }

    @Bean
    public Queue ocorrenciaStatusAlteradoQueue() {
        return new Queue(QUEUE_STATUS_ALTERADO, true);
    }

    @Bean
    public Binding bindingOcorrenciaStatusAlterado(Queue ocorrenciaStatusAlteradoQueue, DirectExchange ocorrenciasExchange) {
        return BindingBuilder.bind(ocorrenciaStatusAlteradoQueue).to(ocorrenciasExchange).with(ROUTING_KEY_STATUS_ALTERADO);
    }

    @Bean
    public Queue ocorrenciaCanceladaQueue() {
        return new Queue(QUEUE_CANCELADA, true);
    }

    @Bean
    public Binding bindingOcorrenciaCancelada(Queue ocorrenciaCanceladaQueue, DirectExchange ocorrenciasExchange) {
        return BindingBuilder.bind(ocorrenciaCanceladaQueue).to(ocorrenciasExchange).with(ROUTING_KEY_CANCELADA);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
