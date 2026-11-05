package Entidades.HistoricoOcorrencia;

import types.DateTime;
import Entidades.Util.StatusOcorrencia;
import types.String;
import Entidades.Usuario.Usuario;

public class HistoricoOcorrencia {

	private long id;

	private DateTime dataHora;

	private StatusOcorrencia statusRegistrado;

	private String justificativa;

	private Usuario usuario;

}
