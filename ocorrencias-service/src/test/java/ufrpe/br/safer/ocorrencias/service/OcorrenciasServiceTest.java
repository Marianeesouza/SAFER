package ufrpe.br.safer.ocorrencias.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ufrpe.br.safer.ocorrencias.client.GeocodificacaoClient;
import ufrpe.br.safer.ocorrencias.dto.AtualizarStatusDTO;
import ufrpe.br.safer.ocorrencias.dto.CancelarOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.dto.ComunicanteDTO;
import ufrpe.br.safer.ocorrencias.dto.LocalizacaoDTO;
import ufrpe.br.safer.ocorrencias.dto.OcorrenciaDetalhadaDTO;
import ufrpe.br.safer.ocorrencias.dto.OcorrenciaResponseDTO;
import ufrpe.br.safer.ocorrencias.dto.RegistrarOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.exception.OcorrenciaJaAssumidaException;
import ufrpe.br.safer.ocorrencias.exception.BusinessRuleException;
import ufrpe.br.safer.ocorrencias.exception.TransicaoStatusInvalidaException;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private OcorrenciaEventPublisher eventPublisher;

    @InjectMocks
    private OcorrenciasService ocorrenciasService;

    private static final String PROTOCOLO_1 = "OC-20261111-0001";
    private static final String PROTOCOLO_2 = "OC-20261111-0002";

    @Test
    @DisplayName("Deve registrar ocorrencia com sucesso dentro do campus")
    void deveRegistrarOcorrenciaComSucesso() {
        RegistrarOcorrenciaDTO dto = new RegistrarOcorrenciaDTO(
                1L,
                new ComunicanteDTO("João Victor", "joao@ufrpe.br", "81999999999", "Aluno"),
                new LocalizacaoDTO(-8.0163, -34.9501, "Rua Dom Manoel de Medeiros, s/n, Dois Irmãos"),
                "Sagui ferido próximo ao CEAGRI"
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
        assertThat(response.status()).isEqualTo(StatusOcorrencia.EM_ABERTO);
        assertThat(response.protocolo()).startsWith("OC-");
        assertThat(response.descricaoAnimal()).isEqualTo("Sagui ferido próximo ao CEAGRI");

        verify(historicoOcorrenciaRepository).save(any(HistoricoOcorrencia.class));
        verify(eventPublisher).publicarOcorrenciaRegistrada(any(OcorrenciaRegistradaEvent.class));
    }

    @Test
    @DisplayName("Deve rejeitar ocorrencia se as coordenadas forem parciais (apenas latitude ou apenas longitude)")
    void deveRejeitarCoordenadasParciais() {
        RegistrarOcorrenciaDTO apenasLat = new RegistrarOcorrenciaDTO(
                1L,
                new ComunicanteDTO("Ana", "ana@ufrpe.br", null, null),
                new LocalizacaoDTO(-8.0163, null, "Campus"),
                "Animal ferido"
        );

        RegistrarOcorrenciaDTO apenasLng = new RegistrarOcorrenciaDTO(
                1L,
                new ComunicanteDTO("Ana", "ana@ufrpe.br", null, null),
                new LocalizacaoDTO(null, -34.9501, "Campus"),
                "Animal ferido"
        );

        assertThatThrownBy(() -> ocorrenciasService.registrarOcorrencia(apenasLat))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Latitude e longitude devem ser fornecidas juntas ou ambas ausentes");

        assertThatThrownBy(() -> ocorrenciasService.registrarOcorrencia(apenasLng))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Latitude e longitude devem ser fornecidas juntas ou ambas ausentes");
    }

    @Test
    @DisplayName("Deve registrar ocorrencia em modo contingencia sem coordenadas (apenas endereco por extenso)")
    void deveRegistrarOcorrenciaModoContingenciaSemCoordenadas() {
        RegistrarOcorrenciaDTO dto = new RegistrarOcorrenciaDTO(
                2L,
                new ComunicanteDTO("Carlos Lima", "carlos@ufrpe.br", "81988887777", "Professor"),
                new LocalizacaoDTO(null, null, "Departamento de Zootecnia, UFRPE"),
                "Cavalo solto na pista"
        );

        when(ocorrenciaRepository.save(any(Ocorrencia.class))).thenAnswer(invocation -> {
            Ocorrencia o = invocation.getArgument(0);
            o.setId(20L);
            return o;
        });

        OcorrenciaResponseDTO response = ocorrenciasService.registrarOcorrencia(dto);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.localizacao().latitude()).isNull();
        assertThat(response.localizacao().longitude()).isNull();
        assertThat(response.localizacao().endereco()).isEqualTo("Departamento de Zootecnia, UFRPE");

        verify(historicoOcorrenciaRepository).save(any(HistoricoOcorrencia.class));
        verify(eventPublisher).publicarOcorrenciaRegistrada(any(OcorrenciaRegistradaEvent.class));
    }

    @Test
    @DisplayName("Deve vincular analista e transicionar para EM_ATENDIMENTO com sucesso")
    void deveVincularAnalistaComSucesso() {
        Ocorrencia o = criarOcorrenciaMock(1L, PROTOCOLO_1, StatusOcorrencia.EM_ABERTO, 5L);

        when(ocorrenciaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(o));

        OcorrenciaResponseDTO response = ocorrenciasService.vincularAnalista(1L, 99L);

        assertThat(response.status()).isEqualTo(StatusOcorrencia.EM_ATENDIMENTO);
        assertThat(response.analistaResponsavelId()).isEqualTo(99L);

        verify(historicoOcorrenciaRepository).save(any(HistoricoOcorrencia.class));
        verify(eventPublisher).publicarStatusAlterado(any(StatusOcorrenciaAlteradoEvent.class));
    }

    @Test
    @DisplayName("Deve barrar com OcorrenciaJaAssumidaException se outro analista ja assumiu (a leitura com bloqueio serializa as tentativas)")
    void deveBarrarVinculoDeOcorrenciaJaAssumida() {
        Ocorrencia o = criarOcorrenciaMock(1L, PROTOCOLO_1, StatusOcorrencia.EM_ATENDIMENTO, 5L);
        o.setAnalistaResponsavelId(77L);

        when(ocorrenciaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> ocorrenciasService.vincularAnalista(1L, 99L))
                .isInstanceOf(OcorrenciaJaAssumidaException.class)
                .hasMessageContaining("já foi assumida por outro analista");
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Deve atualizar status com transição válida segundo a máquina de estados (RN03)")
    void deveAtualizarStatusValido() {
        Ocorrencia o = criarOcorrenciaMock(1L, PROTOCOLO_1, StatusOcorrencia.EM_ATENDIMENTO, 5L);
        o.setAnalistaResponsavelId(99L);

        when(ocorrenciaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(o));

        AtualizarStatusDTO dto = new AtualizarStatusDTO(
                StatusOcorrencia.NECESSITA_ATENDIMENTO_MEDICO.getCodigo(),
                99L,
                "Animal com fratura exposta, encaminhado ao Hospital Veterinário"
        );

        OcorrenciaResponseDTO response = ocorrenciasService.atualizarStatus(1L, dto);

        assertThat(response.status()).isEqualTo(StatusOcorrencia.NECESSITA_ATENDIMENTO_MEDICO);
        verify(historicoOcorrenciaRepository).save(any(HistoricoOcorrencia.class));
        verify(eventPublisher).publicarStatusAlterado(any(StatusOcorrenciaAlteradoEvent.class));
    }

    @Test
    @DisplayName("Deve lançar TransicaoStatusInvalidaException se transição for proibida pela RN03")
    void deveRejeitarTransicaoInvalida() {
        Ocorrencia o = criarOcorrenciaMock(1L, PROTOCOLO_1, StatusOcorrencia.EM_ABERTO, 5L);

        when(ocorrenciaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(o));

        AtualizarStatusDTO dto = new AtualizarStatusDTO(
                StatusOcorrencia.FINALIZADO_COM_SUCESSO.getCodigo(),
                99L,
                "Tentando finalizar sem antes atender"
        );

        assertThatThrownBy(() -> ocorrenciasService.atualizarStatus(1L, dto))
                .isInstanceOf(TransicaoStatusInvalidaException.class)
                .hasMessageContaining("Transição inválida de status");
    }

    @Test
    @DisplayName("Deve rejeitar codigo de status desconhecido antes de consultar o banco")
    void deveRejeitarCodigoDeStatusDesconhecido() {
        assertThatThrownBy(() -> ocorrenciasService.atualizarStatus(1L, new AtualizarStatusDTO(99, 5L, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Status inválido: 99");
        verifyNoInteractions(ocorrenciaRepository, historicoOcorrenciaRepository, eventPublisher);
    }

    @Test
    @DisplayName("Deve barrar assumir ou cancelar pelo endpoint de status (sem analista responsavel nem justificativa)")
    void deveBarrarAssumirOuCancelarPeloEndpointDeStatus() {
        for (StatusOcorrencia destino : List.of(StatusOcorrencia.EM_ATENDIMENTO, StatusOcorrencia.CANCELADO)) {
            Ocorrencia o = criarOcorrenciaMock(1L, PROTOCOLO_1, StatusOcorrencia.EM_ABERTO, 5L);
            when(ocorrenciaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> ocorrenciasService.atualizarStatus(1L, new AtualizarStatusDTO(destino.getCodigo(), 99L, null)))
                    .isInstanceOf(TransicaoStatusInvalidaException.class)
                    .hasMessageContaining("vincular-analista");
            assertThat(o.getStatus()).isEqualTo(StatusOcorrencia.EM_ABERTO);
        }
        verifyNoInteractions(historicoOcorrenciaRepository, eventPublisher);
    }

    @Test
    @DisplayName("Deve cancelar ocorrencia com status EM_ABERTO e gravar justificativa (CDU08)")
    void deveCancelarOcorrenciaComSucesso() {
        Ocorrencia o = criarOcorrenciaMock(1L, PROTOCOLO_1, StatusOcorrencia.EM_ABERTO, 5L);
        CancelarOcorrenciaDTO dto = new CancelarOcorrenciaDTO(5L, "Animal fugiu do local antes da equipe chegar.");

        when(ocorrenciaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(o));

        OcorrenciaResponseDTO response = ocorrenciasService.cancelarOcorrencia(1L, dto);

        assertThat(response.status()).isEqualTo(StatusOcorrencia.CANCELADO);
        verify(historicoOcorrenciaRepository).save(any(HistoricoOcorrencia.class));
        verify(eventPublisher).publicarOcorrenciaCancelada(any(OcorrenciaCanceladaEvent.class));
    }

    @Test
    @DisplayName("Deve barrar cancelamento se ocorrencia já estiver em atendimento (RN03)")
    void deveBarrarCancelamentoSeJaEmAtendimento() {
        Ocorrencia o = criarOcorrenciaMock(1L, PROTOCOLO_1, StatusOcorrencia.EM_ATENDIMENTO, 5L);
        CancelarOcorrenciaDTO dto = new CancelarOcorrenciaDTO(5L, "Tentando cancelar chamada em atendimento.");

        when(ocorrenciaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> ocorrenciasService.cancelarOcorrencia(1L, dto))
                .isInstanceOf(TransicaoStatusInvalidaException.class)
                .hasMessageContaining("já está em atendimento ou finalizada");
    }

    @Test
    @DisplayName("Deve rejeitar ocorrencia se as coordenadas estiverem fora do campus")
    void deveRejeitarOcorrenciaForaDoCampus() {
        RegistrarOcorrenciaDTO dto = new RegistrarOcorrenciaDTO(
                null,
                new ComunicanteDTO("Maria Silva", "maria@gmail.com", null, null),
                new LocalizacaoDTO(-8.0500, -34.9000, "Fora da UFRPE"),
                "Iguana avistada"
        );

        when(geocodificacaoClient.validarPontoNoCampus(-8.0500, -34.9000)).thenReturn(false);

        assertThatThrownBy(() -> ocorrenciasService.registrarOcorrencia(dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("fora do perímetro do campus da UFRPE");
    }

    @Test
    @DisplayName("Deve buscar detalhes da ocorrencia por ID com historico ordenado")
    void deveBuscarDetalhesPorIdComHistorico() {
        LocalDateTime agora = LocalDateTime.now();
        Ocorrencia o = criarOcorrenciaMock(1L, PROTOCOLO_1, StatusOcorrencia.EM_ABERTO, 5L);

        HistoricoOcorrencia h = HistoricoOcorrencia.builder()
                .id(100L)
                .ocorrencia(o)
                .statusRegistrado(StatusOcorrencia.EM_ABERTO)
                .usuarioId(5L)
                .observacao("Ocorrência registrada")
                .dataHora(agora)
                .build();

        when(ocorrenciaRepository.findById(1L)).thenReturn(Optional.of(o));
        when(historicoOcorrenciaRepository.findByOcorrenciaIdOrderByDataHoraAsc(1L)).thenReturn(List.of(h));

        Optional<OcorrenciaDetalhadaDTO> opt = ocorrenciasService.buscarDetalhesPorId(1L);

        assertThat(opt).isPresent();
        OcorrenciaDetalhadaDTO dto = opt.get();
        assertThat(dto.protocolo()).isEqualTo(PROTOCOLO_1);
        assertThat(dto.historico()).hasSize(1);
        assertThat(dto.historico().get(0).statusRegistrado()).isEqualTo(StatusOcorrencia.EM_ABERTO);
    }

    @Test
    @DisplayName("Deve buscar detalhes da ocorrencia por protocolo")
    void deveBuscarDetalhesPorProtocolo() {
        Ocorrencia o = criarOcorrenciaMock(2L, PROTOCOLO_2, StatusOcorrencia.EM_ABERTO, 5L);

        when(ocorrenciaRepository.findByProtocolo(PROTOCOLO_2)).thenReturn(Optional.of(o));
        when(historicoOcorrenciaRepository.findByOcorrenciaIdOrderByDataHoraAsc(2L)).thenReturn(List.of());

        Optional<OcorrenciaDetalhadaDTO> opt = ocorrenciasService.buscarDetalhesPorProtocolo(PROTOCOLO_2);

        assertThat(opt).isPresent();
        assertThat(opt.get().id()).isEqualTo(2L);
        assertThat(opt.get().historico()).isEmpty();
    }

    @Test
    @DisplayName("Deve listar ocorrencias em aberto")
    void deveListarEmAberto() {
        Ocorrencia o1 = criarOcorrenciaMock(1L, PROTOCOLO_1, StatusOcorrencia.EM_ABERTO, 5L);
        Ocorrencia o2 = criarOcorrenciaMock(2L, PROTOCOLO_2, StatusOcorrencia.EM_ABERTO, 6L);

        when(ocorrenciaRepository.findByStatusOrderByCriadoEmDesc(StatusOcorrencia.EM_ABERTO))
                .thenReturn(List.of(o1, o2));

        List<OcorrenciaResponseDTO> lista = ocorrenciasService.listarEmAberto();

        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).status()).isEqualTo(StatusOcorrencia.EM_ABERTO);
    }

    @Test
    @DisplayName("Deve listar ocorrencias de um usuario")
    void deveListarPorUsuario() {
        Ocorrencia o = criarOcorrenciaMock(1L, PROTOCOLO_1, StatusOcorrencia.EM_ABERTO, 5L);

        when(ocorrenciaRepository.findByUsuarioIdOrderByCriadoEmDesc(5L)).thenReturn(List.of(o));

        List<OcorrenciaResponseDTO> lista = ocorrenciasService.listarPorUsuario(5L);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).usuarioId()).isEqualTo(5L);
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
