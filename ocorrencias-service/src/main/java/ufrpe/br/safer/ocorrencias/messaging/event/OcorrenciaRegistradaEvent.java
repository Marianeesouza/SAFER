package ufrpe.br.safer.ocorrencias.messaging.event;

import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;

import java.io.Serializable;
import java.time.LocalDateTime;

public record OcorrenciaRegistradaEvent(
        Long ocorrenciaId,
        String protocolo,
        String comunicanteNome,
        String comunicanteEmail,
        String descricaoAnimal,
        String endereco,
        StatusOcorrencia status,
        LocalDateTime dataRegistro
) implements Serializable {}
