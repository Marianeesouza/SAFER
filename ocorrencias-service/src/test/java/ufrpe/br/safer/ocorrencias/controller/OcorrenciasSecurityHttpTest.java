package ufrpe.br.safer.ocorrencias.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ufrpe.br.safer.ocorrencias.dto.CancelarOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.dto.ComunicanteDTO;
import ufrpe.br.safer.ocorrencias.dto.LocalizacaoDTO;
import ufrpe.br.safer.ocorrencias.dto.OcorrenciaDetalhadaDTO;
import ufrpe.br.safer.ocorrencias.dto.OcorrenciaResponseDTO;
import ufrpe.br.safer.ocorrencias.dto.RegistrarOcorrenciaDTO;
import ufrpe.br.safer.ocorrencias.exception.AcessoNegadoException;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OcorrenciasSecurityHttpTest {

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
    @DisplayName("POST /ocorrencias deve registrar ocorrência anonima sem token (CDU05)")
    void devePermitirRegistroAnonimoSemToken() throws Exception {
        RegistrarOcorrenciaDTO dto = new RegistrarOcorrenciaDTO(
                new ComunicanteDTO("Anonimo", "anonimo@gmail.com", "81999999999", "Visitante"),
                new LocalizacaoDTO(null, null, "Campus UFRPE"),
                "Teju",
                "Teju visto na reitoria",
                List.of()
        );

        OcorrenciaResponseDTO response = new OcorrenciaResponseDTO(
                1L, "OC-20261118-001", null, dto.comunicante(), dto.localizacao(),
                "Teju", dto.descricaoAnimal(), StatusOcorrencia.EM_ABERTO, null,
                LocalDateTime.now(), LocalDateTime.now(), List.of()
        );

        when(ocorrenciasService.registrarOcorrencia(any())).thenReturn(response);

        mockMvc.perform(post("/ocorrencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protocolo").value("OC-20261118-001"));
    }

    @Test
    @DisplayName("GET /ocorrencias/protocolo/{protocolo} deve permitir consulta publica (CDU06)")
    void devePermitirConsultaPorProtocolo() throws Exception {
        OcorrenciaDetalhadaDTO detalhada = new OcorrenciaDetalhadaDTO(
                1L, "OC-PUBLICO-1", null,
                new ComunicanteDTO("A", "a@ufrpe.br", null, null),
                new LocalizacaoDTO(null, null, "Campus"),
                "Teju",
                "Descricao", StatusOcorrencia.EM_ABERTO, null,
                LocalDateTime.now(), LocalDateTime.now(), List.of(), List.of()
        );

        when(ocorrenciasService.buscarDetalhesPorProtocolo("OC-PUBLICO-1")).thenReturn(Optional.of(detalhada));

        mockMvc.perform(get("/ocorrencias/protocolo/OC-PUBLICO-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolo").value("OC-PUBLICO-1"));
    }

    @Test
    @DisplayName("GET /ocorrencias/{id} deve responder 403 Forbidden quando servico rejeita acesso a terceiros")
    void deveRetornar403AoBuscarDetalhesDeOutroUsuario() throws Exception {
        when(ocorrenciasService.buscarDetalhesPorId(10L))
                .thenThrow(new AcessoNegadoException("Acesso restrito ao criador, analista responsável ou administrador."));

        mockMvc.perform(get("/ocorrencias/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso proibido"));
    }

    @Test
    @DisplayName("GET /ocorrencias/usuario/{id} deve responder 403 Forbidden se tentar ver de outro usuario")
    void deveRetornar403AoListarOcorrenciasDeOutroUsuario() throws Exception {
        when(ocorrenciasService.listarPorUsuario(99L))
                .thenThrow(new AcessoNegadoException("Você só pode consultar suas próprias ocorrências."));

        mockMvc.perform(get("/ocorrencias/usuario/99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso proibido"));
    }

    @Test
    @DisplayName("POST /ocorrencias/{id}/vincular-analista deve responder 403 Forbidden se analista nao tem permissao")
    void deveRetornar403AoVincularSemPermissao() throws Exception {
        when(ocorrenciasService.vincularAnalista(1L))
                .thenThrow(new AcessoNegadoException("O cargo do usuário não permite assumir ocorrências."));

        mockMvc.perform(post("/ocorrencias/1/vincular-analista"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso proibido"));
    }

    @Test
    @DisplayName("PATCH /ocorrencias/{id}/cancelar deve responder 403 Forbidden se usuario nao puder cancelar")
    void deveRetornar403AoCancelarSemPermissao() throws Exception {
        CancelarOcorrenciaDTO dto = new CancelarOcorrenciaDTO("Cancelei");

        when(ocorrenciasService.cancelarOcorrencia(eq(1L), any()))
                .thenThrow(new AcessoNegadoException("O usuário não possui permissão para cancelar esta ocorrência."));

        mockMvc.perform(patch("/ocorrencias/1/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso proibido"));
    }
}
