package ufrpe.br.safer.ocorrencias.messaging.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public record OcorrenciaCanceladaEvent(
        Long ocorrenciaId,
        String protocolo,
        String comunicanteEmail,
        Long usuarioCancelamentoId,
        String justificativa,
        LocalDateTime dataCancelamento
) implements Serializable {}
