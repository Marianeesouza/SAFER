package Entidades.Controladores;

import Entidades.Util.GeoConfigsDTO;
import Entidades.Login.CadastroLogin;
import Entidades.Ocorrencia.CadastroOcorrencia;
import AnalysisClasses.Bounderies.ComunicacaoAPIGeocodificacao;
import AnalysisClasses.Bounderies.ComunicacaoServicoEmail;
import Entidades.ISubsistemaComunicacaoAPIGeocodificacao;
import Entidades.ISubsistemaComunicacaoNotificacao;
import Entidades.HistoricoOcorrencia.CadastroHistoricoOcorrencia;
import types.String;
import Entidades.Usuario.Usuario;
import Entidades.Ocorrencia.DadosComunicante;
import Entidades.Ocorrencia.DadosLocalizacao;
import types.Foto;
import Entidades.Ocorrencia.Coordenadas;

public class ControladorCadastroOcorrencia {

	private static ControladorCadastroOcorrencia instanciaControladorCadastroOcorrencia;

	private CadastroLogin cadastroLogin;

	private CadastroOcorrencia cadastroOcorrencia;

	private ComunicacaoAPIGeocodificacao comunicacaoAPIGeocodificacao;

	private ComunicacaoServicoEmail comunicacaoServicoEmail;

	private ComunicacaoAPIGeocodificacao comunicacaoAPIGeocodificacao;

	private ISubsistemaComunicacaoAPIGeocodificacao iSubsistemaComunicacaoAPIGeocodificacao;

	private ISubsistemaComunicacaoNotificacao[] iSubsistemaComunicacaoNotificacao;

	private CadastroHistoricoOcorrencia cadastroHistoricoOcorrencia;

	public static ControladorCadastroOcorrencia getInstanciaControladorCadastroOcorrencia() {
		return null;
	}

	public GeoConfigsDTO obterConfiguracaoMapa() {
		return null;
	}

	public Usuario getInformacoesUsuario(String tokenSessao) {
		return null;
	}

	public String cadastrarOcorrencia(Usuario usuario, DadosComunicante dadosComunicante, DadosLocalizacao dadosLocalizacao, String animal, Foto[] fotos) {
		return null;
	}

	public boolean verificarDadosFormulario(Usuario usuario, DadosLocalizacao dadosLocalizacao, String animal, Foto[] fotos) {
		return false;
	}

	public String obterEnderecoPorCoordenadas(Coordenadas coordenadas) {
		return null;
	}

}
