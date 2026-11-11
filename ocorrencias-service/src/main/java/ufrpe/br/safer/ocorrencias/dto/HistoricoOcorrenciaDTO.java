package ufrpe.br.safer.ocorrencias.dto;

import ufrpe.br.safer.ocorrencias.model.HistoricoOcorrencia;
import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;

import java.time.LocalDateTime;

public record HistoricoOcorrenciaDTO(
        Long id,
        StatusOcorrencia statusRegistrado,
        Long usuarioId,
        String observacao,
        LocalDateTime dataHora
) {
    public static HistoricoOcorrenciaDTO fromEntity(HistoricoOcorrencia h) {
        return new HistoricoOcorrenciaDTO(
                h.getId(),
                h.getStatusRegistrado(),
                h.getUsuarioId(),
                h.getObservacao(),
                h.getDataHora()
        );
    }
}
