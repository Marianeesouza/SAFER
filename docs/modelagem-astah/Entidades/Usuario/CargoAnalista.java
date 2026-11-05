package Entidades.Usuario;

import Entidades.Ocorrencia.Ocorrencia;

public class CargoAnalista implements CargoUsuario {


	/**
	 * @see Entidades.Usuario.CargoUsuario#CargoUsuario(Entidades.Usuario.Usuario)
	 */
	public CargoUsuario CargoUsuario(Usuario usuario) {
		return null;
	}


	/**
	 * @see Entidades.Usuario.CargoUsuario#podeAssumirOcorrencia(Entidades.Ocorrencia.Ocorrencia)
	 */
	public boolean podeAssumirOcorrencia(Ocorrencia ocorrencia) {
		return false;
	}


	/**
	 * @see Entidades.Usuario.CargoUsuario#podeAtualizarOcorrencia(Entidades.Usuario.Usuario, Entidades.Ocorrencia.Ocorrencia)
	 */
	public boolean podeAtualizarOcorrencia(Usuario usuario, Ocorrencia ocorrencia) {
		return false;
	}


	/**
	 * @see Entidades.Usuario.CargoUsuario#podeCancelarOcorrencia(Entidades.Usuario.Usuario, Entidades.Ocorrencia.Ocorrencia)
	 */
	public boolean podeCancelarOcorrencia(Usuario usuario, Ocorrencia ocorrencia) {
		return false;
	}


	/**
	 * @see Entidades.Usuario.CargoUsuario#notificarNovaOcorrencia()
	 */
	public boolean notificarNovaOcorrencia() {
		return false;
	}

}
