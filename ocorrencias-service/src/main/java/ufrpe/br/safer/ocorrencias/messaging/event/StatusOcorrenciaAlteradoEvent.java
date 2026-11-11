package ufrpe.br.safer.ocorrencias.messaging.event;

import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;

import java.io.Serializable;
import java.time.LocalDateTime;

public record StatusOcorrenciaAlteradoEvent(
        Long ocorrenciaId,
        String protocolo,
        String comunicanteEmail,
        StatusOcorrencia statusAnterior,
        StatusOcorrencia novoStatus,
        Long analistaId,
        String observacao,
        LocalDateTime dataAlteracao
) implements Serializable {}
