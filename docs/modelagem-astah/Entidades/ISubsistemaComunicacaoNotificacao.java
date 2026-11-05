package Entidades;

import types.String;
import Entidades.Ocorrencia.Ocorrencia;
import Entidades.Util.StatusOcorrencia;
import Entidades.Usuario.Usuario;

public interface ISubsistemaComunicacaoNotificacao {

	public abstract boolean verificaDisponibilidade();

	public abstract void notificaAnalistas(String numeroProtocolo, String animal, String linkOcorrencia);

	public abstract void notificarUsuario(Ocorrencia ocorrencia, StatusOcorrencia novoStatus, Usuario usuario);

}
