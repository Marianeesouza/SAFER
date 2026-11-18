package ufrpe.br.safer.usuarios.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import ufrpe.br.safer.usuarios.model.Cargo;
import ufrpe.br.safer.usuarios.model.Usuario;
import ufrpe.br.safer.usuarios.repository.UsuarioRepository;

class AdministradorInicialTest {

    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    @Test
    void criaOAdministradorQuandoAindaNaoExiste() {
        when(repository.existsByLoginEmail("admin@ufrpe.br")).thenReturn(false);
        when(encoder.encode("senha-do-ambiente")).thenReturn("hash");

        new AdministradorInicial(repository, encoder, " Admin@ufrpe.br ", "senha-do-ambiente").run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        Usuario salvo = captor.getValue();
        assertEquals(Cargo.ADMINISTRADOR, salvo.getCargo());
        assertEquals("admin@ufrpe.br", salvo.getLogin().getEmail());
        assertEquals("hash", salvo.getLogin().getSenhaHash());
    }

    @Test
    void naoMexeNoAdministradorQueJaExiste() {
        when(repository.existsByLoginEmail("admin@ufrpe.br")).thenReturn(true);

        new AdministradorInicial(repository, encoder, "admin@ufrpe.br", "senha-do-ambiente").run(null);

        verify(repository, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void semVariaveisDeAmbienteNaoCriaNada() {
        new AdministradorInicial(repository, encoder, "", "").run(null);
        new AdministradorInicial(repository, encoder, "admin@ufrpe.br", " ").run(null);

        verifyNoInteractions(repository, encoder);
    }
}
