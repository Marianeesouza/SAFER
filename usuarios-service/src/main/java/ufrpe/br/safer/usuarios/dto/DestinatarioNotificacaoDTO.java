package ufrpe.br.safer.usuarios.dto;

import ufrpe.br.safer.usuarios.model.Usuario;

public record DestinatarioNotificacaoDTO(
        Long id,
        String nome,
        String email
) {
    public static DestinatarioNotificacaoDTO fromEntity(Usuario usuario) {
        return new DestinatarioNotificacaoDTO(usuario.getId(), usuario.getNome(), usuario.getLogin().getEmail());
    }
}
