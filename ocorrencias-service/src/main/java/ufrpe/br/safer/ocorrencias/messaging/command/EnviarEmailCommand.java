package ufrpe.br.safer.ocorrencias.messaging.command;

import java.io.Serializable;

public record EnviarEmailCommand(
        String chaveIdempotencia,
        String destinatario,
        String assunto,
        String corpo
) implements Serializable {}
