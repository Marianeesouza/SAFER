package ufrpe.br.safer.notificacoes.messaging;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EnviarEmailCommand(
        @NotBlank String chaveIdempotencia, //identifica o envio; a mesma chave nunca é enviada duas vezes
        @NotBlank @Email String destinatario,
        @NotBlank String assunto,
        @NotBlank String corpo
) {}
