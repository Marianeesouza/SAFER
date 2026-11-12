package ufrpe.br.safer.usuarios.dto;

import jakarta.validation.constraints.NotNull;
import ufrpe.br.safer.usuarios.model.Cargo;

public record AtualizarCargoDTO(
        @NotNull(message = "Novo cargo é obrigatório")
        Cargo novoCargo
) {
}
