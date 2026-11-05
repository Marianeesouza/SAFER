package Entidades.HistoricoOcorrencia;

import Entidades.Ocorrencia.CadastroOcorrencia;
import Entidades.Ocorrencia.Ocorrencia;
import types.DateTime;
import Entidades.Util.StatusOcorrencia;
import Entidades.Usuario.Usuario;
import types.String;

public class CadastroHistoricoOcorrencia {

	private static CadastroOcorrencia instanciaCadastroHistorico;

	private IRepositorioHistoricoOcorrencia iRepositorioHistoricoOcorrencia;

	public static CadastroHistoricoOcorrencia getInstanciaCadastroHistorico() {
		return null;
	}

	public HistoricoOcorrencia criarHistorico(Ocorrencia ocorrencia, DateTime dataHora, StatusOcorrencia statusRegistrado, Usuario usuario) {
		return null;
	}

	public HistoricoOcorrencia criarHistoricoCancelamento(Ocorrencia ocorrencia, String justificativa, DateTime dataHora, StatusOcorrencia statusRegistrado, Usuario usuario) {
		return null;
	}

	public HistoricoOcorrencia[] buscarHistoricoPorOcorrencia(long idOcorrencia) {
		return null;
	}

}
