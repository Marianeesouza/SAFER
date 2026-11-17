package ufrpe.br.safer.notificacoes.messaging;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarEmailCommand(
        @NotBlank @Size(max = 200) String chaveIdempotencia, //identifica o envio; a mesma chave nunca é enviada duas vezes
        @NotBlank @Email @Size(max = 150) String destinatario,
        @NotBlank @Size(max = 255) String assunto,
        @NotBlank String corpo
) {}
