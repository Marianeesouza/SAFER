package Entidades.SubsistemaComunicacaoAPIGeocodificacao;

import types.String;
import Entidades.Ocorrencia.Coordenadas;

public interface IAdaptadorServicoGeocodificacao {

	public abstract String obterEnderecoPorCoordenadas(Coordenadas coordenadas);

}
