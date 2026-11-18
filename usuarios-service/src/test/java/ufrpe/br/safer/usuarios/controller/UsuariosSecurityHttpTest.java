package ufrpe.br.safer.usuarios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ufrpe.br.safer.usuarios.config.JwtAuthenticationFilter;
import ufrpe.br.safer.usuarios.config.JwtService;
import ufrpe.br.safer.usuarios.config.SecurityConfig;
import ufrpe.br.safer.usuarios.dto.UsuarioResponseDTO;
import ufrpe.br.safer.usuarios.exception.AcessoNegadoException;
import ufrpe.br.safer.usuarios.exception.CredenciaisInvalidasException;
import ufrpe.br.safer.usuarios.model.Cargo;
import ufrpe.br.safer.usuarios.model.RelacaoUfrpe;
import ufrpe.br.safer.usuarios.service.UsuariosService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuariosSecurityHttpTest {

    private final UsuariosService usuariosService = mock(UsuariosService.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;
    private JwtAuthenticationFilter jwtFilter;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
        jwtFilter = new JwtAuthenticationFilter(jwtService);
        UsuariosController usuariosController = new UsuariosController(usuariosService);
        AuthController authController = new AuthController(usuariosService);

        mockMvc = MockMvcBuilders.standaloneSetup(usuariosController, authController)
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    @DisplayName("JwtAuthenticationFilter deve autenticar token válido com ROLE_<CARGO>")
    void deveAutenticarTokenValidoComRole() throws Exception {
        when(jwtService.tokenValido("token-valido")).thenReturn(true);
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.getSubject()).thenReturn("admin@ufrpe.br");
        when(claims.get("cargo", String.class)).thenReturn("ADMINISTRADOR");
        when(jwtService.extrairClaims("token-valido")).thenReturn(claims);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-valido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtFilter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("admin@ufrpe.br", SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals("ROLE_ADMINISTRADOR", SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("JwtAuthenticationFilter não deve autenticar quando cabeçalho ausente")
    void naoDeveAutenticarQuandoSemCabecalho() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtFilter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("GET /usuarios sem permissão de admin deve retornar 403 Forbidden")
    void deveRetornar403QuandoNaoForAdminAoListar() throws Exception {
        when(usuariosService.listarTodos()).thenThrow(new AcessoNegadoException("Apenas administradores podem listar todos os usuários."));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /usuarios com admin deve retornar 200 OK")
    void deveRetornar200QuandoAdminAoListar() throws Exception {
        when(usuariosService.listarTodos()).thenReturn(List.of(
                new UsuarioResponseDTO(1L, "Admin", "admin@ufrpe.br", RelacaoUfrpe.DOCENTE, Cargo.ADMINISTRADOR, null, null, LocalDateTime.now(), LocalDateTime.now())
        ));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /auth/me sem autenticação deve retornar 401 Unauthorized")
    void deveRetornar401QuandoNaoAutenticado() throws Exception {
        when(usuariosService.obterUsuarioLogado()).thenThrow(new CredenciaisInvalidasException("Usuário não autenticado no contexto de segurança."));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /usuarios deve retornar 201 Created no autocadastro")
    void devePermitirAutocadastro() throws Exception {
        when(usuariosService.cadastrar(any())).thenReturn(
                new UsuarioResponseDTO(2L, "Visitante", "vis@ufrpe.br", RelacaoUfrpe.VISITANTE, Cargo.COMUM, null, null, LocalDateTime.now(), LocalDateTime.now())
        );

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Visitante\",\"email\":\"vis@ufrpe.br\",\"senha\":\"SenhaForte@123\",\"relacaoUfrpe\":\"VISITANTE\"}"))
                .andExpect(status().isCreated());
    }
}
