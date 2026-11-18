package ufrpe.br.safer.usuarios.dto;

public record PermissaoResponseDTO(
        boolean permitido,
        String motivoRecusa
) {
    public static PermissaoResponseDTO concedida() {
        return new PermissaoResponseDTO(true, null);
    }

    public static PermissaoResponseDTO negada(String motivo) {
        return new PermissaoResponseDTO(false, motivo);
    }
}
