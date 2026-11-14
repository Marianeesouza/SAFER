package ufrpe.br.safer.ocorrencias.dto;

import ufrpe.br.safer.ocorrencias.model.Ocorrencia;
import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;

import java.time.LocalDateTime;
import java.util.List;

public record OcorrenciaResponseDTO(
        Long id,
        String protocolo,
        Long usuarioId,
        ComunicanteDTO comunicante,
        LocalizacaoDTO localizacao,
        String descricaoAnimal,
        StatusOcorrencia status,
        Long analistaResponsavelId,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        List<String> imagens
) {
    public static OcorrenciaResponseDTO fromEntity(Ocorrencia o) {
        return new OcorrenciaResponseDTO(
                o.getId(),
                o.getProtocolo(),
                o.getUsuarioId(),
                ComunicanteDTO.fromEntity(o),
                LocalizacaoDTO.fromEntity(o),
                o.getDescricaoAnimal(),
                o.getStatus(),
                o.getAnalistaResponsavelId(),
                o.getCriadoEm(),
                o.getAtualizadoEm(),
                o.getImagens() != null ? List.copyOf(o.getImagens()) : List.of()
        );
    }
}
