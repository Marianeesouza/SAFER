package ufrpe.br.safer.ocorrencias.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegistrarOcorrenciaDTO(
        Long usuarioId,

        @NotNull(message = "Os dados do comunicante são obrigatórios")
        @Valid
        ComunicanteDTO comunicante,

        @NotNull(message = "A localização da ocorrência é obrigatória")
        @Valid
        LocalizacaoDTO localizacao,

        @NotBlank(message = "A descrição do animal e da situação é obrigatória")
        String descricaoAnimal
) {}
