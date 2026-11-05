package Componentes;

/**
 * +notificarNovaOcorrencia(ocorrencia: Ocorrencia): void
 * +notificarAtualizacaoStatus(ocorrencia: Ocorrencia, status: StatusOcorrencia): void
 * +notificarCancelamento(ocorrencia: Ocorrencia, motivo: String): void
 */
public interface IServiceNotificacoes {

	public abstract void notificarNovaOcorrencia();

	public abstract void notificarAtualizacaoStatus();

	public abstract void notificarCancelamento();

}
