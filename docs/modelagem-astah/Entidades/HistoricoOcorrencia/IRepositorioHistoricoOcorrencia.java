package Entidades.HistoricoOcorrencia;

public interface IRepositorioHistoricoOcorrencia {

	public abstract void inserir(HistoricoOcorrencia historicoOcorrencia);

	public abstract void atualizar(HistoricoOcorrencia historicoOcorrencia);

	public abstract void deletar(HistoricoOcorrencia historicoOcorrencia);

	public abstract HistoricoOcorrencia consultar(HistoricoOcorrencia historicoOcorrencia);

}
