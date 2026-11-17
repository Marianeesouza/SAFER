package ufrpe.br.safer.usuarios.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ufrpe.br.safer.usuarios.model.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLoginEmail(String email);

    boolean existsByLoginEmail(String email);
}
