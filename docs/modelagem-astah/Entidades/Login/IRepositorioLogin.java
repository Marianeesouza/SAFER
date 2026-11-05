package Entidades.Login;

public interface IRepositorioLogin {

	public abstract void inserir(Login login);

	public abstract void atualizar(Login login);

	public abstract void deletar(Login login);

	public abstract Login consultar(Login login);

}
