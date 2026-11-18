package ufrpe.br.safer.usuarios.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ufrpe.br.safer.usuarios.config.JwtService;
import ufrpe.br.safer.usuarios.dto.AtualizarCargoDTO;
import ufrpe.br.safer.usuarios.dto.AtualizarPerfilDTO;
import ufrpe.br.safer.usuarios.dto.CadastrarUsuarioDTO;
import ufrpe.br.safer.usuarios.dto.DestinatarioNotificacaoDTO;
import ufrpe.br.safer.usuarios.dto.LoginDTO;
import ufrpe.br.safer.usuarios.dto.TokenResponseDTO;
import ufrpe.br.safer.usuarios.dto.UsuarioResponseDTO;
import ufrpe.br.safer.usuarios.exception.AcessoNegadoException;
import ufrpe.br.safer.usuarios.exception.AlteracaoCargoInvalidaException;
import ufrpe.br.safer.usuarios.exception.CredenciaisInvalidasException;
import ufrpe.br.safer.usuarios.exception.EmailJaCadastradoException;
import ufrpe.br.safer.usuarios.model.Cargo;
import ufrpe.br.safer.usuarios.model.Login;
import ufrpe.br.safer.usuarios.model.RelacaoUfrpe;
import ufrpe.br.safer.usuarios.model.Usuario;
import ufrpe.br.safer.usuarios.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuariosServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UsuariosService usuariosService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String email) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void deveCadastrarUsuarioComCargoComumESenhaCriptografada() {
        CadastrarUsuarioDTO dto = new CadastrarUsuarioDTO(
                "João Silva",
                "joao@ufrpe.br",
                "SenhaForte@123",
                RelacaoUfrpe.DISCENTE,
                "8133200000",
                "81999990000"
        );

        when(usuarioRepository.existsByLoginEmail("joao@ufrpe.br")).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte@123")).thenReturn("$2a$10$hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        UsuarioResponseDTO response = usuariosService.cadastrar(dto);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("João Silva", response.nome());
        assertEquals("joao@ufrpe.br", response.email());
        assertEquals(Cargo.COMUM, response.cargo());
        assertEquals(RelacaoUfrpe.DISCENTE, response.relacaoUfrpe());
    }

    @Test
    void deveRejeitarCadastroComEmailJaExistente() {
        CadastrarUsuarioDTO dto = new CadastrarUsuarioDTO(
                "João Silva",
                "duplicado@ufrpe.br",
                "SenhaForte@123",
                RelacaoUfrpe.DISCENTE,
                null,
                "81999999999"
        );

        when(usuarioRepository.existsByLoginEmail("duplicado@ufrpe.br")).thenReturn(true);

        assertThrows(EmailJaCadastradoException.class, () -> usuariosService.cadastrar(dto));
    }

    @Test
    void deveAutenticarComCredenciaisValidas() {
        Login login = Login.builder()
                .email("joao@ufrpe.br")
                .senhaHash("$2a$10$hashed")
                .build();
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("João Silva")
                .login(login)
                .relacaoUfrpe(RelacaoUfrpe.DISCENTE)
                .cargo(Cargo.COMUM)
                .build();

        when(usuarioRepository.findByLoginEmail("joao@ufrpe.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("SenhaForte@123", "$2a$10$hashed")).thenReturn(true);
        when(jwtService.gerarToken(usuario)).thenReturn("jwt-token-valido");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        TokenResponseDTO response = usuariosService.autenticar(new LoginDTO("joao@ufrpe.br", "SenhaForte@123"));

        assertNotNull(response);
        assertEquals("jwt-token-valido", response.tokenSessao());
        assertEquals("Bearer", response.tipo());
        assertEquals(86400000L, response.expiraEmMs());
        assertNotNull(response.usuario());
        assertEquals("João Silva", response.usuario().nome());
    }

    @Test
    void deveRejeitarAutenticacaoComSenhaIncorreta() {
        Login login = Login.builder()
                .email("joao@ufrpe.br")
                .senhaHash("$2a$10$hashed")
                .build();
        Usuario usuario = Usuario.builder().id(1L).login(login).build();

        when(usuarioRepository.findByLoginEmail("joao@ufrpe.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-errada", "$2a$10$hashed")).thenReturn(false);

        assertThrows(CredenciaisInvalidasException.class,
                () -> usuariosService.autenticar(new LoginDTO("joao@ufrpe.br", "senha-errada")));
    }

    @Test
    void deveObterUsuarioLogadoViaSecurityContext() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("Maria Silva")
                .login(Login.builder().email("maria@ufrpe.br").build())
                .cargo(Cargo.ANALISTA)
                .build();

        autenticarComo("maria@ufrpe.br");
        when(usuarioRepository.findByLoginEmail("maria@ufrpe.br")).thenReturn(Optional.of(usuario));

        UsuarioResponseDTO response = usuariosService.obterUsuarioLogado();

        assertEquals("maria@ufrpe.br", response.email());
        assertEquals(Cargo.ANALISTA, response.cargo());
    }

    @Test
    void devePromoverComumParaAnalistaQuandoSolicitanteForAdmin() {
        Usuario admin = Usuario.builder()
                .id(99L)
                .nome("Admin")
                .login(Login.builder().email("admin@ufrpe.br").build())
                .cargo(Cargo.ADMINISTRADOR)
                .build();
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("José")
                .login(Login.builder().email("jose@ufrpe.br").build())
                .cargo(Cargo.COMUM)
                .build();

        autenticarComo("admin@ufrpe.br");
        when(usuarioRepository.findByLoginEmail("admin@ufrpe.br")).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponseDTO response = usuariosService.atualizarCargo(1L, new AtualizarCargoDTO(Cargo.ANALISTA));

        assertEquals(Cargo.ANALISTA, response.cargo());
    }

    @Test
    void deveBloquearAlteracaoDeCargoQuandoSolicitanteNaoForAdmin() {
        Usuario comum = Usuario.builder()
                .id(1L)
                .nome("Comum")
                .login(Login.builder().email("comum@ufrpe.br").build())
                .cargo(Cargo.COMUM)
                .build();

        autenticarComo("comum@ufrpe.br");
        when(usuarioRepository.findByLoginEmail("comum@ufrpe.br")).thenReturn(Optional.of(comum));

        assertThrows(AcessoNegadoException.class,
                () -> usuariosService.atualizarCargo(2L, new AtualizarCargoDTO(Cargo.ANALISTA)));
    }

    @Test
    void deveRejeitarTransicaoInvalidaSegundoRN04() {
        Usuario admin = Usuario.builder()
                .id(99L)
                .nome("Admin")
                .login(Login.builder().email("admin@ufrpe.br").build())
                .cargo(Cargo.ADMINISTRADOR)
                .build();

        autenticarComo("admin@ufrpe.br");
        when(usuarioRepository.findByLoginEmail("admin@ufrpe.br")).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(AlteracaoCargoInvalidaException.class,
                () -> usuariosService.atualizarCargo(1L, new AtualizarCargoDTO(Cargo.ANALISTA)));
    }

    @Test
    void deveAtualizarProprioPerfilComSucesso() {
        Login login = Login.builder()
                .email("maria@ufrpe.br")
                .senhaHash("$2a$10$hashed")
                .build();
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("Maria")
                .login(login)
                .relacaoUfrpe(RelacaoUfrpe.DISCENTE)
                .cargo(Cargo.COMUM)
                .build();

        autenticarComo("maria@ufrpe.br");
        when(usuarioRepository.findByLoginEmail("maria@ufrpe.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("SenhaAtual@123", "$2a$10$hashed")).thenReturn(true);
        when(usuarioRepository.existsByLoginEmail("maria.nova@ufrpe.br")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponseDTO response = usuariosService.atualizarProprioPerfil(
                new AtualizarPerfilDTO("Maria Souza", "maria.nova@ufrpe.br", "NovaSenha@123", "SenhaAtual@123", null, null, "81988887777")
        );

        assertEquals("Maria Souza", response.nome());
        assertEquals("maria.nova@ufrpe.br", response.email());
        assertEquals("81988887777", response.celular());
    }

    @Test
    void deveListarTodosQuandoSolicitanteForAdmin() {
        Usuario admin = Usuario.builder()
                .id(99L)
                .login(Login.builder().email("admin@ufrpe.br").build())
                .cargo(Cargo.ADMINISTRADOR)
                .build();

        autenticarComo("admin@ufrpe.br");
        when(usuarioRepository.findByLoginEmail("admin@ufrpe.br")).thenReturn(Optional.of(admin));
        when(usuarioRepository.findAll()).thenReturn(List.of(admin));

        List<UsuarioResponseDTO> list = usuariosService.listarTodos();
        assertEquals(1, list.size());
    }

    @Test
    void deveBloquearListarTodosQuandoSolicitanteNaoForAdmin() {
        Usuario comum = Usuario.builder()
                .id(1L)
                .login(Login.builder().email("comum@ufrpe.br").build())
                .cargo(Cargo.COMUM)
                .build();

        autenticarComo("comum@ufrpe.br");
        when(usuarioRepository.findByLoginEmail("comum@ufrpe.br")).thenReturn(Optional.of(comum));

        assertThrows(AcessoNegadoException.class, () -> usuariosService.listarTodos());
    }

    @Test
    void devePermitirBuscarPorIdSeForOProprioUsuario() {
        Usuario comum = Usuario.builder()
                .id(1L)
                .login(Login.builder().email("comum@ufrpe.br").build())
                .cargo(Cargo.COMUM)
                .build();

        autenticarComo("comum@ufrpe.br");
        when(usuarioRepository.findByLoginEmail("comum@ufrpe.br")).thenReturn(Optional.of(comum));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(comum));

        UsuarioResponseDTO res = usuariosService.buscarPorId(1L);
        assertEquals(1L, res.id());
    }

    @Test
    void deveBloquearBuscarPorIdDeOutroUsuarioSeNaoForAdmin() {
        Usuario comum = Usuario.builder()
                .id(1L)
                .login(Login.builder().email("comum@ufrpe.br").build())
                .cargo(Cargo.COMUM)
                .build();

        autenticarComo("comum@ufrpe.br");
        when(usuarioRepository.findByLoginEmail("comum@ufrpe.br")).thenReturn(Optional.of(comum));

        assertThrows(AcessoNegadoException.class, () -> usuariosService.buscarPorId(2L));
    }

    @Test
    void deveListarAnalistasEAdministradoresComoDestinatariosDeNovasOcorrencias() {
        Usuario analista = Usuario.builder()
                .id(1L).nome("Ana Analista").cargo(Cargo.ANALISTA)
                .login(Login.builder().email("ana@ufrpe.br").senhaHash("hash").build())
                .build();
        Usuario admin = Usuario.builder()
                .id(2L).nome("Admin").cargo(Cargo.ADMINISTRADOR)
                .login(Login.builder().email("admin@ufrpe.br").senhaHash("hash").build())
                .build();
        Usuario comum = Usuario.builder()
                .id(3L).nome("Maria Comum").cargo(Cargo.COMUM)
                .login(Login.builder().email("maria@ufrpe.br").senhaHash("hash").build())
                .build();
        when(usuarioRepository.findAll()).thenReturn(List.of(comum, analista, admin));

        List<DestinatarioNotificacaoDTO> destinatarios = usuariosService.listarDestinatariosDeNovasOcorrencias();

        assertEquals(List.of(
                new DestinatarioNotificacaoDTO(1L, "Ana Analista", "ana@ufrpe.br"),
                new DestinatarioNotificacaoDTO(2L, "Admin", "admin@ufrpe.br")), destinatarios);
    }
}
