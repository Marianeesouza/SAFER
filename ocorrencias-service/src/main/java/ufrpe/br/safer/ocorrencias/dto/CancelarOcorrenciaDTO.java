package ufrpe.br.safer.ocorrencias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CancelarOcorrenciaDTO(
        @NotNull(message = "O ID do usuário responsável pelo cancelamento é obrigatório")
        Long usuarioId,

        @NotBlank(message = "A justificativa de cancelamento é obrigatória")
        @Size(max = 500, message = "A justificativa não pode exceder 500 caracteres")
        String justificativa
) {}
