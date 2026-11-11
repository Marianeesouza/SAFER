package ufrpe.br.safer.ocorrencias.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComunicanteDTO(
        @NotBlank(message = "O nome do comunicante é obrigatório")
        @Size(max = 150, message = "O nome não pode exceder 150 caracteres")
        String nome,

        @NotBlank(message = "O e-mail do comunicante é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "O e-mail não pode exceder 150 caracteres")
        String email,

        @Size(max = 30, message = "O telefone não pode exceder 30 caracteres")
        String telefone,

        @Size(max = 50, message = "A relação com a UFRPE não pode exceder 50 caracteres")
        String relacaoUfrpe
) {}
