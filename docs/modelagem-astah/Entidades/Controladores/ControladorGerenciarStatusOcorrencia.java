package Entidades.Controladores;

import Entidades.Login.CadastroLogin;
import Entidades.Ocorrencia.CadastroOcorrencia;
import Entidades.HistoricoOcorrencia.CadastroHistoricoOcorrencia;
import AnalysisClasses.Bounderies.ComunicacaoServicoEmail;
import Entidades.ISubsistemaComunicacaoNotificacao;
import Entidades.Ocorrencia.Ocorrencia;
import Entidades.Usuario.Usuario;
import Entidades.Util.StatusOcorrencia;

public class ControladorGerenciarStatusOcorrencia {

	private CadastroLogin cadastroLogin;

	private CadastroOcorrencia cadastroOcorrencia;

	private CadastroHistoricoOcorrencia cadastroHistoricoOcorrencia;

	private ComunicacaoServicoEmail comunicacaoServicoEmail;

	private ISubsistemaComunicacaoNotificacao iSubsistemaComunicacaoNotificacao;

	public Ocorrencia[] listarOcorrenciasEmAberto() {
		return null;
	}

	public boolean vincularOcorrencia(Usuario usuario, Ocorrencia ocorrencia) {
		return false;
	}

	public boolean verificarPermissao(Ocorrencia ocorrencia, Usuario usuario) {
		return false;
	}

	public Ocorrencia atualizarStatusOcorrencia(Ocorrencia ocorrencia, Usuario usuario, StatusOcorrencia statusOcorrencia) {
		return null;
	}

}
