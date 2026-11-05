package Entidades.Usuario;

import types.String;
import Entidades.Util.RelacaoUfrpe;
import Entidades.Ocorrencia.Ocorrencia;

public class Usuario {

	private long id;

	private String nome;

	private String telefoneFixo;

	private String celular;

	private String email;

	private RelacaoUfrpe relacaoUfrpe;

	public void mudarCargo(CargoUsuario cargo) {

	}

	public boolean podeCancelarOcorrencia(Ocorrencia ocorrencia) {
		return false;
	}

	public boolean podeAssumirOcorrencia(Ocorrencia ocorrencia) {
		return false;
	}

	public boolean podeAtualizarOcorrencia(Ocorrencia ocorrencia) {
		return false;
	}

	public boolean notificarNovaOcorrencia() {
		return false;
	}

}
