package Entidades.SubsistemaComunicacaoEmail;

import Entidades.ISubsistemaComunicacaoNotificacao;
import types.String;
import Entidades.Ocorrencia.Ocorrencia;
import Entidades.Util.StatusOcorrencia;
import Entidades.Usuario.Usuario;

public class FachadaComunicacaoEmail implements ISubsistemaComunicacaoNotificacao {

	private static SubsistemaComunicacaoEmail.FachadaComunicacaoEmail instanciaFachadaComunicacaoEmail;

	public static SubsistemaComunicacaoEmail.FachadaComunicacaoEmail getInstanciaFachadaComunicacaoEmail() {
		return null;
	}

	public boolean verificaDisponibilidade() {
		return false;
	}

	public void notificaAnalistas(String numeroProtocolo, String animal, String linkOcorrencia) {

	}

	public void notificarUsuario(Ocorrencia ocorrencia, StatusOcorrencia novoStatus, Usuario usuario) {

	}

}
