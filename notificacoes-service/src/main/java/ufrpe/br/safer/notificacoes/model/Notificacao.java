package ufrpe.br.safer.notificacoes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificacao")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_idempotencia", nullable = false, unique = true, length = 200)
    private String chaveIdempotencia;

    @Column(nullable = false, length = 150)
    private String destinatario;

    @Column(nullable = false)
    private String assunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusNotificacao status;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    protected Notificacao() {
    }

    /** Cria o registro já com o resultado da primeira tentativa */
    public static Notificacao primeiraTentativa(String chaveIdempotencia, String destinatario, String assunto,
                                                boolean enviada) {
        var notificacao = new Notificacao();
        notificacao.chaveIdempotencia = chaveIdempotencia;
        notificacao.destinatario = destinatario;
        notificacao.assunto = assunto;
        notificacao.criadoEm = LocalDateTime.now();
        notificacao.registrarTentativa(enviada);
        return notificacao;
    }

    /** Registra o resultado de uma nova tentativa */
    public void registrarTentativa(boolean enviada) {
        this.tentativas++;
        this.status = enviada ? StatusNotificacao.ENVIADA : StatusNotificacao.FALHA;
        this.atualizadoEm = LocalDateTime.now();
    }

    public boolean foiEnviada() {
        return status == StatusNotificacao.ENVIADA;
    }

    public Long getId() {
        return id;
    }

    public String getChaveIdempotencia() {
        return chaveIdempotencia;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getAssunto() {
        return assunto;
    }

    public StatusNotificacao getStatus() {
        return status;
    }

    public int getTentativas() {
        return tentativas;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
