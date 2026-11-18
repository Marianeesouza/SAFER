package ufrpe.br.safer.ocorrencias.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import ufrpe.br.safer.ocorrencias.dto.DestinatarioDTO;
import ufrpe.br.safer.ocorrencias.messaging.command.EnviarEmailCommand;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaCanceladaEvent;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaRegistradaEvent;
import ufrpe.br.safer.ocorrencias.messaging.event.StatusOcorrenciaAlteradoEvent;
import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;

class EmailOcorrenciaBuilderTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 16, 10, 0);

    @Test
    void registroTrazProtocoloDadosDaOcorrenciaEChaveEstavel() {
        OcorrenciaRegistradaEvent evento = new OcorrenciaRegistradaEvent(7L, "OC-1", "Ana", "ana@ufrpe.br", "Sagui ferido",
                "Bloco A", StatusOcorrencia.EM_ABERTO, AGORA);

        EnviarEmailCommand comando = EmailOcorrenciaBuilder.registro(evento);

        assertThat(comando.chaveIdempotencia()).isEqualTo("ocorrencia-7-registrada");
        assertThat(comando.destinatario()).isEqualTo("ana@ufrpe.br");
        assertThat(comando.assunto()).contains("OC-1");
        assertThat(comando.corpo()).contains("Ana", "OC-1", "Sagui ferido", "Bloco A");
    }

    @Test
    void novaOcorrenciaParaAnalistaTrazDadosLinkEChavePorAnalista() {
        OcorrenciaRegistradaEvent evento = new OcorrenciaRegistradaEvent(7L, "OC-1", "Ana", "ana@ufrpe.br",
                "Sagui ferido", "Bloco A", StatusOcorrencia.EM_ABERTO, AGORA);
        DestinatarioDTO analista = new DestinatarioDTO(3L, "Carlos", "carlos@ufrpe.br");

        EnviarEmailCommand comando = EmailOcorrenciaBuilder.novaOcorrenciaParaAnalista(
                evento, analista, "http://sistema.local");

        assertThat(comando.chaveIdempotencia()).isEqualTo("ocorrencia-7-analista-3");
        assertThat(comando.destinatario()).isEqualTo("carlos@ufrpe.br");
        assertThat(comando.assunto()).contains("OC-1");
        assertThat(comando.corpo()).contains("Carlos", "OC-1", "Sagui ferido", "Bloco A", "http://sistema.local");
        // o e-mail do analista não expõe dados pessoais do comunicante
        assertThat(comando.corpo()).doesNotContain("Ana", "ana@ufrpe.br");
    }

    @Test
    void statusAlteradoUsaTextoAmigavelEChavePorStatus() {
        StatusOcorrenciaAlteradoEvent evento = new StatusOcorrenciaAlteradoEvent(7L, "OC-1", "ana@ufrpe.br",
                StatusOcorrencia.EM_ABERTO, StatusOcorrencia.EM_ATENDIMENTO, 3L, "obs interna", AGORA);

        EnviarEmailCommand comando = EmailOcorrenciaBuilder.statusAlterado(evento);

        assertThat(comando.chaveIdempotencia()).isEqualTo("ocorrencia-7-status-EM_ATENDIMENTO");
        assertThat(comando.corpo()).contains("OC-1", "em atendimento pela nossa equipe");
        // observação e id do analista são internos e não vão para o comunicante
        assertThat(comando.corpo()).doesNotContain("obs interna");
    }

    @Test
    void cancelamentoTrazOMotivo() {
        OcorrenciaCanceladaEvent evento = new OcorrenciaCanceladaEvent(7L, "OC-1", "ana@ufrpe.br", 1L, "Animal já foi embora", AGORA);

        EnviarEmailCommand comando = EmailOcorrenciaBuilder.cancelamento(evento);

        assertThat(comando.chaveIdempotencia()).isEqualTo("ocorrencia-7-cancelada");
        assertThat(comando.corpo()).contains("OC-1", "Animal já foi embora");
    }

    @Test
    void todoStatusTemDescricao() {
        for (StatusOcorrencia status : StatusOcorrencia.values()) {
            assertThat(EmailOcorrenciaBuilder.descricao(status)).isNotBlank();
        }
    }
}
