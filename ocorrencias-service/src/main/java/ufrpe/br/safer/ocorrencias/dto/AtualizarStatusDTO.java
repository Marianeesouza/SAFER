package ufrpe.br.safer.ocorrencias.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;

public record AtualizarStatusDTO(
        @NotNull(message = "O novo status é obrigatório")
        StatusOcorrencia novoStatus,

        @NotNull(message = "O ID do analista é obrigatório")
        Long analistaId,

        @Size(max = 1000, message = "A observação não pode exceder 1000 caracteres")
        String observacao
) {}
