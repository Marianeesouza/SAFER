package ufrpe.br.safer.ocorrencias.client.dto;

import jakarta.validation.constraints.NotNull;

public record ValidarPermissaoDTO(
        @NotNull
        TipoAcaoOcorrencia acao,
        Long criadorOcorrenciaId,
        Long analistaResponsavelId
) {}
