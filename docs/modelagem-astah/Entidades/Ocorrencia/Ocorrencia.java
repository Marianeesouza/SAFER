package Entidades.Ocorrencia;

import types.String;
import types.DateTime;
import Entidades.HistoricoOcorrencia.HistoricoOcorrencia;
import Entidades.Util.StatusOcorrencia;
import Entidades.Usuario.Usuario;

public class Ocorrencia {

	private long id;

	private String numeroProtocolo;

	private DateTime dataHora;

	private HistoricoOcorrencia[] historicoOcorrencia;

	private StatusOcorrencia statusOcorrencia;

	private Usuario usuario;

	public String getNumeroProtocolo() {
		return null;
	}

	public StatusOcorrencia getStatus() {
		return null;
	}

	public void setStatus() {

	}

}
