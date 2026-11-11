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
        List<HistoricoOcorrenciaDTO> historico
) {
    public static OcorrenciaDetalhadaDTO fromEntity(Ocorrencia o, List<HistoricoOcorrenciaDTO> historico) {
        ComunicanteDTO comunicante = new ComunicanteDTO(
                o.getComunicanteNome(),
                o.getComunicanteEmail(),
                o.getComunicanteTelefone(),
                o.getComunicanteRelacaoUfrpe()
        );
        LocalizacaoDTO localizacao = new LocalizacaoDTO(
                o.getLatitude(),
                o.getLongitude(),
                o.getEndereco()
        );
        return new OcorrenciaDetalhadaDTO(
                o.getId(),
                o.getProtocolo(),
                o.getUsuarioId(),
                comunicante,
                localizacao,
                o.getDescricaoAnimal(),
                o.getStatus(),
                o.getAnalistaResponsavelId(),
                o.getCriadoEm(),
                o.getAtualizadoEm(),
                historico
        );
    }
}
