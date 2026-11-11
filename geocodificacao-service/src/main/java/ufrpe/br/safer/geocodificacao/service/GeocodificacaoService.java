package ufrpe.br.safer.geocodificacao.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import ufrpe.br.safer.geocodificacao.adapter.ProvedorGeocodificacao;
import ufrpe.br.safer.geocodificacao.model.CacheGeocodificacao;
import ufrpe.br.safer.geocodificacao.model.Coordenadas;
import ufrpe.br.safer.geocodificacao.model.Localizacao;
import ufrpe.br.safer.geocodificacao.repository.CacheGeocodificacaoRepository;

@Service
public class GeocodificacaoService {

	private final ProvedorGeocodificacao provedor;
	private final CacheGeocodificacaoRepository cache;

	public GeocodificacaoService(ProvedorGeocodificacao provedor, CacheGeocodificacaoRepository cache) {
		this.provedor = provedor;
		this.cache = cache;
	}

	public Optional<Localizacao> obterCoordenadas(String endereco) {
		String chave = "endereco:" + endereco.strip().replaceAll("\s+", " ").toLowerCase(Locale.ROOT);
		Optional<CacheGeocodificacao> emCache = cache.findByChave(chave);
		if (emCache.isPresent()) {
			return emCache.map(c -> new Localizacao(c.getCoordenadas(), c.getEndereco()));
		}
		Optional<Localizacao> encontrada = provedor.buscarPorEndereco(endereco);
		encontrada.ifPresent(l -> guardar(chave, l));
		return encontrada;
	}

	public Optional<Localizacao> obterEndereco(Coordenadas coordenadas) {
		String chave = String.format(Locale.ROOT, "coordenadas:%.5f,%.5f",
				coordenadas.latitude(), coordenadas.longitude());
		Optional<CacheGeocodificacao> emCache = cache.findByChave(chave);
		if (emCache.isPresent()) {
			return Optional.of(new Localizacao(coordenadas, emCache.get().getEndereco()));
		}
		Optional<Localizacao> encontrada = provedor.buscarPorCoordenadas(coordenadas);
		encontrada.ifPresent(l -> guardar(chave, l));
		return encontrada;
	}

	private void guardar(String chave, Localizacao localizacao) {
		try {
			cache.save(new CacheGeocodificacao(chave, localizacao.coordenadas(), localizacao.endereco()));
		} catch (DataIntegrityViolationException e) {
			// Outra requisição gravou a mesma consulta ao mesmo tempo; o resultado continua válido.
		}
	}

}
