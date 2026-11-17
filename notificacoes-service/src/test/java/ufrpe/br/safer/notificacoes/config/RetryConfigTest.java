package ufrpe.br.safer.notificacoes.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

class RetryConfigTest {

    @Test
    void retryLimitadoComBackoffENaoReenfileiraRejeitadas() throws IOException {
        StandardEnvironment ambiente = new StandardEnvironment();
        new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))
                .forEach(ambiente.getPropertySources()::addLast);

        RabbitProperties rabbit = Binder.get(ambiente).bind("spring.rabbitmq", RabbitProperties.class).get();
        RabbitProperties.SimpleContainer listener = rabbit.getListener().getSimple();

        assertFalse(listener.getDefaultRequeueRejected());
        assertTrue(listener.getRetry().isEnabled());
        assertEquals(2, listener.getRetry().getMaxRetries());
        assertEquals(Duration.ofSeconds(1), listener.getRetry().getInitialInterval());
        assertEquals(2.0, listener.getRetry().getMultiplier());
        assertEquals(Duration.ofSeconds(10), listener.getRetry().getMaxInterval());
    }
}
