package ufrpe.br.safer.geocodificacao.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ufrpe.br.safer.geocodificacao.model.CacheGeocodificacao;

public interface CacheGeocodificacaoRepository extends JpaRepository<CacheGeocodificacao, Long> {

	Optional<CacheGeocodificacao> findByChave(String chave);

}
