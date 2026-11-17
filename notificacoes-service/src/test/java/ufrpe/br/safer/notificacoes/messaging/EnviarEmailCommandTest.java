package ufrpe.br.safer.notificacoes.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

class EnviarEmailCommandTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void comandoValidoNaoTemViolacoes() {
        var comando = new EnviarEmailCommand("chave-1", "ana@ufrpe.br", "Assunto", "Corpo");

        assertTrue(validator.validate(comando).isEmpty());
    }

    @Test
    void camposVaziosEEmailInvalidoSaoRejeitados() {
        var comando = new EnviarEmailCommand(" ", "nao-e-email", "", null);

        // chave, destinatario, assunto e corpo
        assertEquals(4, validator.validate(comando).size());
    }

    @Test
    void camposMaioresQueAsColunasDoBancoSaoRejeitados() {
        var comando = new EnviarEmailCommand("c".repeat(201), "ana@ufrpe.br", "a".repeat(256), "Corpo");

        // chave e assunto
        assertEquals(2, validator.validate(comando).size());
    }

    @Test
    void desserializaJsonPublicadoPorOutroServico() {
        var json = """
                {"chaveIdempotencia":"ocorrencia-1-registro","destinatario":"ana@ufrpe.br",
                 "assunto":"Ocorrência registrada","corpo":"Protocolo 123","extra":"ignorado"}
                """;
        var props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        // o tipo vem do parâmetro do listener
        props.setInferredArgumentType(EnviarEmailCommand.class);

        var convertido = new JacksonJsonMessageConverter()
                .fromMessage(new Message(json.getBytes(StandardCharsets.UTF_8), props));

        assertEquals(new EnviarEmailCommand("ocorrencia-1-registro", "ana@ufrpe.br",
                "Ocorrência registrada", "Protocolo 123"), convertido);
    }
}
