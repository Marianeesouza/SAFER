package ufrpe.br.safer.usuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ufrpe.br.safer.usuarios.model.cargo.CargoUsuario;
import ufrpe.br.safer.usuarios.model.cargo.CargoUsuarioFactory;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Embedded
    private Login login;

    @Enumerated(EnumType.STRING)
    @Column(name = "relacao_ufrpe", nullable = false, length = 30)
    private RelacaoUfrpe relacaoUfrpe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Cargo cargo;

    @Column(name = "telefone_fixo", length = 20)
    private String telefoneFixo;

    @Column(length = 20)
    private String celular;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Transient
    @Setter(AccessLevel.NONE)
    private CargoUsuario cargoState;

    @PostLoad
    void postLoad() {
        this.cargoState = CargoUsuarioFactory.criar(this, this.cargo);
    }

    public CargoUsuario getCargoState() {
        if (this.cargoState == null) {
            this.cargoState = CargoUsuarioFactory.criar(this, this.cargo);
        }
        return this.cargoState;
    }

    public void mudarCargo(Cargo novoCargo) {
        if (!getCargoState().podeMudarPara(novoCargo)) {
            throw new ufrpe.br.safer.usuarios.exception.AlteracaoCargoInvalidaException(
                    "Transição de cargo inválida de " + this.cargo + " para " + novoCargo + "."
            );
        }
        this.cargo = novoCargo;
        this.cargoState = CargoUsuarioFactory.criar(this, novoCargo);
    }

    public boolean podeAssumirOcorrencia() {
        return getCargoState().podeAssumirOcorrencia();
    }

    public boolean podeAtualizarOcorrencia(Long analistaResponsavelId) {
        return getCargoState().podeAtualizarOcorrencia(analistaResponsavelId);
    }

    public boolean podeCancelarOcorrencia(Long criadorOcorrenciaId) {
        return getCargoState().podeCancelarOcorrencia(criadorOcorrenciaId);
    }

    public boolean notificarNovaOcorrencia() {
        return getCargoState().notificarNovaOcorrencia();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
        if (this.cargo == null) {
            this.cargo = Cargo.COMUM;
        }
        this.cargoState = CargoUsuarioFactory.criar(this, this.cargo);
    }

    @PreUpdate
    void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
