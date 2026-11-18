package ufrpe.br.safer.ocorrencias.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "safer.ocorrencias.exchange";

    // Eventos de domínio. Este serviço não declara filas para eles: quem quiser consumir declara a
    // própria fila neste exchange, com a routing key desejada.
    public static final String ROUTING_KEY_REGISTRADA = "ocorrencia.registrada";
    public static final String ROUTING_KEY_STATUS_ALTERADO = "ocorrencia.status-alterado";
    public static final String ROUTING_KEY_CANCELADA = "ocorrencia.cancelada";

    // A fila é declarada pelo notificacoes-service, dono do consumo.
    public static final String ROUTING_KEY_ENVIAR_EMAIL = "notificacao.email.enviar";

    @Bean
    public DirectExchange ocorrenciasExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
