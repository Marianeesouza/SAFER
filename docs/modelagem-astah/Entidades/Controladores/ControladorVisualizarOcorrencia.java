package Entidades.Controladores;

import Entidades.Util.GeoConfigsDTO;
import Entidades.Login.CadastroLogin;
import Entidades.HistoricoOcorrencia.CadastroHistoricoOcorrencia;
import Entidades.Ocorrencia.CadastroOcorrencia;
import AnalysisClasses.Bounderies.ComunicacaoAPIGeocodificacao;
import Entidades.ISubsistemaComunicacaoAPIGeocodificacao;
import types.String;
import Entidades.Ocorrencia.Ocorrencia;

public class ControladorVisualizarOcorrencia {

	private static ControladorVisualizarOcorrencia instanciaControladorVisualizarOcorrencia;

	private CadastroLogin cadastroLogin;

	private CadastroHistoricoOcorrencia cadastroHistoricoOcorrencia;

	private CadastroOcorrencia cadastroOcorrencia;

	private ComunicacaoAPIGeocodificacao comunicacaoAPIGeocodificacao;

	private ISubsistemaComunicacaoAPIGeocodificacao iSubsistemaComunicacaoAPIGeocodificacao;

	public static ControladorVisualizarOcorrencia getInstanciaControladorVisualizarOcorrencia() {
		return null;
	}

	public Ocorrencia[] listarOcorrenciasUsuario(String tokenSessao) {
		return null;
	}

	public Ocorrencia[] obterHistoricoOcorrencia(long idOcorrencia) {
		return null;
	}

	public Ocorrencia buscarOcorrenciaPorProtocolo(String numeroProtocolo) {
		return null;
	}

	public Ocorrencia buscarOcorrenciaPorIdOcorrencia(long idCorrencia) {
		return null;
	}

	public GeoConfigsDTO obterConfiguracaoMapa() {
		return null;
	}

}
