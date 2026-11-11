package ufrpe.br.safer.ocorrencias.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ufrpe.br.safer.ocorrencias.client.GeocodificacaoClient;
import ufrpe.br.safer.ocorrencias.dto.AtualizarStatusDTO;
import ufrpe.br.safer.ocorrencias.dto.CancelarOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.dto.HistoricoOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.dto.OcorrenciaDetalhadaDTO;
import ufrpe.br.safer.ocorrencias.dto.OcorrenciaResponseDTO;
import ufrpe.br.safer.ocorrencias.dto.RegistrarOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.exception.OcorrenciaJaAssumidaException;
import ufrpe.br.safer.ocorrencias.exception.OcorrenciaNaoEncontradaException;
import ufrpe.br.safer.ocorrencias.exception.TransicaoStatusInvalidaException;
import ufrpe.br.safer.ocorrencias.messaging.OcorrenciaEventPublisher;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaCanceladaEvent;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaRegistradaEvent;
import ufrpe.br.safer.ocorrencias.messaging.event.StatusOcorrenciaAlteradoEvent;
import ufrpe.br.safer.ocorrencias.model.HistoricoOcorrencia;
import ufrpe.br.safer.ocorrencias.model.Ocorrencia;
import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;
import ufrpe.br.safer.ocorrencias.repository.HistoricoOcorrenciaRepository;
import ufrpe.br.safer.ocorrencias.repository.OcorrenciaRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OcorrenciasService {

    private final OcorrenciaRepository ocorrenciaRepository;
    private final HistoricoOcorrenciaRepository historicoOcorrenciaRepository;
    private final GeocodificacaoClient geocodificacaoClient;
    private final OcorrenciaEventPublisher eventPublisher;

    @Transactional
    public OcorrenciaResponseDTO registrarOcorrencia(RegistrarOcorrenciaDTO dto) {
        Double lat = dto.localizacao().latitude();
        Double lng = dto.localizacao().longitude();

        if ((lat == null && lng != null) || (lat != null && lng == null)) {
            throw new IllegalArgumentException("Latitude e longitude devem ser fornecidas juntas ou ambas ausentes.");
        }

        if (lat != null) {
            boolean noCampus = geocodificacaoClient.validarPontoNoCampus(lat, lng);
            if (!noCampus) {
                throw new IllegalArgumentException("As coordenadas informadas estão fora do perímetro do campus da UFRPE.");
            }
        } else {
            log.info("Ocorrência registrada no modo de contingência (endereço textual sem coordenadas prévias).");
        }

        LocalDateTime agora = LocalDateTime.now();
        String protocolo = gerarProtocoloUnico();

        Ocorrencia ocorrencia = Ocorrencia.builder()
                .protocolo(protocolo)
                .usuarioId(dto.usuarioId())
                .comunicanteNome(dto.comunicante().nome().trim())
                .comunicanteEmail(dto.comunicante().email().trim())
                .comunicanteTelefone(dto.comunicante().telefone())
                .comunicanteRelacaoUfrpe(dto.comunicante().relacaoUfrpe())
                .descricaoAnimal(dto.descricaoAnimal().trim())
                .latitude(lat)
                .longitude(lng)
                .endereco(dto.localizacao().endereco().trim())
                .status(StatusOcorrencia.EM_ABERTO)
                .criadoEm(agora)
                .atualizadoEm(agora)
                .build();

        Ocorrencia salva = ocorrenciaRepository.save(ocorrencia);

        HistoricoOcorrencia historicoInicial = HistoricoOcorrencia.builder()
                .ocorrencia(salva)
                .statusRegistrado(StatusOcorrencia.EM_ABERTO)
                .usuarioId(dto.usuarioId())
                .observacao("Ocorrência registrada no sistema.")
                .dataHora(agora)
                .build();

        historicoOcorrenciaRepository.save(historicoInicial);

        OcorrenciaRegistradaEvent evento = new OcorrenciaRegistradaEvent(
                salva.getId(),
                salva.getProtocolo(),
                salva.getComunicanteNome(),
                salva.getComunicanteEmail(),
                salva.getDescricaoAnimal(),
                salva.getEndereco(),
                salva.getStatus(),
                salva.getCriadoEm()
        );
        eventPublisher.publicarOcorrenciaRegistrada(evento);

        return OcorrenciaResponseDTO.fromEntity(salva);
    }

    @Transactional
    public OcorrenciaResponseDTO vincularAnalista(Long id, Long analistaId) {
        Ocorrencia ocorrencia = ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new OcorrenciaNaoEncontradaException("Ocorrência não encontrada com o ID: " + id));

        if (ocorrencia.getStatus() != StatusOcorrencia.EM_ABERTO || ocorrencia.getAnalistaResponsavelId() != null) {
            throw new OcorrenciaJaAssumidaException("Esta ocorrência já foi assumida por outro analista ou não está mais disponível para atendimento.");
        }

        LocalDateTime agora = LocalDateTime.now();
        int atualizados = ocorrenciaRepository.vincularAnalistaAtomicamente(
                id,
                analistaId,
                StatusOcorrencia.EM_ATENDIMENTO,
                StatusOcorrencia.EM_ABERTO,
                agora
        );

        if (atualizados == 0) {
            throw new OcorrenciaJaAssumidaException("Esta ocorrência já foi assumida por outro analista e não está mais disponível para atendimento.");
        }

        ocorrencia.setAnalistaResponsavelId(analistaId);
        ocorrencia.setStatus(StatusOcorrencia.EM_ATENDIMENTO);
        ocorrencia.setAtualizadoEm(agora);

        HistoricoOcorrencia historico = HistoricoOcorrencia.builder()
                .ocorrencia(ocorrencia)
                .statusRegistrado(StatusOcorrencia.EM_ATENDIMENTO)
                .usuarioId(analistaId)
                .observacao("Atendimento assumido pelo analista ID " + analistaId + ".")
                .dataHora(agora)
                .build();

        historicoOcorrenciaRepository.save(historico);

        StatusOcorrenciaAlteradoEvent evento = new StatusOcorrenciaAlteradoEvent(
                ocorrencia.getId(),
                ocorrencia.getProtocolo(),
                ocorrencia.getComunicanteEmail(),
                StatusOcorrencia.EM_ABERTO,
                StatusOcorrencia.EM_ATENDIMENTO,
                analistaId,
                historico.getObservacao(),
                agora
        );
        eventPublisher.publicarStatusAlterado(evento);

        return OcorrenciaResponseDTO.fromEntity(ocorrencia);
    }

    @Transactional
    public OcorrenciaResponseDTO atualizarStatus(Long id, AtualizarStatusDTO dto) {
        Ocorrencia ocorrencia = ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new OcorrenciaNaoEncontradaException("Ocorrência não encontrada com o ID: " + id));

        StatusOcorrencia statusAtual = ocorrencia.getStatus();
        StatusOcorrencia novoStatus = dto.novoStatus();

        if (!statusAtual.podeTransitarPara(novoStatus)) {
            throw new TransicaoStatusInvalidaException(
                    "Transição inválida de status: de '" + statusAtual + "' para '" + novoStatus + "'."
            );
        }

        LocalDateTime agora = LocalDateTime.now();
        ocorrencia.setStatus(novoStatus);
        ocorrencia.setAtualizadoEm(agora);
        Ocorrencia atualizada = ocorrenciaRepository.save(ocorrencia);

        String observacao = (dto.observacao() != null && !dto.observacao().isBlank())
                ? dto.observacao().trim()
                : "Status alterado para " + novoStatus;

        HistoricoOcorrencia historico = HistoricoOcorrencia.builder()
                .ocorrencia(atualizada)
                .statusRegistrado(novoStatus)
                .usuarioId(dto.analistaId())
                .observacao(observacao)
                .dataHora(agora)
                .build();

        historicoOcorrenciaRepository.save(historico);

        StatusOcorrenciaAlteradoEvent evento = new StatusOcorrenciaAlteradoEvent(
                atualizada.getId(),
                atualizada.getProtocolo(),
                atualizada.getComunicanteEmail(),
                statusAtual,
                novoStatus,
                dto.analistaId(),
                observacao,
                agora
        );
        eventPublisher.publicarStatusAlterado(evento);

        return OcorrenciaResponseDTO.fromEntity(atualizada);
    }

    @Transactional
    public OcorrenciaResponseDTO cancelarOcorrencia(Long id, CancelarOcorrenciaDTO dto) {
        Ocorrencia ocorrencia = ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new OcorrenciaNaoEncontradaException("Ocorrência não encontrada com o ID: " + id));

        if (ocorrencia.getStatus() != StatusOcorrencia.EM_ABERTO) {
            throw new TransicaoStatusInvalidaException(
                    "O cancelamento não pode ser realizado porque a ocorrência já está em atendimento ou finalizada."
            );
        }

        LocalDateTime agora = LocalDateTime.now();
        int cancelados = ocorrenciaRepository.cancelarOcorrenciaAtomicamente(
                id,
                StatusOcorrencia.CANCELADO,
                StatusOcorrencia.EM_ABERTO,
                agora
        );

        if (cancelados == 0) {
            throw new OcorrenciaJaAssumidaException(
                    "O cancelamento não pode ser realizado porque o estado da ocorrência foi alterado recentemente por outro usuário ou analista."
            );
        }

        ocorrencia.setStatus(StatusOcorrencia.CANCELADO);
        ocorrencia.setAtualizadoEm(agora);

        HistoricoOcorrencia historico = HistoricoOcorrencia.builder()
                .ocorrencia(ocorrencia)
                .statusRegistrado(StatusOcorrencia.CANCELADO)
                .usuarioId(dto.usuarioId())
                .observacao("Cancelamento: " + dto.justificativa().trim())
                .dataHora(agora)
                .build();

        historicoOcorrenciaRepository.save(historico);

        OcorrenciaCanceladaEvent evento = new OcorrenciaCanceladaEvent(
                ocorrencia.getId(),
                ocorrencia.getProtocolo(),
                ocorrencia.getComunicanteEmail(),
                dto.usuarioId(),
                dto.justificativa().trim(),
                agora
        );
        eventPublisher.publicarOcorrenciaCancelada(evento);

        return OcorrenciaResponseDTO.fromEntity(ocorrencia);
    }

    @Transactional(readOnly = true)
    public Optional<OcorrenciaDetalhadaDTO> buscarDetalhesPorId(Long id) {
        return ocorrenciaRepository.findById(id).map(this::montarOcorrenciaDetalhada);
    }

    @Transactional(readOnly = true)
    public Optional<OcorrenciaDetalhadaDTO> buscarDetalhesPorProtocolo(String protocolo) {
        return ocorrenciaRepository.findByProtocolo(protocolo).map(this::montarOcorrenciaDetalhada);
    }

    @Transactional(readOnly = true)
    public List<OcorrenciaResponseDTO> listarEmAberto() {
        return ocorrenciaRepository.findByStatusOrderByCriadoEmDesc(StatusOcorrencia.EM_ABERTO).stream()
                .map(OcorrenciaResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OcorrenciaResponseDTO> listarPorUsuario(Long usuarioId) {
        return ocorrenciaRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId).stream()
                .map(OcorrenciaResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OcorrenciaResponseDTO> listar(StatusOcorrencia status) {
        List<Ocorrencia> lista = (status != null)
                ? ocorrenciaRepository.findByStatusOrderByCriadoEmDesc(status)
                : ocorrenciaRepository.findAllByOrderByCriadoEmDesc();

        return lista.stream()
                .map(OcorrenciaResponseDTO::fromEntity)
                .toList();
    }

    private OcorrenciaDetalhadaDTO montarOcorrenciaDetalhada(Ocorrencia ocorrencia) {
        List<HistoricoOcorrenciaDTO> historico = historicoOcorrenciaRepository
                .findByOcorrenciaIdOrderByDataHoraAsc(ocorrencia.getId())
                .stream()
                .map(HistoricoOcorrenciaDTO::fromEntity)
                .toList();

        return OcorrenciaDetalhadaDTO.fromEntity(ocorrencia, historico);
    }

    private String gerarProtocoloUnico() {
        String dataPrefixo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String protocolo;
        do {
            String sufixo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            protocolo = "OC-" + dataPrefixo + "-" + sufixo;
        } while (ocorrenciaRepository.existsByProtocolo(protocolo));
        return protocolo;
    }
}
