package Componentes;

import Entidades.Ocorrencia.Coordenadas;
import types.String;

/**
 * +obterCoordenadas(endereco: String): Coordenadas
 * +obterEndereco(coordenadas: Coordenadas): Endereco
 * +validarLocalizacaoCampus(coordenadas: Coordenadas): boolean
 */
public interface IServiceGeocodificacao {

	public abstract Coordenadas obterCoordenadas();

	public abstract String obterEndereco();

}
