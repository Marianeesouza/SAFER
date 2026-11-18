package ufrpe.br.safer.ocorrencias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarOcorrenciaDTO(
        @NotBlank(message = "A justificativa do cancelamento é obrigatória")
        @Size(max = 1000, message = "A justificativa não pode exceder 1000 caracteres")
        String justificativa
) {}
