package ufrpe.br.safer.ocorrencias.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;

public record AtualizarStatusDTO(
        @NotNull(message = "O novo status é obrigatório")
        @Schema(description = "Código do novo status: " + StatusOcorrencia.DESCRICAO_CODIGOS, example = "3")
        Integer novoStatus,

        @NotNull(message = "O ID do analista é obrigatório")
        Long analistaId,

        @Size(max = 1000, message = "A observação não pode exceder 1000 caracteres")
        String observacao
) {}
