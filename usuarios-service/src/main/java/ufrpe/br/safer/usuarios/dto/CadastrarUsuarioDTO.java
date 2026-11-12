package ufrpe.br.safer.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ufrpe.br.safer.usuarios.model.RelacaoUfrpe;

public record CadastrarUsuarioDTO(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 180, message = "E-mail deve ter no máximo 180 caracteres")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%&*!?\\-_]).{10,}$",
                message = "A senha deve ter no mínimo 10 caracteres, contendo letra maiúscula, minúscula, número e caractere especial (@#$%&*!?-_)"
        )
        String senha,

        @NotNull(message = "Relação com a UFRPE é obrigatória")
        RelacaoUfrpe relacaoUfrpe,

        @Size(max = 20, message = "Telefone fixo deve ter no máximo 20 caracteres")
        String telefoneFixo,

        @Size(max = 20, message = "Celular deve ter no máximo 20 caracteres")
        String celular
) {
}
