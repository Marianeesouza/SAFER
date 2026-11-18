package ufrpe.br.safer.ocorrencias.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ufrpe.br.safer.ocorrencias.dto.AtualizarStatusDTO;
import ufrpe.br.safer.ocorrencias.dto.CancelarOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.dto.ComunicanteDTO;
import ufrpe.br.safer.ocorrencias.dto.HistoricoOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.dto.LocalizacaoDTO;
import ufrpe.br.safer.ocorrencias.dto.OcorrenciaDetalhadaDTO;
import ufrpe.br.safer.ocorrencias.dto.OcorrenciaResponseDTO;
import ufrpe.br.safer.ocorrencias.dto.RegistrarOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.exception.OcorrenciaJaAssumidaException;
import ufrpe.br.safer.ocorrencias.exception.OcorrenciaNaoEncontradaException;
import ufrpe.br.safer.ocorrencias.exception.TransicaoStatusInvalidaException;
import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;
import ufrpe.br.safer.ocorrencias.service.OcorrenciasService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OcorrenciasControllerTest {

    private static final String PROTOCOLO_A = "OC-20261111-ABC123";
    private static final String PROTOCOLO_B = "OC-20261111-PROT1";
    private static final String PROTOCOLO_C = "OC-INEXISTENTE";

    private final OcorrenciasService ocorrenciasService = mock(OcorrenciasService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        OcorrenciasController controller = new OcorrenciasController(ocorrenciasService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    private OcorrenciaResponseDTO criarResponse(Long id, String protocolo, StatusOcorrencia status, Long analistaId) {
        return new OcorrenciaResponseDTO(
                id,
                protocolo,
                1L,
                new ComunicanteDTO("João", "joao@ufrpe.br", "81999999999", "Aluno"),
                new LocalizacaoDTO(-8.0163, -34.9501, "Campus"),
                "Capivara",
                "Animal ferido",
                status,
                analistaId,
                LocalDateTime.now(),
                LocalDateTime.now(),
                List.of()
        );
    }

    private OcorrenciaDetalhadaDTO criarDetalhada(Long id, String protocolo, List<HistoricoOcorrenciaDTO> historico) {
        return new OcorrenciaDetalhadaDTO(
                id,
                protocolo,
                1L,
                new ComunicanteDTO("Maria", "maria@ufrpe.br", "81988888888", "Servidora"),
                new LocalizacaoDTO(-8.0163, -34.9501, "Rua Central"),
                "Gato",
                "Gato preso",
                StatusOcorrencia.EM_ABERTO,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                historico,
                List.of()
        );
    }

    @Test
    @DisplayName("POST /ocorrencias deve retornar 201 Created quando payload valido")
    void deveRegistrarOcorrenciaValida() throws Exception {
        RegistrarOcorrenciaDTO dto = new RegistrarOcorrenciaDTO(
                new ComunicanteDTO("João Victor", "joao@ufrpe.br", "81999999999", "Aluno"),
                new LocalizacaoDTO(-8.0163, -34.9501, "Campus UFRPE"),
                "Capivara",
                "Animal ferido",
                List.of()
        );

        OcorrenciaResponseDTO response = criarResponse(1L, PROTOCOLO_A, StatusOcorrencia.EM_ABERTO, null);
        when(ocorrenciasService.registrarOcorrencia(any())).thenReturn(response);

        mockMvc.perform(post("/ocorrencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/ocorrencias/1"))
                .andExpect(jsonPath("$.protocolo").value(PROTOCOLO_A))
                .andExpect(jsonPath("$.status").value("EM_ABERTO"));
    }

    @Test
    @DisplayName("POST /ocorrencias deve retornar 400 Bad Request quando campos obrigatorios ausentes")
    void deveRetornarBadRequestQuandoCamposInvalidos() throws Exception {
        RegistrarOcorrenciaDTO invalido = new RegistrarOcorrenciaDTO(
                new ComunicanteDTO("", "email_invalido", null, null),
                null,
                "",
                "",
                List.of()
        );

        mockMvc.perform(post("/ocorrencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /ocorrencias/{id}/vincular-analista deve retornar 200 OK quando assumido com sucesso")
    void deveVincularAnalistaComSucesso() throws Exception {
        OcorrenciaResponseDTO response = criarResponse(1L, PROTOCOLO_A, StatusOcorrencia.EM_ATENDIMENTO, 99L);
        when(ocorrenciasService.vincularAnalista(1L)).thenReturn(response);

        mockMvc.perform(post("/ocorrencias/1/vincular-analista"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andExpect(jsonPath("$.analistaResponsavelId").value(99));
    }

    @Test
    @DisplayName("POST /ocorrencias/{id}/vincular-analista deve retornar 409 Conflict se houver race condition (RN03)")
    void deveRetornar409ConflictSeJaAssumida() throws Exception {
        when(ocorrenciasService.vincularAnalista(1L))
                .thenThrow(new OcorrenciaJaAssumidaException("Esta ocorrência já foi assumida por outro analista."));

        mockMvc.perform(post("/ocorrencias/1/vincular-analista"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito de atendimento (RN03)"));
    }

    @Test
    @DisplayName("PATCH /ocorrencias/{id}/status deve retornar 200 OK quando transicao valida")
    void deveAtualizarStatusComSucesso() throws Exception {
        AtualizarStatusDTO dto = new AtualizarStatusDTO(StatusOcorrencia.NECESSITA_ATENDIMENTO_MEDICO.getCodigo(), "Obs");
        OcorrenciaResponseDTO response = criarResponse(1L, PROTOCOLO_A, StatusOcorrencia.NECESSITA_ATENDIMENTO_MEDICO, 99L);

        when(ocorrenciasService.atualizarStatus(eq(1L), any())).thenReturn(response);

        mockMvc.perform(patch("/ocorrencias/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NECESSITA_ATENDIMENTO_MEDICO"));
    }

    @Test
    @DisplayName("PATCH /ocorrencias/{id}/status deve retornar 422 Unprocessable se transicao invalida (RN03)")
    void deveRetornar422SeTransicaoInvalida() throws Exception {
        AtualizarStatusDTO dto = new AtualizarStatusDTO(StatusOcorrencia.FINALIZADO_COM_SUCESSO.getCodigo(), "Obs");

        when(ocorrenciasService.atualizarStatus(eq(1L), any()))
                .thenThrow(new TransicaoStatusInvalidaException("Transição inválida de status."));

        mockMvc.perform(patch("/ocorrencias/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Transição de status inválida (RN03)"));
    }

    @Test
    @DisplayName("PATCH /ocorrencias/{id}/cancelar deve retornar 200 OK quando cancelado com sucesso")
    void deveCancelarOcorrenciaComSucesso() throws Exception {
        CancelarOcorrenciaDTO dto = new CancelarOcorrenciaDTO("Animal resgatado por terceiros.");
        OcorrenciaResponseDTO response = criarResponse(1L, PROTOCOLO_A, StatusOcorrencia.CANCELADO, null);

        when(ocorrenciasService.cancelarOcorrencia(eq(1L), any())).thenReturn(response);

        mockMvc.perform(patch("/ocorrencias/1/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    @Test
    @DisplayName("PATCH /ocorrencias/{id}/cancelar deve retornar 400 Bad Request se justificativa vazia")
    void deveRetornarBadRequestSeJustificativaVazia() throws Exception {
        CancelarOcorrenciaDTO dto = new CancelarOcorrenciaDTO("");

        mockMvc.perform(patch("/ocorrencias/1/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /ocorrencias/{id} deve retornar 200 OK com detalhes e historico")
    void deveBuscarPorIdComSucesso() throws Exception {
        OcorrenciaDetalhadaDTO detalhada = criarDetalhada(1L, PROTOCOLO_A,
                List.of(new HistoricoOcorrenciaDTO(1L, StatusOcorrencia.EM_ABERTO, 1L, "Criada", LocalDateTime.now()))
        );

        when(ocorrenciasService.buscarDetalhesPorId(1L)).thenReturn(Optional.of(detalhada));

        mockMvc.perform(get("/ocorrencias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.historico[0].observacao").value("Criada"));
    }

    @Test
    @DisplayName("GET /ocorrencias/{id} deve retornar 404 Not Found quando id nao existe")
    void deveRetornarNotFoundQuandoIdNaoExiste() throws Exception {
        when(ocorrenciasService.buscarDetalhesPorId(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/ocorrencias/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ocorrência não encontrada"));
    }

    @Test
    @DisplayName("GET /ocorrencias/protocolo/{protocolo} deve retornar 200 OK")
    void deveBuscarPorProtocoloComSucesso() throws Exception {
        OcorrenciaDetalhadaDTO detalhada = criarDetalhada(1L, PROTOCOLO_A, List.of());

        when(ocorrenciasService.buscarDetalhesPorProtocolo(PROTOCOLO_A)).thenReturn(Optional.of(detalhada));

        mockMvc.perform(get("/ocorrencias/protocolo/" + PROTOCOLO_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolo").value(PROTOCOLO_A));
    }

    @Test
    @DisplayName("GET /ocorrencias/protocolo/{protocolo} deve retornar 404 quando protocolo nao existe")
    void deveRetornarNotFoundQuandoProtocoloNaoExiste() throws Exception {
        when(ocorrenciasService.buscarDetalhesPorProtocolo(PROTOCOLO_C)).thenReturn(Optional.empty());

        mockMvc.perform(get("/ocorrencias/protocolo/" + PROTOCOLO_C))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ocorrência não encontrada"));
    }

    @Test
    @DisplayName("GET /ocorrencias/em-aberto deve retornar lista filtrada")
    void deveListarOcorrenciasEmAberto() throws Exception {
        OcorrenciaResponseDTO item = criarResponse(1L, PROTOCOLO_B, StatusOcorrencia.EM_ABERTO, null);

        when(ocorrenciasService.listarEmAberto()).thenReturn(List.of(item));

        mockMvc.perform(get("/ocorrencias/em-aberto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].protocolo").value(PROTOCOLO_B))
                .andExpect(jsonPath("$[0].status").value("EM_ABERTO"));
    }

    @Test
    @DisplayName("GET /ocorrencias/usuario/{id} deve retornar ocorrencias do usuario")
    void deveListarOcorrenciasDoUsuario() throws Exception {
        when(ocorrenciasService.listarPorUsuario(1L)).thenReturn(List.of());

        mockMvc.perform(get("/ocorrencias/usuario/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /ocorrencias deve listar todas com ou sem filtro de status")
    void deveListarTodasComFiltroStatus() throws Exception {
        when(ocorrenciasService.listar(StatusOcorrencia.EM_ATENDIMENTO)).thenReturn(List.of());

        mockMvc.perform(get("/ocorrencias").param("status", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
