package Entidades;

import Entidades.Util.GeoConfigsDTO;
import types.String;
import Entidades.Ocorrencia.Coordenadas;

public class SubsistemaComunicacaoAPIGeocodificacao implements ISubsistemaComunicacaoAPIGeocodificacao {


	/**
	 * @see Entidades.ISubsistemaComunicacaoAPIGeocodificacao#obterConfiguracaoMapa()
	 *  
	 */
	public GeoConfigsDTO obterConfiguracaoMapa() {
		return null;
	}


	/**
	 * @see Entidades.ISubsistemaComunicacaoAPIGeocodificacao#obterEnderecoPorCoordenadas(Entidades.Ocorrencia.Coordenadas)
	 */
	public String obterEnderecoPorCoordenadas(Coordenadas coordenadas) {
		return null;
	}

}
