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
import ufrpe.br.safer.ocorrencias.dto.VincularAnalistaDTO;
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

    @Test
    @DisplayName("POST /ocorrencias deve retornar 201 Created quando payload valido")
    void deveRegistrarOcorrenciaValida() throws Exception {
        RegistrarOcorrenciaDTO dto = new RegistrarOcorrenciaDTO(
                1L,
                new ComunicanteDTO("João Victor", "joao@ufrpe.br", "81999999999", "Aluno"),
                new LocalizacaoDTO(-8.0163, -34.9501, "Campus UFRPE"),
                "Animal ferido"
        );

        OcorrenciaResponseDTO response = new OcorrenciaResponseDTO(
                1L,
                "OC-20261111-ABC123",
                1L,
                dto.comunicante(),
                dto.localizacao(),
                dto.descricaoAnimal(),
                StatusOcorrencia.EM_ABERTO,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(ocorrenciasService.registrarOcorrencia(any())).thenReturn(response);

        mockMvc.perform(post("/ocorrencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/ocorrencias/1"))
                .andExpect(jsonPath("$.protocolo").value("OC-20261111-ABC123"))
                .andExpect(jsonPath("$.status").value("EM_ABERTO"));
    }

    @Test
    @DisplayName("POST /ocorrencias deve retornar 400 Bad Request quando campos obrigatorios ausentes")
    void deveRetornarBadRequestQuandoCamposInvalidos() throws Exception {
        RegistrarOcorrenciaDTO invalido = new RegistrarOcorrenciaDTO(
                null,
                new ComunicanteDTO("", "email_invalido", null, null),
                null,
                ""
        );

        mockMvc.perform(post("/ocorrencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /ocorrencias/{id}/vincular-analista deve retornar 200 OK quando assumido com sucesso")
    void deveVincularAnalistaComSucesso() throws Exception {
        VincularAnalistaDTO dto = new VincularAnalistaDTO(99L);
        OcorrenciaResponseDTO response = new OcorrenciaResponseDTO(
                1L,
                "OC-20261111-ABC123",
                1L,
                new ComunicanteDTO("João", "joao@ufrpe.br", null, null),
                new LocalizacaoDTO(-8.0163, -34.9501, "Campus"),
                "Animal ferido",
                StatusOcorrencia.EM_ATENDIMENTO,
                99L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(ocorrenciasService.vincularAnalista(1L, 99L)).thenReturn(response);

        mockMvc.perform(post("/ocorrencias/1/vincular-analista")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andExpect(jsonPath("$.analistaResponsavelId").value(99));
    }

    @Test
    @DisplayName("POST /ocorrencias/{id}/vincular-analista deve retornar 409 Conflict se houver race condition (RN03)")
    void deveRetornar409ConflictSeJaAssumida() throws Exception {
        VincularAnalistaDTO dto = new VincularAnalistaDTO(99L);

        when(ocorrenciasService.vincularAnalista(1L, 99L))
                .thenThrow(new OcorrenciaJaAssumidaException("Esta ocorrência já foi assumida por outro analista."));

        mockMvc.perform(post("/ocorrencias/1/vincular-analista")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito de atendimento (RN03)"));
    }

    @Test
    @DisplayName("PATCH /ocorrencias/{id}/status deve retornar 200 OK quando transicao valida")
    void deveAtualizarStatusComSucesso() throws Exception {
        AtualizarStatusDTO dto = new AtualizarStatusDTO(StatusOcorrencia.NECESSITA_ATENDIMENTO_MEDICO, 99L, "Obs");
        OcorrenciaResponseDTO response = new OcorrenciaResponseDTO(
                1L,
                "OC-20261111-ABC123",
                1L,
                new ComunicanteDTO("João", "joao@ufrpe.br", null, null),
                new LocalizacaoDTO(-8.0163, -34.9501, "Campus"),
                "Animal ferido",
                StatusOcorrencia.NECESSITA_ATENDIMENTO_MEDICO,
                99L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

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
        AtualizarStatusDTO dto = new AtualizarStatusDTO(StatusOcorrencia.FINALIZADO_COM_SUCESSO, 99L, "Obs");

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
        CancelarOcorrenciaDTO dto = new CancelarOcorrenciaDTO(1L, "Animal resgatado por terceiros.");
        OcorrenciaResponseDTO response = new OcorrenciaResponseDTO(
                1L,
                "OC-20261111-ABC123",
                1L,
                new ComunicanteDTO("João", "joao@ufrpe.br", null, null),
                new LocalizacaoDTO(-8.0163, -34.9501, "Campus"),
                "Animal ferido",
                StatusOcorrencia.CANCELADO,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

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
        CancelarOcorrenciaDTO dto = new CancelarOcorrenciaDTO(1L, "");

        mockMvc.perform(patch("/ocorrencias/1/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /ocorrencias/{id} deve retornar 200 OK com detalhes e historico")
    void deveBuscarPorIdComSucesso() throws Exception {
        OcorrenciaDetalhadaDTO detalhada = new OcorrenciaDetalhadaDTO(
                1L,
                "OC-20261111-0001",
                10L,
                new ComunicanteDTO("Maria", "maria@ufrpe.br", "81988888888", "Servidor"),
                new LocalizacaoDTO(-8.0163, -34.9501, "Reitoria"),
                "Pássaro ferido",
                StatusOcorrencia.EM_ABERTO,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                List.of(new HistoricoOcorrenciaDTO(1L, StatusOcorrencia.EM_ABERTO, 10L, "Criada", LocalDateTime.now()))
        );

        when(ocorrenciasService.buscarDetalhesPorId(1L)).thenReturn(Optional.of(detalhada));

        mockMvc.perform(get("/ocorrencias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolo").value("OC-20261111-0001"))
                .andExpect(jsonPath("$.historico[0].statusRegistrado").value("EM_ABERTO"));
    }

    @Test
    @DisplayName("GET /ocorrencias/{id} deve retornar 404 Not Found se inexistente")
    void deveRetornar404QuandoNaoEncontradaPorId() throws Exception {
        when(ocorrenciasService.buscarDetalhesPorId(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/ocorrencias/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /ocorrencias/protocolo/{protocolo} deve retornar 200 OK")
    void deveBuscarPorProtocoloComSucesso() throws Exception {
        OcorrenciaDetalhadaDTO detalhada = new OcorrenciaDetalhadaDTO(
                2L,
                "OC-20261111-PROT1",
                10L,
                new ComunicanteDTO("Maria", "maria@ufrpe.br", null, null),
                new LocalizacaoDTO(-8.0163, -34.9501, "Campus"),
                "Gato ferido",
                StatusOcorrencia.EM_ABERTO,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                List.of()
        );

        when(ocorrenciasService.buscarDetalhesPorProtocolo("OC-20261111-PROT1")).thenReturn(Optional.of(detalhada));

        mockMvc.perform(get("/ocorrencias/protocolo/OC-20261111-PROT1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolo").value("OC-20261111-PROT1"));
    }

    @Test
    @DisplayName("GET /ocorrencias/protocolo/{protocolo} deve retornar 404 Not Found se inexistente")
    void deveRetornar404QuandoProtocoloNaoExiste() throws Exception {
        when(ocorrenciasService.buscarDetalhesPorProtocolo("OC-INEXISTENTE")).thenReturn(Optional.empty());

        mockMvc.perform(get("/ocorrencias/protocolo/OC-INEXISTENTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /ocorrencias com filtro status deve retornar 200 OK")
    void deveListarComFiltroStatus() throws Exception {
        when(ocorrenciasService.listar(StatusOcorrencia.EM_ABERTO)).thenReturn(List.of());

        mockMvc.perform(get("/ocorrencias").param("status", "EM_ABERTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /ocorrencias/em-aberto deve retornar 200 OK com lista")
    void deveListarEmAbertoComSucesso() throws Exception {
        when(ocorrenciasService.listarEmAberto()).thenReturn(List.of());

        mockMvc.perform(get("/ocorrencias/em-aberto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /ocorrencias/usuario/{usuarioId} deve retornar 200 OK")
    void deveListarPorUsuarioComSucesso() throws Exception {
        when(ocorrenciasService.listarPorUsuario(10L)).thenReturn(List.of());

        mockMvc.perform(get("/ocorrencias/usuario/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
