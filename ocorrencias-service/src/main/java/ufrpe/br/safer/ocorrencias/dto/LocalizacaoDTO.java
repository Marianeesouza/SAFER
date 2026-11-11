package ufrpe.br.safer.ocorrencias.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ufrpe.br.safer.ocorrencias.model.Ocorrencia;

public record LocalizacaoDTO(
        @DecimalMin(value = "-90.0", message = "Latitude deve estar entre -90 e 90")
        @DecimalMax(value = "90.0", message = "Latitude deve estar entre -90 e 90")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "Longitude deve estar entre -180 e 180")
        @DecimalMax(value = "180.0", message = "Longitude deve estar entre -180 e 180")
        Double longitude,

        @NotBlank(message = "O endereço é obrigatório")
        @Size(max = 500, message = "O endereço não pode exceder 500 caracteres")
        String endereco
) {
    public static LocalizacaoDTO fromEntity(Ocorrencia o) {
        return new LocalizacaoDTO(o.getLatitude(), o.getLongitude(), o.getEndereco());
    }
}
