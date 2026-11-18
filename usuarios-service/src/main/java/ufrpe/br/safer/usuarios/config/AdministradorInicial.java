package ufrpe.br.safer.usuarios.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ufrpe.br.safer.usuarios.model.Cargo;
import ufrpe.br.safer.usuarios.model.Login;
import ufrpe.br.safer.usuarios.model.RelacaoUfrpe;
import ufrpe.br.safer.usuarios.model.Usuario;
import ufrpe.br.safer.usuarios.repository.UsuarioRepository;

// Cria o primeiro administrador ao subir, a partir de ADMIN_EMAIL e ADMIN_SENHA
@Component
@Slf4j
public class AdministradorInicial implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;

    public AdministradorInicial(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${safer.admin.email:}") String email,
            @Value("${safer.admin.senha:}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || senha.isBlank()) {
            log.warn("ADMIN_EMAIL e ADMIN_SENHA não definidos: nenhum administrador inicial será criado.");
            return;
        }
        String emailNormalizado = email.trim().toLowerCase();
        if (usuarioRepository.existsByLoginEmail(emailNormalizado)) {
            return;
        }
        Usuario administrador = Usuario.builder()
                .nome("Administrador SAFER")
                .login(Login.builder()
                        .email(emailNormalizado)
                        .senhaHash(passwordEncoder.encode(senha))
                        .build())
                .relacaoUfrpe(RelacaoUfrpe.TECNICO)
                .cargo(Cargo.ADMINISTRADOR)
                .build();
        usuarioRepository.save(administrador);
        log.info("Administrador inicial criado: {}", emailNormalizado);
    }
}
