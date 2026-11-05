package Entidades.Usuario;

import Entidades.Ocorrencia.Ocorrencia;

public interface CargoUsuario {

	private Usuario usuario;

	public CargoUsuario CargoUsuario(Usuario usuario);

	public abstract boolean podeAssumirOcorrencia(Ocorrencia ocorrencia);

	public abstract boolean podeAtualizarOcorrencia(Usuario usuario, Ocorrencia ocorrencia);

	public abstract boolean podeCancelarOcorrencia(Usuario usuario, Ocorrencia ocorrencia);

	public abstract boolean notificarNovaOcorrencia();

}
