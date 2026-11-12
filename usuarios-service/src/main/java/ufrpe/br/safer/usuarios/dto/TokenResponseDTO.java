package ufrpe.br.safer.usuarios.dto;

public record TokenResponseDTO(
        String tokenSessao,
        String tipo,
        long expiraEmMs,
        UsuarioResponseDTO usuario
) {
}
