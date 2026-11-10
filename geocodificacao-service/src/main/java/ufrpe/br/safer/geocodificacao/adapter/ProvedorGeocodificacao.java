package ufrpe.br.safer.geocodificacao.adapter;

import java.util.Optional;

import ufrpe.br.safer.geocodificacao.model.Coordenadas;
import ufrpe.br.safer.geocodificacao.model.Localizacao;

public interface ProvedorGeocodificacao {

	Optional<Localizacao> buscarPorEndereco(String endereco);

	Optional<Localizacao> buscarPorCoordenadas(Coordenadas coordenadas);

}
