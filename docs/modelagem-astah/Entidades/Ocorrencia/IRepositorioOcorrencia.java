package Entidades.Ocorrencia;

public interface IRepositorioOcorrencia {

	public abstract void inserir(Ocorrencia ocorrencia);

	public abstract void atualizar(Ocorrencia ocorrencia);

	public abstract void deletar(Ocorrencia ocorrencia);

	public abstract Ocorrencia consultar(Ocorrencia ocorrencia);

}
