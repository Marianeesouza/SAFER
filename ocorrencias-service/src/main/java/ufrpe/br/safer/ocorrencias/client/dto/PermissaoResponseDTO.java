package ufrpe.br.safer.ocorrencias.client.dto;

public record PermissaoResponseDTO(
        boolean permitido,
        String motivoRecusa
) {}
