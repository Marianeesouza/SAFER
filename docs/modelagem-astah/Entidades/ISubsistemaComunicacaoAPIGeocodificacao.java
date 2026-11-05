package Entidades;

import Entidades.Util.GeoConfigsDTO;
import types.String;
import Entidades.Ocorrencia.Coordenadas;

public interface ISubsistemaComunicacaoAPIGeocodificacao {

	public abstract GeoConfigsDTO obterConfiguracaoMapa();

	public abstract String obterEnderecoPorCoordenadas(Coordenadas coordenadas);

}
