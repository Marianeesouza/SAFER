package Componentes;

import Entidades.Usuario.Usuario;

public interface IServiceUsuarios {

	public abstract Usuario autenticarUsuario();

	public abstract Usuario cadastrarUsuario();

	public abstract Usuario buscarPorId();

}
