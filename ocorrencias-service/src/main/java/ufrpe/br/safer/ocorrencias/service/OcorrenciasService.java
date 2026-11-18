package ufrpe.br.safer.ocorrencias.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ufrpe.br.safer.ocorrencias.client.GeocodificacaoClient;
import ufrpe.br.safer.ocorrencias.client.UsuariosClient;
import ufrpe.br.safer.ocorrencias.client.dto.TipoAcaoOcorrencia;
import ufrpe.br.safer.ocorrencias.dto.AtualizarStatusDTO;
import ufrpe.br.safer.ocorrencias.dto.CancelarOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.dto.HistoricoOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.dto.OcorrenciaDetalhadaDTO;
import ufrpe.br.safer.ocorrencias.dto.OcorrenciaResponseDTO;
import ufrpe.br.safer.ocorrencias.dto.RegistrarOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.exception.AcessoNegadoException;
import ufrpe.br.safer.ocorrencias.exception.BusinessRuleException;
import ufrpe.br.safer.ocorrencias.exception.OcorrenciaJaAssumidaException;
import ufrpe.br.safer.ocorrencias.exception.OcorrenciaNaoEncontradaException;
import ufrpe.br.safer.ocorrencias.exception.TransicaoStatusInvalidaException;
import ufrpe.br.safer.ocorrencias.messaging.EmailNotificacaoPublisher;
import ufrpe.br.safer.ocorrencias.messaging.OcorrenciaEventPublisher;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaCanceladaEvent;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaRegistradaEvent;
import ufrpe.br.safer.ocorrencias.messaging.event.StatusOcorrenciaAlteradoEvent;
import ufrpe.br.safer.ocorrencias.model.Coordenadas;
import ufrpe.br.safer.ocorrencias.model.DadosComunicante;
import ufrpe.br.safer.ocorrencias.model.DadosLocalizacao;
import ufrpe.br.safer.ocorrencias.model.HistoricoOcorrencia;
import ufrpe.br.safer.ocorrencias.model.Ocorrencia;
import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;
import ufrpe.br.safer.ocorrencias.repository.HistoricoOcorrenciaRepository;
import ufrpe.br.safer.ocorrencias.repository.OcorrenciaRepository;
import ufrpe.br.safer.ocorrencias.security.UsuarioAutenticado;

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
    private final UsuariosClient usuariosClient;
    private final OcorrenciaEventPublisher eventPublisher;
    private final EmailNotificacaoPublisher emailPublisher;

    @Transactional
    public OcorrenciaResponseDTO registrarOcorrencia(RegistrarOcorrenciaDTO dto) {
        Double lat = dto.localizacao().latitude();
        Double lng = dto.localizacao().longitude();

        if ((lat == null) != (lng == null)) {
            throw new BusinessRuleException("Latitude e longitude devem ser fornecidas juntas ou ambas ausentes.");
        }

        if (lat != null) {
            if (!geocodificacaoClient.validarPontoNoCampus(lat, lng)) {
                throw new BusinessRuleException("As coordenadas informadas estão fora do perímetro do campus da UFRPE.");
            }
        } else {
            log.info("Ocorrência registrada no modo de contingência (endereço textual sem coordenadas prévias).");
        }

        LocalDateTime agora = LocalDateTime.now();

        DadosComunicante comunicante = DadosComunicante.builder()
                .nome(dto.comunicante().nome().trim())
                .email(dto.comunicante().email().trim())
                .telefone(dto.comunicante().telefone())
                .relacaoUfrpe(dto.comunicante().relacaoUfrpe())
                .build();

        Coordenadas coordenadas = (lat != null && lng != null)
                ? new Coordenadas(lat, lng)
                : null;

        DadosLocalizacao localizacao = DadosLocalizacao.builder()
                .endereco(dto.localizacao().endereco().trim())
                .coordenadas(coordenadas)
                .build();

        UsuarioAutenticado usuarioAutenticado = obterUsuarioAutenticadoOpcional();
        Long usuarioId = usuarioAutenticado != null ? usuarioAutenticado.id() : null;

        Ocorrencia ocorrencia = Ocorrencia.builder()
                .protocolo(gerarProtocoloUnico())
                .usuarioId(usuarioId)
                .comunicante(comunicante)
                .especieAnimal(dto.especieAnimal().trim())
                .descricaoAnimal(dto.descricaoAnimal().trim())
                .localizacao(localizacao)
                .status(StatusOcorrencia.EM_ABERTO)
                .imagens(dto.imagens() != null ? new java.util.ArrayList<>(dto.imagens()) : new java.util.ArrayList<>())
                .criadoEm(agora)
                .atualizadoEm(agora)
                .build();

        Ocorrencia salva = ocorrenciaRepository.save(ocorrencia);
        registrarHistorico(salva, StatusOcorrencia.EM_ABERTO, usuarioId, "Ocorrência registrada no sistema.", agora);

        OcorrenciaRegistradaEvent eventoRegistro = new OcorrenciaRegistradaEvent(
                salva.getId(),
                salva.getProtocolo(),
                salva.getComunicante().getNome(),
                salva.getComunicante().getEmail(),
                salva.getDescricaoAnimal(),
                salva.getLocalizacao().getEndereco(),
                salva.getStatus(),
                salva.getCriadoEm()
        );
        eventPublisher.publicarOcorrenciaRegistrada(eventoRegistro);
        emailPublisher.notificarRegistro(eventoRegistro);
        emailPublisher.notificarAnalistas(eventoRegistro);

        return OcorrenciaResponseDTO.fromEntity(salva);
    }

    @Transactional
    public OcorrenciaResponseDTO vincularAnalista(Long id) {
        UsuarioAutenticado usuario = obterUsuarioAutenticadoObrigatorio();
        String token = obterTokenAtual();

        Ocorrencia ocorrencia = buscarComBloqueio(id);

        if (ocorrencia.getStatus() != StatusOcorrencia.EM_ABERTO || ocorrencia.getAnalistaResponsavelId() != null) {
            throw new OcorrenciaJaAssumidaException("Esta ocorrência já foi assumida por outro analista ou não está mais disponível para atendimento.");
        }

        // Delegação de autorização ao State em usuarios-service (fail-closed)
        usuariosClient.validarPermissaoOuFalhar(
                usuario.id(),
                TipoAcaoOcorrencia.ASSUMIR,
                ocorrencia.getUsuarioId(),
                ocorrencia.getAnalistaResponsavelId(),
                token
        );

        LocalDateTime agora = LocalDateTime.now();
        ocorrencia.setAnalistaResponsavelId(usuario.id());
        ocorrencia.setStatus(StatusOcorrencia.EM_ATENDIMENTO);
        ocorrencia.setAtualizadoEm(agora);

        String observacao = "Atendimento assumido pelo analista ID " + usuario.id() + ".";
        registrarHistorico(ocorrencia, StatusOcorrencia.EM_ATENDIMENTO, usuario.id(), observacao, agora);

        StatusOcorrenciaAlteradoEvent eventoStatus = new StatusOcorrenciaAlteradoEvent(
                ocorrencia.getId(),
                ocorrencia.getProtocolo(),
                ocorrencia.getComunicante().getEmail(),
                StatusOcorrencia.EM_ABERTO,
                StatusOcorrencia.EM_ATENDIMENTO,
                usuario.id(),
                observacao,
                agora
        );
        eventPublisher.publicarStatusAlterado(eventoStatus);
        emailPublisher.notificarStatusAlterado(eventoStatus);

        return OcorrenciaResponseDTO.fromEntity(ocorrencia);
    }

    @Transactional
    public OcorrenciaResponseDTO atualizarStatus(Long id, AtualizarStatusDTO dto) {
        UsuarioAutenticado usuario = obterUsuarioAutenticadoObrigatorio();
        String token = obterTokenAtual();

        StatusOcorrencia novoStatus = StatusOcorrencia.deCodigo(dto.novoStatus());
        Ocorrencia ocorrencia = buscarComBloqueio(id);
        StatusOcorrencia statusAtual = ocorrencia.getStatus();

        if (!statusAtual.podeTransitarPara(novoStatus)) {
            throw new TransicaoStatusInvalidaException(
                    "Transição inválida de status: de '" + statusAtual + "' para '" + novoStatus + "'."
            );
        }

        if (statusAtual == StatusOcorrencia.EM_ABERTO) {
            throw new TransicaoStatusInvalidaException(
                    "Para assumir ou cancelar uma ocorrência em aberto, use os endpoints vincular-analista e cancelar."
            );
        }

        // Delegação de autorização ao State em usuarios-service (fail-closed)
        usuariosClient.validarPermissaoOuFalhar(
                usuario.id(),
                TipoAcaoOcorrencia.ATUALIZAR,
                ocorrencia.getUsuarioId(),
                ocorrencia.getAnalistaResponsavelId(),
                token
        );

        LocalDateTime agora = LocalDateTime.now();
        ocorrencia.setStatus(novoStatus);
        ocorrencia.setAtualizadoEm(agora);

        String observacao = (dto.observacao() != null && !dto.observacao().isBlank())
                ? dto.observacao().trim()
                : "Status alterado para " + novoStatus;
        registrarHistorico(ocorrencia, novoStatus, usuario.id(), observacao, agora);

        StatusOcorrenciaAlteradoEvent eventoStatus = new StatusOcorrenciaAlteradoEvent(
                ocorrencia.getId(),
                ocorrencia.getProtocolo(),
                ocorrencia.getComunicante().getEmail(),
                statusAtual,
                novoStatus,
                usuario.id(),
                observacao,
                agora
        );
        eventPublisher.publicarStatusAlterado(eventoStatus);
        emailPublisher.notificarStatusAlterado(eventoStatus);

        return OcorrenciaResponseDTO.fromEntity(ocorrencia);
    }

    @Transactional
    public OcorrenciaResponseDTO cancelarOcorrencia(Long id, CancelarOcorrenciaDTO dto) {
        UsuarioAutenticado usuario = obterUsuarioAutenticadoObrigatorio();
        String token = obterTokenAtual();

        Ocorrencia ocorrencia = buscarComBloqueio(id);

        if (ocorrencia.getStatus() != StatusOcorrencia.EM_ABERTO) {
            throw new TransicaoStatusInvalidaException(
                    "O cancelamento não pode ser realizado porque a ocorrência já está em atendimento ou finalizada."
            );
        }

        // Delegação de autorização ao State em usuarios-service (fail-closed)
        usuariosClient.validarPermissaoOuFalhar(
                usuario.id(),
                TipoAcaoOcorrencia.CANCELAR,
                ocorrencia.getUsuarioId(),
                ocorrencia.getAnalistaResponsavelId(),
                token
        );

        LocalDateTime agora = LocalDateTime.now();
        String justificativa = dto.justificativa().trim();
        ocorrencia.setStatus(StatusOcorrencia.CANCELADO);
        ocorrencia.setAtualizadoEm(agora);

        registrarHistorico(ocorrencia, StatusOcorrencia.CANCELADO, usuario.id(), "Cancelamento: " + justificativa, agora);

        OcorrenciaCanceladaEvent eventoCancelamento = new OcorrenciaCanceladaEvent(
                ocorrencia.getId(),
                ocorrencia.getProtocolo(),
                ocorrencia.getComunicante().getEmail(),
                usuario.id(),
                justificativa,
                agora
        );
        eventPublisher.publicarOcorrenciaCancelada(eventoCancelamento);
        emailPublisher.notificarCancelamento(eventoCancelamento);

        return OcorrenciaResponseDTO.fromEntity(ocorrencia);
    }

    @Transactional(readOnly = true)
    public Optional<OcorrenciaDetalhadaDTO> buscarDetalhesPorId(Long id) {
        UsuarioAutenticado usuario = obterUsuarioAutenticadoObrigatorio();
        Optional<Ocorrencia> optOcorrencia = ocorrenciaRepository.findById(id);

        if (optOcorrencia.isEmpty()) {
            return Optional.empty();
        }

        Ocorrencia ocorrencia = optOcorrencia.get();
        boolean ehCriador = ocorrencia.getUsuarioId() != null && ocorrencia.getUsuarioId().equals(usuario.id());
        boolean ehAnalistaResponsavel = ocorrencia.getAnalistaResponsavelId() != null
                && ocorrencia.getAnalistaResponsavelId().equals(usuario.id());
        boolean ehAdmin = usuario.ehAdministrador();

        if (!ehCriador && !ehAnalistaResponsavel && !ehAdmin) {
            throw new AcessoNegadoException("Acesso restrito ao criador, analista responsável ou administrador.");
        }

        return Optional.of(montarOcorrenciaDetalhada(ocorrencia));
    }

    @Transactional(readOnly = true)
    public Optional<OcorrenciaDetalhadaDTO> buscarDetalhesPorProtocolo(String protocolo) {
        return ocorrenciaRepository.findByProtocolo(protocolo).map(this::montarOcorrenciaDetalhada);
    }

    @Transactional(readOnly = true)
    public List<OcorrenciaResponseDTO> listarEmAberto() {
        return listar(StatusOcorrencia.EM_ABERTO);
    }

    @Transactional(readOnly = true)
    public List<OcorrenciaResponseDTO> listarPorUsuario(Long usuarioId) {
        UsuarioAutenticado usuario = obterUsuarioAutenticadoObrigatorio();
        if (!usuario.id().equals(usuarioId) && !usuario.ehAdministrador()) {
            throw new AcessoNegadoException("Você só pode consultar suas próprias ocorrências.");
        }
        return paraResposta(ocorrenciaRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId));
    }

    @Transactional(readOnly = true)
    public List<OcorrenciaResponseDTO> listar(StatusOcorrencia status) {
        return paraResposta(status != null
                ? ocorrenciaRepository.findByStatusOrderByCriadoEmDesc(status)
                : ocorrenciaRepository.findAllByOrderByCriadoEmDesc());
    }

    private UsuarioAutenticado obterUsuarioAutenticadoObrigatorio() {
        UsuarioAutenticado usuario = obterUsuarioAutenticadoOpcional();
        if (usuario == null) {
            throw new AcessoNegadoException("Autenticação necessária para realizar esta operação.");
        }
        return usuario;
    }

    private UsuarioAutenticado obterUsuarioAutenticadoOpcional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return usuario;
        }
        return null;
    }

    private String obterTokenAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof String token) {
            return token;
        }
        return "";
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

    private Ocorrencia buscarComBloqueio(Long id) {
        return ocorrenciaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new OcorrenciaNaoEncontradaException("Ocorrência não encontrada com o ID: " + id));
    }

    private void registrarHistorico(Ocorrencia ocorrencia, StatusOcorrencia status, Long usuarioId,
                                    String observacao, LocalDateTime dataHora) {
        historicoOcorrenciaRepository.save(HistoricoOcorrencia.builder()
                .ocorrencia(ocorrencia)
                .statusRegistrado(status)
                .usuarioId(usuarioId)
                .observacao(observacao)
                .dataHora(dataHora)
                .build());
    }

    private List<OcorrenciaResponseDTO> paraResposta(List<Ocorrencia> ocorrencias) {
        return ocorrencias.stream().map(OcorrenciaResponseDTO::fromEntity).toList();
    }

    private OcorrenciaDetalhadaDTO montarOcorrenciaDetalhada(Ocorrencia ocorrencia) {
        List<HistoricoOcorrenciaDTO> historico = historicoOcorrenciaRepository
                .findByOcorrenciaIdOrderByDataHoraAsc(ocorrencia.getId())
                .stream()
                .map(HistoricoOcorrenciaDTO::fromEntity)
                .toList();

        return OcorrenciaDetalhadaDTO.fromEntity(ocorrencia, historico);
    }
}
