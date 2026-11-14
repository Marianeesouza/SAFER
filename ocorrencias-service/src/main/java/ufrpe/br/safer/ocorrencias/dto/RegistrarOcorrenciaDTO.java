package ufrpe.br.safer.ocorrencias.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegistrarOcorrenciaDTO(
        Long usuarioId,

        @NotNull(message = "Os dados do comunicante são obrigatórios")
        @Valid
        ComunicanteDTO comunicante,

        @NotNull(message = "A localização da ocorrência é obrigatória")
        @Valid
        LocalizacaoDTO localizacao,

        @NotBlank(message = "A descrição do animal e da situação é obrigatória")
        @Size(max = 2000, message = "A descrição não pode exceder 2000 caracteres")
        String descricaoAnimal,

        @Size(max = 5, message = "Máximo de 5 imagens por ocorrência")
        List<String> imagens
) {}
