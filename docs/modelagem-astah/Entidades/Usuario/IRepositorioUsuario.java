package Entidades.Usuario;

public interface IRepositorioUsuario {

	public abstract void inserir(Usuario usuario);

	public abstract void atualizar(Usuario usuario);

	public abstract void deletar(Usuario usuario);

	public abstract Usuario consultar(Usuario usuario);

}
