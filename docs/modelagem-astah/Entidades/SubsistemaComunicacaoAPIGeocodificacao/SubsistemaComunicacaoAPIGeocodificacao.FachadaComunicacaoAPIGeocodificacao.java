package Entidades.SubsistemaComunicacaoAPIGeocodificacao;

import Entidades.ISubsistemaComunicacaoAPIGeocodificacao;
import Entidades.Util.GeoConfigsDTO;
import types.String;
import Entidades.Ocorrencia.Coordenadas;

public class FachadaComunicacaoAPIGeocodificacao implements ISubsistemaComunicacaoAPIGeocodificacao {

	private static SubsistemaComunicacaoAPIGeocodificacao.FachadaComunicacaoAPIGeocodificacao instanciaFachadaComunicacaoAPIGeocodificacao;

	public static SubsistemaComunicacaoAPIGeocodificacao.FachadaComunicacaoAPIGeocodificacao getInstanciaFachadaComunicacaoAPIGeocodificacao() {
		return null;
	}

	public GeoConfigsDTO obterConfiguracaoMapa() {
		return null;
	}

	public String obterEnderecoPorCoordenadas(Coordenadas coordenadas) {
		return null;
	}

}
