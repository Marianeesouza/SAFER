package Componentes;

import Entidades.Ocorrencia.Coordenadas;
import types.String;

/**
 * +consultarCoordenadas(endereco: String): String
 * +consultarEndereco(latitude: double, longitude: double): String
 */
public interface IAPIGeocodificacao {

	public abstract Coordenadas consultarCoordenadas();

	public abstract String consultarEndereco();

}
