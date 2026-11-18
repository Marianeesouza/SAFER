package ufrpe.br.safer.usuarios.dto;

import jakarta.validation.constraints.NotNull;
import ufrpe.br.safer.usuarios.model.cargo.TipoAcaoOcorrencia;

public record ValidarPermissaoDTO(
        @NotNull(message = "O tipo de ação é obrigatório")
        TipoAcaoOcorrencia acao,
        Long criadorOcorrenciaId,
        Long analistaResponsavelId
) {}
