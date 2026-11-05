package Entidades.SubsistemaComunicacaoAPIGeocodificacao;

import types.String;
import Entidades.Ocorrencia.Coordenadas;

public class AdaptadorServicoGeocodificacao implements SubsistemaComunicacaoAPIGeocodificacao.IAdaptadorServicoGeocodificacao {

	private String urlBaseMapa;

	private SubsistemaComunicacaoAPIGeocodificacao.APIGeocodificacao aPIGeocodificacao;

	public String obterEnderecoPorCoordenadas(Coordenadas coordenadas) {
		return null;
	}

}
