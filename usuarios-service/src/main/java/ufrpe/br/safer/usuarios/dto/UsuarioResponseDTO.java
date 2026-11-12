package ufrpe.br.safer.usuarios.dto;

import ufrpe.br.safer.usuarios.model.Cargo;
import ufrpe.br.safer.usuarios.model.RelacaoUfrpe;
import ufrpe.br.safer.usuarios.model.Usuario;

import java.time.LocalDateTime;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        RelacaoUfrpe relacaoUfrpe,
        Cargo cargo,
        String telefoneFixo,
        String celular,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static UsuarioResponseDTO fromEntity(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getLogin().getEmail(),
                usuario.getRelacaoUfrpe(),
                usuario.getCargo(),
                usuario.getTelefoneFixo(),
                usuario.getCelular(),
                usuario.getCriadoEm(),
                usuario.getAtualizadoEm()
        );
    }
}
