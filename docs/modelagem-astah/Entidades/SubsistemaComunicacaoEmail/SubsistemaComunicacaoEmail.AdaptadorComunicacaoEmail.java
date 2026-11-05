package Entidades.SubsistemaComunicacaoEmail;

import types.String;

public class AdaptadorComunicacaoEmail implements SubsistemaComunicacaoEmail.IAdaptadorComunicacaoEmail {

	private SubsistemaComunicacaoEmail.ServicoEmailSDK servicoEmailSDK;

	public abstract boolean verificaDisponibilidade();

	public abstract boolean enviar(String destinatario, String mensagem);

}
