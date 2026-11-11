package ufrpe.br.safer.ocorrencias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ocorrencia")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ocorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String protocolo;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "comunicante_nome", nullable = false, length = 150)
    private String comunicanteNome;

    @Column(name = "comunicante_email", nullable = false, length = 150)
    private String comunicanteEmail;

    @Column(name = "comunicante_telefone", length = 30)
    private String comunicanteTelefone;

    @Column(name = "comunicante_relacao_ufrpe", length = 50)
    private String comunicanteRelacaoUfrpe;

    @Column(name = "descricao_animal", nullable = false, columnDefinition = "TEXT")
    private String descricaoAnimal;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(nullable = false, length = 500)
    private String endereco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private StatusOcorrencia status = StatusOcorrencia.EM_ABERTO;

    @Column(name = "analista_responsavel_id")
    private Long analistaResponsavelId;

    @jakarta.persistence.Version
    @Column(nullable = false)
    @Builder.Default
    private Long versao = 0L;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
