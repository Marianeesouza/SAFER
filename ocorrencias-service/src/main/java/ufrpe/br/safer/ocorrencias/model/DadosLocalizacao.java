package ufrpe.br.safer.ocorrencias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DadosLocalizacao {

    @Column(name = "endereco", nullable = false, length = 500)
    private String endereco;

    @Embedded
    private Coordenadas coordenadas;
}
