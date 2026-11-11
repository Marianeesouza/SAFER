package ufrpe.br.safer.ocorrencias.dto;

import jakarta.validation.constraints.NotNull;

public record VincularAnalistaDTO(
        @NotNull(message = "O ID do analista é obrigatório")
        Long analistaId
) {}
