package ufrpe.br.safer.ocorrencias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DadosComunicante {

    @Column(name = "comunicante_nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "comunicante_email", nullable = false, length = 150)
    private String email;

    @Column(name = "comunicante_telefone", length = 30)
    private String telefone;

    @Column(name = "comunicante_relacao_ufrpe", length = 50)
    private String relacaoUfrpe;
}
