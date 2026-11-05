package Entidades.SubsistemaComunicacaoEmail;

import types.String;

public interface IAdaptadorComunicacaoEmail {

	public abstract boolean verificaDisponibilidade();

	public abstract boolean enviar(String destinatario, String mensagem);

}
