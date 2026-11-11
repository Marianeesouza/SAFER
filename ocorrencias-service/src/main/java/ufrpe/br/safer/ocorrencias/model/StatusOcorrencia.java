package ufrpe.br.safer.ocorrencias.model;

import lombok.Getter;
import ufrpe.br.safer.ocorrencias.exception.BusinessRuleException;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
public enum StatusOcorrencia {
    EM_ABERTO(1),
    EM_ATENDIMENTO(2),
    NECESSITA_ATENDIMENTO_MEDICO(3),
    FINALIZADO_COM_SUCESSO(4),
    FINALIZADO_COM_COMPLICACOES(5),
    CANCELADO(6);

    public static final String DESCRICAO_CODIGOS = "1=EM_ABERTO, 2=EM_ATENDIMENTO, 3=NECESSITA_ATENDIMENTO_MEDICO, "
            + "4=FINALIZADO_COM_SUCESSO, 5=FINALIZADO_COM_COMPLICACOES, 6=CANCELADO";

    private final int codigo;

    StatusOcorrencia(int codigo) {
        this.codigo = codigo;
    }

    public static StatusOcorrencia deCodigo(int codigo) {
        return Arrays.stream(values())
                .filter(s -> s.codigo == codigo)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Status inválido: " + codigo + ". Valores aceitos: "
                                + Arrays.stream(values()).map(s -> s.codigo + "=" + s.name()).collect(Collectors.joining(", ")) + "."));
    }

    public boolean podeTransitarPara(StatusOcorrencia novoStatus) {
        if (novoStatus == null || novoStatus == this) {
            return false;
        }

        return switch (this) {
            case EM_ABERTO -> novoStatus == EM_ATENDIMENTO || novoStatus == CANCELADO;
            case EM_ATENDIMENTO -> novoStatus == NECESSITA_ATENDIMENTO_MEDICO
                    || novoStatus == FINALIZADO_COM_SUCESSO
                    || novoStatus == FINALIZADO_COM_COMPLICACOES;
            case NECESSITA_ATENDIMENTO_MEDICO -> novoStatus == FINALIZADO_COM_SUCESSO
                    || novoStatus == FINALIZADO_COM_COMPLICACOES;
            case FINALIZADO_COM_SUCESSO, FINALIZADO_COM_COMPLICACOES, CANCELADO -> false;
        };
    }
}
