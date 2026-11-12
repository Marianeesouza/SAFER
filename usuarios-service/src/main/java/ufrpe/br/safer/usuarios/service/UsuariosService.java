package ufrpe.br.safer.usuarios.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import ufrpe.br.safer.usuarios.exception.UsuarioNaoEncontradoException;
import ufrpe.br.safer.usuarios.model.Cargo;
import ufrpe.br.safer.usuarios.model.Login;
import ufrpe.br.safer.usuarios.model.Usuario;
import ufrpe.br.safer.usuarios.repository.UsuarioRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuariosService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public UsuarioResponseDTO cadastrar(CadastrarUsuarioDTO dto) {
        String emailNormalizado = dto.email().trim().toLowerCase();
        if (usuarioRepository.existsByLoginEmail(emailNormalizado)) {
            throw new EmailJaCadastradoException("Já existe um usuário cadastrado com o e-mail informado.");
        }

        Login login = Login.builder()
                .email(emailNormalizado)
                .senhaHash(passwordEncoder.encode(dto.senha()))
                .build();

        Usuario usuario = Usuario.builder()
                .nome(dto.nome().trim())
                .login(login)
                .relacaoUfrpe(dto.relacaoUfrpe())
                .cargo(Cargo.COMUM)
                .telefoneFixo(dto.telefoneFixo() != null ? dto.telefoneFixo().trim() : null)
                .celular(dto.celular() != null ? dto.celular().trim() : null)
                .build();

        Usuario salvo = usuarioRepository.save(usuario);
        return UsuarioResponseDTO.fromEntity(salvo);
    }

    @Transactional(readOnly = true)
    public TokenResponseDTO autenticar(LoginDTO dto) {
        String emailNormalizado = dto.email().trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByLoginEmail(emailNormalizado)
                .orElseThrow(() -> new CredenciaisInvalidasException("E-mail ou senha inválidos."));

        if (!passwordEncoder.matches(dto.senha(), usuario.getLogin().getSenhaHash())) {
            throw new CredenciaisInvalidasException("E-mail ou senha inválidos.");
        }

        String token = jwtService.gerarToken(usuario);
        return new TokenResponseDTO(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                UsuarioResponseDTO.fromEntity(usuario)
        );
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO obterUsuarioViaToken(String authorizationHeader) {
        return UsuarioResponseDTO.fromEntity(autenticarPorHeader(authorizationHeader));
    }

    @Transactional
    public UsuarioResponseDTO atualizarProprioPerfil(String authorizationHeader, AtualizarPerfilDTO dto) {
        Usuario usuario = autenticarPorHeader(authorizationHeader);

        boolean alteraEmail = dto.email() != null && !dto.email().isBlank();
        boolean alteraSenha = dto.novaSenha() != null && !dto.novaSenha().isBlank();

        if (alteraEmail || alteraSenha) {
            if (dto.senhaAtual() == null || !passwordEncoder.matches(dto.senhaAtual(), usuario.getLogin().getSenhaHash())) {
                throw new CredenciaisInvalidasException("Senha atual inválida ou não informada para alteração de credenciais.");
            }
        }

        if (dto.nome() != null && !dto.nome().isBlank()) {
            usuario.setNome(dto.nome().trim());
        }

        if (dto.relacaoUfrpe() != null) {
            usuario.setRelacaoUfrpe(dto.relacaoUfrpe());
        }

        if (dto.telefoneFixo() != null) {
            usuario.setTelefoneFixo(dto.telefoneFixo().trim());
        }

        if (dto.celular() != null) {
            usuario.setCelular(dto.celular().trim());
        }

        if (alteraEmail) {
            String novoEmail = dto.email().trim().toLowerCase();
            if (!novoEmail.equals(usuario.getLogin().getEmail()) && usuarioRepository.existsByLoginEmail(novoEmail)) {
                throw new EmailJaCadastradoException("Já existe um usuário cadastrado com o e-mail informado.");
            }
            usuario.getLogin().setEmail(novoEmail);
        }

        if (alteraSenha) {
            usuario.getLogin().setSenhaHash(passwordEncoder.encode(dto.novaSenha()));
        }

        Usuario atualizado = usuarioRepository.save(usuario);
        return UsuarioResponseDTO.fromEntity(atualizado);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado com id: " + id));
        return UsuarioResponseDTO.fromEntity(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public UsuarioResponseDTO atualizarCargo(String authorizationHeader, Long id, AtualizarCargoDTO dto) {
        Usuario solicitante = autenticarPorHeader(authorizationHeader);
        if (solicitante.getCargo() != Cargo.ADMINISTRADOR) {
            throw new AcessoNegadoException("Apenas administradores podem alterar cargos de usuários.");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado com id: " + id));

        if (!usuario.getCargo().podeAlterarPara(dto.novoCargo())) {
            throw new AlteracaoCargoInvalidaException(
                    "Transição de cargo inválida de " + usuario.getCargo() + " para " + dto.novoCargo() + "."
            );
        }

        usuario.setCargo(dto.novoCargo());
        Usuario atualizado = usuarioRepository.save(usuario);
        return UsuarioResponseDTO.fromEntity(atualizado);
    }

    private Usuario autenticarPorHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CredenciaisInvalidasException("Token de autenticação ausente ou inválido.");
        }

        String token = authorizationHeader.substring(7).trim();
        if (!jwtService.tokenValido(token)) {
            throw new CredenciaisInvalidasException("Token de autenticação expirado ou inválido.");
        }

        String email = jwtService.extrairEmail(token);
        return usuarioRepository.findByLoginEmail(email)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário associado ao token não foi encontrado."));
    }
}
