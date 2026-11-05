package Entidades.Controladores;

import Entidades.Ocorrencia.CadastroOcorrencia;
import Entidades.HistoricoOcorrencia.CadastroHistoricoOcorrencia;
import Entidades.Login.CadastroLogin;
import AnalysisClasses.Bounderies.ComunicacaoServicoEmail;
import Entidades.ISubsistemaComunicacaoNotificacao;
import types.String;
import Entidades.Ocorrencia.Ocorrencia;
import Entidades.Usuario.Usuario;

public class ControladorCancelarOcorrencia {

	private CadastroOcorrencia cadastroOcorrencia;

	private CadastroHistoricoOcorrencia cadastroHistoricoOcorrencia;

	private CadastroLogin cadastroLogin;

	private ComunicacaoServicoEmail comunicacaoServicoEmail;

	private ISubsistemaComunicacaoNotificacao iSubsistemaComunicacaoNotificacao;

	public boolean cancelarOcorrencia(long idOcorrencia, String justificativa, String tokenSessao) {
		return false;
	}

	public boolean verificarPermissao(Ocorrencia ocorrencia, Usuario usuario) {
		return false;
	}

	public boolean verificarStatusOcorrencia(Ocorrencia ocorrencia) {
		return false;
	}

}
