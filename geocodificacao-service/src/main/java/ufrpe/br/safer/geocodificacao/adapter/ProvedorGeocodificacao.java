package ufrpe.br.safer.geocodificacao.adapter;

import java.util.Optional;

import ufrpe.br.safer.geocodificacao.model.Coordinates;
import ufrpe.br.safer.geocodificacao.model.Location;

public interface ProvedorGeocodificacao {

	Optional<Location> buscarPorEndereco(String endereco);

	Optional<Location> buscarPorCoordenadas(Coordinates coordenadas);

}
