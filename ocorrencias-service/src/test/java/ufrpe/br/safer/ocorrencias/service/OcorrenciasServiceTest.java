package ufrpe.br.safer.ocorrencias.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import ufrpe.br.safer.ocorrencias.client.GeocodificacaoClient;
import ufrpe.br.safer.ocorrencias.client.UsuariosClient;
import ufrpe.br.safer.ocorrencias.client.dto.TipoAcaoOcorrencia;
import ufrpe.br.safer.ocorrencias.dto.AtualizarStatusDTO;
import ufrpe.br.safer.ocorrencias.dto.CancelarOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.dto.ComunicanteDTO;
import ufrpe.br.safer.ocorrencias.dto.HistoricoOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.dto.LocalizacaoDTO;
import ufrpe.br.safer.ocorrencias.dto.OcorrenciaDetalhadaDTO;
import ufrpe.br.safer.ocorrencias.dto.OcorrenciaResponseDTO;
import ufrpe.br.safer.ocorrencias.dto.RegistrarOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.exception.AcessoNegadoException;
import ufrpe.br.safer.ocorrencias.exception.BusinessRuleException;
import ufrpe.br.safer.ocorrencias.exception.OcorrenciaJaAssumidaException;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OcorrenciasServiceTest {

    @Mock
    private OcorrenciaRepository ocorrenciaRepository;

    @Mock
    private HistoricoOcorrenciaRepository historicoOcorrenciaRepository;

    @Mock
    private GeocodificacaoClient geocodificacaoClient;

    @Mock
    private UsuariosClient usuariosClient;

    @Mock
    private OcorrenciaEventPublisher eventPublisher;

    @Mock
    private EmailNotificacaoPublisher emailPublisher;

    @InjectMocks
    private OcorrenciasService ocorrenciasService;

    private static final String PROTOCOLO_1 = "OC-20261111-0001";
    private static final String PROTOCOLO_2 = "OC-20261111-0002";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockAutenticado(Long id, String cargo) {
        UsuarioAutenticado user = new UsuarioAutenticado(id, "user@ufrpe.br", "User", cargo);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, "dummy-token", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Deve registrar ocorrencia com usuario logado com sucesso")
    void deveRegistrarOcorrenciaComUsuarioLogado() {
        mockAutenticado(1L, "COMUM");

        RegistrarOcorrenciaDTO dto = new RegistrarOcorrenciaDTO(
                new ComunicanteDTO("João Victor", "joao@ufrpe.br", "81999999999", "Aluno"),
                new LocalizacaoDTO(-8.0163, -34.9501, "Rua Dom Manoel de Medeiros, s/n, Dois Irmãos"),
                "Sagui",
                "Sagui ferido próximo ao CEAGRI",
                List.of()
        );

        when(geocodificacaoClient.validarPontoNoCampus(-8.0163, -34.9501)).thenReturn(true);
        when(ocorrenciaRepository.save(any(Ocorrencia.class))).thenAnswer(invocation -> {
            Ocorrencia o = invocation.getArgument(0);
            o.setId(10L);
            return o;
        });

        OcorrenciaResponseDTO response = ocorrenciasService.registrarOcorrencia(dto);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.usuarioId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(StatusOcorrencia.EM_ABERTO);

        verify(historicoOcorrenciaRepository).save(any(HistoricoOcorrencia.class));
        verify(eventPublisher).publicarOcorrenciaRegistrada(any(OcorrenciaRegistradaEvent.class));
        verify(emailPublisher).notificarRegistro(any(OcorrenciaRegistradaEvent.class));
    }

    @Test
    @DisplayName("Deve registrar ocorrencia como visitante (anonimo sem token: usuarioId nulo)")
    void deveRegistrarOcorrenciaAnonimaComSucesso() {
        SecurityContextHolder.clearContext();

        RegistrarOcorrenciaDTO dto = new RegistrarOcorrenciaDTO(
                new ComunicanteDTO("Visitante", "vis@externo.com", null, null),
                new LocalizacaoDTO(null, null, "Campus UFRPE"),
                "Iguana",
                "Iguana no gramado",
                List.of()
        );

        when(ocorrenciaRepository.save(any(Ocorrencia.class))).thenAnswer(invocation -> {
            Ocorrencia o = invocation.getArgument(0);
            o.setId(20L);
            return o;
        });

        OcorrenciaResponseDTO response = ocorrenciasService.registrarOcorrencia(dto);

        assertThat(response).isNotNull();
        assertThat(response.usuarioId()).isNull();
        verify(historicoOcorrenciaRepository).save(any(HistoricoOcorrencia.class));
    }

    @Test
    @DisplayName("Deve vincular analista com sucesso delegando autorizacao ao State via UsuariosClient")
    void deveVincularAnalistaComSucesso() {
        mockAutenticado(99L, "ANALISTA");

        Ocorrencia o = criarOcorrenciaMock(1L, PROTOCOLO_1, StatusOcorrencia.EM_ABERTO, 5L);
        when(ocorrenciaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(o));
        doNothing().when(usuariosClient).validarPermissaoOuFalhar(eq(99L), eq(TipoAcaoOcorrencia.ASSUMIR), eq(5L), eq(null), eq("dummy-token"));

        OcorrenciaResponseDTO response = ocorrenciasService.vincularAnalista(1L);

        assertThat(response.status()).isEqualTo(StatusOcorrencia.EM_ATENDIMENTO);
        assertThat(response.analistaResponsavelId()).isEqualTo(99L);
        verify(historicoOcorrenciaRepository).save(any(HistoricoOcorrencia.class));
    }

    @Test
    @DisplayName("Deve barrar com OcorrenciaJaAssumidaException se outro analista ja assumiu")
    void deveBarrarVinculoDeOcorrenciaJaAssumida() {
        mockAutenticado(99L, "ANALISTA");

        Ocorrencia o = criarOcorrenciaMock(1L, PROTOCOLO_1, StatusOcorrencia.EM_ATENDIMENTO, 5L);
        o.setAnalistaResponsavelId(77L);

        when(ocorrenciaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> ocorrenciasService.vincularAnalista(1L))
                .isInstanceOf(OcorrenciaJaAssumidaException.class)
                .hasMessageContaining("já foi assumida por outro analista");
        verifyNoInteractions(eventPublisher, emailPublisher, usuariosClient);
    }

    @Test
    @DisplayName("Deve atualizar status com transição válida delegando ao UsuariosClient")
    void deveAtualizarStatusValido() {
        mockAutenticado(99L, "ANALISTA");

        Ocorrencia o = criarOcorrenciaMock(1L, PROTOCOLO_1, StatusOcorrencia.EM_ATENDIMENTO, 5L);
        o.setAnalistaResponsavelId(99L);

        when(ocorrenciaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(o));
        doNothing().when(usuariosClient).validarPermissaoOuFalhar(eq(99L), eq(TipoAcaoOcorrencia.ATUALIZAR), eq(5L), eq(99L), eq("dummy-token"));

        AtualizarStatusDTO dto = new AtualizarStatusDTO(
                StatusOcorrencia.NECESSITA_ATENDIMENTO_MEDICO.getCodigo(),
                "Encaminhado ao HV"
        );

        OcorrenciaResponseDTO response = ocorrenciasService.atualizarStatus(1L, dto);

        assertThat(response.status()).isEqualTo(StatusOcorrencia.NECESSITA_ATENDIMENTO_MEDICO);
        verify(historicoOcorrenciaRepository).save(any(HistoricoOcorrencia.class));
    }

    @Test
    @DisplayName("Deve cancelar ocorrencia com status EM_ABERTO e gravar justificativa")
    void deveCancelarOcorrenciaComSucesso() {
        mockAutenticado(5L, "COMUM");

        Ocorrencia o = criarOcorrenciaMock(1L, PROTOCOLO_1, StatusOcorrencia.EM_ABERTO, 5L);
        CancelarOcorrenciaDTO dto = new CancelarOcorrenciaDTO("Animal fugiu.");

        when(ocorrenciaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(o));
        doNothing().when(usuariosClient).validarPermissaoOuFalhar(eq(5L), eq(TipoAcaoOcorrencia.CANCELAR), eq(5L), eq(null), eq("dummy-token"));

        OcorrenciaResponseDTO response = ocorrenciasService.cancelarOcorrencia(1L, dto);

        assertThat(response.status()).isEqualTo(StatusOcorrencia.CANCELADO);
        verify(historicoOcorrenciaRepository).save(any(HistoricoOcorrencia.class));
    }

    @Test
    @DisplayName("Deve barrar busca por ID se usuario nao for o criador, analista responsavel ou admin")
    void deveBarrarBuscaPorIdSeNaoAutorizado() {
        mockAutenticado(88L, "COMUM");

        Ocorrencia o = criarOcorrenciaMock(1L, PROTOCOLO_1, StatusOcorrencia.EM_ABERTO, 5L);
        o.setAnalistaResponsavelId(99L);
        when(ocorrenciaRepository.findById(1L)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> ocorrenciasService.buscarDetalhesPorId(1L))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessageContaining("Acesso restrito");
    }

    @Test
    @DisplayName("Deve permitir busca por ID se usuario for o criador")
    void devePermitirBuscaPorIdSeCriador() {
        mockAutenticado(5L, "COMUM");

        Ocorrencia o = criarOcorrenciaMock(1L, PROTOCOLO_1, StatusOcorrencia.EM_ABERTO, 5L);
        when(ocorrenciaRepository.findById(1L)).thenReturn(Optional.of(o));
        when(historicoOcorrenciaRepository.findByOcorrenciaIdOrderByDataHoraAsc(1L)).thenReturn(List.of());

        Optional<OcorrenciaDetalhadaDTO> opt = ocorrenciasService.buscarDetalhesPorId(1L);

        assertThat(opt).isPresent();
        assertThat(opt.get().protocolo()).isEqualTo(PROTOCOLO_1);
    }

    @Test
    @DisplayName("Deve barrar listarPorUsuario se tentar consultar ocorrencias de outro usuario")
    void deveBarrarListagemDeOutroUsuario() {
        mockAutenticado(10L, "COMUM");

        assertThatThrownBy(() -> ocorrenciasService.listarPorUsuario(20L))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessageContaining("Você só pode consultar suas próprias ocorrências");
    }

    private Ocorrencia criarOcorrenciaMock(Long id, String protocolo, StatusOcorrencia status, Long usuarioId) {
        return Ocorrencia.builder()
                .id(id)
                .protocolo(protocolo)
                .usuarioId(usuarioId)
                .comunicante(DadosComunicante.builder()
                        .nome("Fulano")
                        .email("fulano@ufrpe.br")
                        .telefone("81999999999")
                        .relacaoUfrpe("Aluno")
                        .build())
                .descricaoAnimal("Gato ferido")
                .especieAnimal("Gato")
                .localizacao(DadosLocalizacao.builder()
                        .endereco("Campus")
                        .coordenadas(new Coordenadas(-8.0163, -34.9501))
                        .build())
                .status(status)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
    }
}
