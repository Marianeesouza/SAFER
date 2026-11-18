package ufrpe.br.safer.usuarios.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ufrpe.br.safer.usuarios.config.JwtService;
import ufrpe.br.safer.usuarios.dto.AtualizarCargoDTO;
import ufrpe.br.safer.usuarios.dto.AtualizarPerfilDTO;
import ufrpe.br.safer.usuarios.dto.CadastrarUsuarioDTO;
import ufrpe.br.safer.usuarios.dto.LoginDTO;
import ufrpe.br.safer.usuarios.dto.PermissaoResponseDTO;
import ufrpe.br.safer.usuarios.dto.TokenResponseDTO;
import ufrpe.br.safer.usuarios.dto.UsuarioResponseDTO;
import ufrpe.br.safer.usuarios.dto.ValidarPermissaoDTO;
import ufrpe.br.safer.usuarios.exception.AcessoNegadoException;
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
    public UsuarioResponseDTO obterUsuarioLogado() {
        return UsuarioResponseDTO.fromEntity(obterUsuarioAutenticado());
    }

    @Transactional
    public UsuarioResponseDTO atualizarProprioPerfil(AtualizarPerfilDTO dto) {
        Usuario usuario = obterUsuarioAutenticado();

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
        Usuario solicitante = obterUsuarioAutenticado();
        if (!solicitante.getId().equals(id) && !solicitante.getCargo().ehAdministrador()) {
            throw new AcessoNegadoException("Acesso não autorizado aos dados de outro usuário.");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado com id: " + id));
        return UsuarioResponseDTO.fromEntity(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        Usuario solicitante = obterUsuarioAutenticado();
        if (!solicitante.getCargo().ehAdministrador()) {
            throw new AcessoNegadoException("Apenas administradores podem listar todos os usuários.");
        }

        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public UsuarioResponseDTO atualizarCargo(Long id, AtualizarCargoDTO dto) {
        Usuario solicitante = obterUsuarioAutenticado();
        if (!solicitante.getCargo().ehAdministrador()) {
            throw new AcessoNegadoException("Apenas administradores podem alterar cargos de usuários.");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado com id: " + id));

        // Transição delegada ao padrão State em Usuario
        usuario.mudarCargo(dto.novoCargo());

        Usuario atualizado = usuarioRepository.save(usuario);
        return UsuarioResponseDTO.fromEntity(atualizado);
    }

    @Transactional
    public void excluir(Long id) {
        Usuario solicitante = obterUsuarioAutenticado();
        if (!solicitante.getCargo().ehAdministrador()) {
            throw new AcessoNegadoException("Apenas administradores podem excluir usuários.");
        }
        if (solicitante.getId().equals(id)) {
            throw new AcessoNegadoException("Não é permitido excluir o próprio usuário autenticado.");
        }
        if (!usuarioRepository.existsById(id)) {
            throw new UsuarioNaoEncontradoException("Usuário não encontrado com id: " + id);
        }
        usuarioRepository.deleteById(id);
    }

    public Usuario obterUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new CredenciaisInvalidasException("Usuário não autenticado no contexto de segurança.");
        }

        String email = auth.getName();
        return usuarioRepository.findByLoginEmail(email)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário autenticado não encontrado na base de dados."));
    }

    @Transactional(readOnly = true)
    public PermissaoResponseDTO validarPermissaoOcorrencia(Long id, ValidarPermissaoDTO dto) {
        Usuario solicitante = obterUsuarioAutenticado();
        if (!solicitante.getId().equals(id) && !solicitante.getCargo().ehAdministrador()) {
            throw new AcessoNegadoException("O usuário autenticado não possui autorização para validar permissões de outro usuário.");
        }

        Usuario alvo = solicitante.getId().equals(id)
                ? solicitante
                : usuarioRepository.findById(id).orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado com id: " + id));

        boolean concedido = switch (dto.acao()) {
            case ASSUMIR -> alvo.podeAssumirOcorrencia();
            case ATUALIZAR -> alvo.podeAtualizarOcorrencia(dto.analistaResponsavelId());
            case CANCELAR -> alvo.podeCancelarOcorrencia(dto.criadorOcorrenciaId());
        };

        if (concedido) {
            return PermissaoResponseDTO.concedida();
        }

        String motivo = switch (dto.acao()) {
            case ASSUMIR -> "O cargo do usuário não permite assumir ocorrências.";
            case ATUALIZAR -> "O usuário não possui permissão para atualizar esta ocorrência (deve ser o analista responsável ou administrador).";
            case CANCELAR -> "O usuário não possui permissão para cancelar esta ocorrência (deve ser o criador ou administrador).";
        };

        return PermissaoResponseDTO.negada(motivo);
    }
}
