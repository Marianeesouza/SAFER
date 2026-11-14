package ufrpe.br.safer.ocorrencias.dto;

import ufrpe.br.safer.ocorrencias.model.Ocorrencia;
import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;

import java.time.LocalDateTime;
import java.util.List;

public record OcorrenciaDetalhadaDTO(
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
        List<HistoricoOcorrenciaDTO> historico,
        List<String> imagens
) {
    public static OcorrenciaDetalhadaDTO fromEntity(Ocorrencia o, List<HistoricoOcorrenciaDTO> historico) {
        return new OcorrenciaDetalhadaDTO(
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
                historico,
                o.getImagens() != null ? List.copyOf(o.getImagens()) : List.of()
        );
    }
}
