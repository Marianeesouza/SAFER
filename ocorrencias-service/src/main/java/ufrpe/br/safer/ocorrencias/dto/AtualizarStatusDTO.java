package ufrpe.br.safer.ocorrencias.dto;

import jakarta.validation.constraints.NotNull;
import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;

public record AtualizarStatusDTO(
        @NotNull(message = "O novo status é obrigatório")
        StatusOcorrencia novoStatus,

        @NotNull(message = "O ID do analista é obrigatório")
        Long analistaId,

        String observacao
) {}
