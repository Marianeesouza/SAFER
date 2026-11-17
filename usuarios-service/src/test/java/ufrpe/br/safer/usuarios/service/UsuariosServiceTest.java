package ufrpe.br.safer.usuarios.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ufrpe.br.safer.usuarios.config.JwtService;
import ufrpe.br.safer.usuarios.dto.AtualizarCargoDTO;
import ufrpe.br.safer.usuarios.dto.AtualizarPerfilDTO;
import ufrpe.br.safer.usuarios.dto.CadastrarUsuarioDTO;
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

    @Test
    void deveCadastrarUsuarioComCargoComumESenhaCriptografada() {
        CadastrarUsuarioDTO dto = new CadastrarUsuarioDTO(
                "Maria Silva",
                "Maria@ufrpe.br",
                "SenhaForte@123",
                RelacaoUfrpe.DISCENTE,
                null,
                "81999999999"
        );

        when(usuarioRepository.existsByLoginEmail("maria@ufrpe.br")).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte@123")).thenReturn("$2a$10$hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        UsuarioResponseDTO response = usuariosService.cadastrar(dto);

        assertEquals(1L, response.id());
        assertEquals("maria@ufrpe.br", response.email());
        assertEquals(Cargo.COMUM, response.cargo());
        assertEquals(RelacaoUfrpe.DISCENTE, response.relacaoUfrpe());
        verify(passwordEncoder).encode("SenhaForte@123");
    }

    @Test
    void deveLancarExcecaoAoCadastrarEmailDuplicado() {
        CadastrarUsuarioDTO dto = new CadastrarUsuarioDTO(
                "Maria Silva",
                "maria@ufrpe.br",
                "SenhaForte@123",
                RelacaoUfrpe.DISCENTE,
                null,
                null
        );

        when(usuarioRepository.existsByLoginEmail("maria@ufrpe.br")).thenReturn(true);

        assertThrows(EmailJaCadastradoException.class, () -> usuariosService.cadastrar(dto));
    }

    @Test
    void deveAutenticarUsuarioERetornarToken() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("Maria Silva")
                .login(Login.builder().email("maria@ufrpe.br").senhaHash("$2a$10$hashed").build())
                .relacaoUfrpe(RelacaoUfrpe.DISCENTE)
                .cargo(Cargo.COMUM)
                .build();

        when(usuarioRepository.findByLoginEmail("maria@ufrpe.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("SenhaForte@123", "$2a$10$hashed")).thenReturn(true);
        when(jwtService.gerarToken(usuario)).thenReturn("jwt-token-valido");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        TokenResponseDTO response = usuariosService.autenticar(new LoginDTO("maria@ufrpe.br", "SenhaForte@123"));

        assertNotNull(response);
        assertEquals("jwt-token-valido", response.tokenSessao());
        assertEquals("Bearer", response.tipo());
        assertEquals("maria@ufrpe.br", response.usuario().email());
    }

    @Test
    void deveLancarExcecaoQuandoSenhaIncorreta() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .login(Login.builder().email("maria@ufrpe.br").senhaHash("$2a$10$hashed").build())
                .build();

        when(usuarioRepository.findByLoginEmail("maria@ufrpe.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("SenhaErrada@123", "$2a$10$hashed")).thenReturn(false);

        assertThrows(CredenciaisInvalidasException.class,
                () -> usuariosService.autenticar(new LoginDTO("maria@ufrpe.br", "SenhaErrada@123")));
    }

    @Test
    void deveObterUsuarioViaTokenValido() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("Maria Silva")
                .login(Login.builder().email("maria@ufrpe.br").senhaHash("$2a$10$hashed").build())
                .relacaoUfrpe(RelacaoUfrpe.DOCENTE)
                .cargo(Cargo.ANALISTA)
                .build();

        when(jwtService.tokenValido("jwt-valido")).thenReturn(true);
        when(jwtService.extrairEmail("jwt-valido")).thenReturn("maria@ufrpe.br");
        when(usuarioRepository.findByLoginEmail("maria@ufrpe.br")).thenReturn(Optional.of(usuario));

        UsuarioResponseDTO response = usuariosService.obterUsuarioViaToken("Bearer jwt-valido");

        assertEquals("maria@ufrpe.br", response.email());
        assertEquals(Cargo.ANALISTA, response.cargo());
    }

    @Test
    void deveLancarExcecaoQuandoTokenAusenteOuInvalido() {
        assertThrows(CredenciaisInvalidasException.class, () -> usuariosService.obterUsuarioViaToken(null));
        when(jwtService.tokenValido("invalido")).thenReturn(false);
        assertThrows(CredenciaisInvalidasException.class, () -> usuariosService.obterUsuarioViaToken("Bearer invalido"));
    }

    @Test
    void devePromoverComumParaAnalistaQuandoSolicitanteForAdmin() {
        Usuario admin = Usuario.builder()
                .id(99L)
                .login(Login.builder().email("admin@ufrpe.br").senhaHash("$2a$10$hashed").build())
                .cargo(Cargo.ADMINISTRADOR)
                .build();

        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("Maria Silva")
                .login(Login.builder().email("maria@ufrpe.br").senhaHash("$2a$10$hashed").build())
                .relacaoUfrpe(RelacaoUfrpe.TECNICO)
                .cargo(Cargo.COMUM)
                .build();

        when(jwtService.tokenValido("token-admin")).thenReturn(true);
        when(jwtService.extrairEmail("token-admin")).thenReturn("admin@ufrpe.br");
        when(usuarioRepository.findByLoginEmail("admin@ufrpe.br")).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponseDTO response = usuariosService.atualizarCargo("Bearer token-admin", 1L, new AtualizarCargoDTO(Cargo.ANALISTA));

        assertEquals(Cargo.ANALISTA, response.cargo());
    }

    @Test
    void deveBloquearAlteracaoDeCargoQuandoSolicitanteNaoForAdmin() {
        Usuario comum = Usuario.builder()
                .id(2L)
                .login(Login.builder().email("comum@ufrpe.br").senhaHash("$2a$10$hashed").build())
                .cargo(Cargo.COMUM)
                .build();

        when(jwtService.tokenValido("token-comum")).thenReturn(true);
        when(jwtService.extrairEmail("token-comum")).thenReturn("comum@ufrpe.br");
        when(usuarioRepository.findByLoginEmail("comum@ufrpe.br")).thenReturn(Optional.of(comum));

        assertThrows(AcessoNegadoException.class,
                () -> usuariosService.atualizarCargo("Bearer token-comum", 1L, new AtualizarCargoDTO(Cargo.ANALISTA)));
    }

    @Test
    void deveBloquearRebaixamentoDeAdministrador() {
        Usuario admin = Usuario.builder()
                .id(1L)
                .nome("Admin")
                .login(Login.builder().email("admin@ufrpe.br").senhaHash("$2a$10$hashed").build())
                .relacaoUfrpe(RelacaoUfrpe.DOCENTE)
                .cargo(Cargo.ADMINISTRADOR)
                .build();

        when(jwtService.tokenValido("token-admin")).thenReturn(true);
        when(jwtService.extrairEmail("token-admin")).thenReturn("admin@ufrpe.br");
        when(usuarioRepository.findByLoginEmail("admin@ufrpe.br")).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(AlteracaoCargoInvalidaException.class,
                () -> usuariosService.atualizarCargo("Bearer token-admin", 1L, new AtualizarCargoDTO(Cargo.ANALISTA)));
    }

    @Test
    void deveAtualizarProprioPerfilExigindoSenhaAtualAoAlterarEmailOuSenha() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("Maria Silva")
                .login(Login.builder().email("maria@ufrpe.br").senhaHash("$2a$10$hashed").build())
                .relacaoUfrpe(RelacaoUfrpe.DISCENTE)
                .cargo(Cargo.COMUM)
                .build();

        when(jwtService.tokenValido("token-maria")).thenReturn(true);
        when(jwtService.extrairEmail("token-maria")).thenReturn("maria@ufrpe.br");
        when(usuarioRepository.findByLoginEmail("maria@ufrpe.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("SenhaAtual@123", "$2a$10$hashed")).thenReturn(true);
        when(usuarioRepository.existsByLoginEmail("maria.nova@ufrpe.br")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponseDTO response = usuariosService.atualizarProprioPerfil(
                "Bearer token-maria",
                new AtualizarPerfilDTO("Maria Souza", "maria.nova@ufrpe.br", null, "SenhaAtual@123", null, null, "81988887777")
        );

        assertEquals("Maria Souza", response.nome());
        assertEquals("maria.nova@ufrpe.br", response.email());
        assertEquals("81988887777", response.celular());
    }
}
