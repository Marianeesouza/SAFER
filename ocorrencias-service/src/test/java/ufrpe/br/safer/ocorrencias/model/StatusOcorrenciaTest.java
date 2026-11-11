package ufrpe.br.safer.ocorrencias.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ufrpe.br.safer.ocorrencias.exception.BusinessRuleException;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusOcorrenciaTest {

    @Test
    @DisplayName("Cada status tem um codigo numerico proprio")
    void codigosSaoUnicos() {
        assertThat(Arrays.stream(StatusOcorrencia.values()).map(StatusOcorrencia::getCodigo))
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Os codigos gravados no banco nao podem mudar")
    void codigosSaoOsEsperados() {
        assertThat(StatusOcorrencia.EM_ABERTO.getCodigo()).isEqualTo(1);
        assertThat(StatusOcorrencia.EM_ATENDIMENTO.getCodigo()).isEqualTo(2);
        assertThat(StatusOcorrencia.NECESSITA_ATENDIMENTO_MEDICO.getCodigo()).isEqualTo(3);
        assertThat(StatusOcorrencia.FINALIZADO_COM_SUCESSO.getCodigo()).isEqualTo(4);
        assertThat(StatusOcorrencia.FINALIZADO_COM_COMPLICACOES.getCodigo()).isEqualTo(5);
        assertThat(StatusOcorrencia.CANCELADO.getCodigo()).isEqualTo(6);
    }

    @Test
    @DisplayName("deCodigo devolve o status de cada codigo")
    void deCodigoDevolveOStatus() {
        for (StatusOcorrencia status : StatusOcorrencia.values()) {
            assertThat(StatusOcorrencia.deCodigo(status.getCodigo())).isEqualTo(status);
        }
    }

    @Test
    @DisplayName("Codigo desconhecido lanca erro de regra de negocio listando os valores aceitos")
    void codigoDesconhecido() {
        assertThatThrownBy(() -> StatusOcorrencia.deCodigo(0))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Status inválido: 0")
                .hasMessageContaining("1=EM_ABERTO");
    }

    @Test
    @DisplayName("O texto de documentacao do Swagger bate com os valores do enum")
    void descricaoDosCodigosBateComOEnum() {
        String esperado = Arrays.stream(StatusOcorrencia.values())
                .map(s -> s.getCodigo() + "=" + s.name())
                .collect(Collectors.joining(", "));
        assertThat(StatusOcorrencia.DESCRICAO_CODIGOS).isEqualTo(esperado);
    }

    @Test
    @DisplayName("O conversor grava o codigo numerico e le de volta o status")
    void conversorFazIdaEVolta() {
        StatusOcorrenciaConverter conversor = new StatusOcorrenciaConverter();

        assertThat(conversor.convertToDatabaseColumn(StatusOcorrencia.EM_ATENDIMENTO)).isEqualTo(2);
        assertThat(conversor.convertToEntityAttribute(6)).isEqualTo(StatusOcorrencia.CANCELADO);
        assertThat(conversor.convertToDatabaseColumn(null)).isNull();
        assertThat(conversor.convertToEntityAttribute(null)).isNull();
    }
}
