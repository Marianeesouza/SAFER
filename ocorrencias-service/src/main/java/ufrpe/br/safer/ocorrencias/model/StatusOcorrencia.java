package ufrpe.br.safer.ocorrencias.model;

public enum StatusOcorrencia {
    EM_ABERTO,
    EM_ATENDIMENTO,
    NECESSITA_ATENDIMENTO_MEDICO,
    FINALIZADO_COM_SUCESSO,
    FINALIZADO_COM_COMPLICACOES,
    CANCELADO;

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
