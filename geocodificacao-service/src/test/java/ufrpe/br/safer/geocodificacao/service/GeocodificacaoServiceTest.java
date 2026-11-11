package ufrpe.br.safer.geocodificacao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ufrpe.br.safer.geocodificacao.adapter.ProvedorGeocodificacao;
import ufrpe.br.safer.geocodificacao.adapter.ProvedorIndisponivelException;
import ufrpe.br.safer.geocodificacao.model.CacheGeocodificacao;
import ufrpe.br.safer.geocodificacao.model.Coordenadas;
import ufrpe.br.safer.geocodificacao.model.Localizacao;
import ufrpe.br.safer.geocodificacao.repository.CacheGeocodificacaoRepository;

class GeocodificacaoServiceTest {

	private final ProvedorGeocodificacao provedor = mock(ProvedorGeocodificacao.class);
	private final CacheGeocodificacaoRepository cache = mock(CacheGeocodificacaoRepository.class);
	private final GeocodificacaoService service = new GeocodificacaoService(provedor, cache);

	private final Coordenadas ufrpe = new Coordenadas(-8.017611, -34.949472);

	@Test
	void acertoNoCacheNaoConsultaOProvedor() {
		when(cache.findByChave("endereco:ufrpe recife"))
				.thenReturn(Optional.of(new CacheGeocodificacao("endereco:ufrpe recife", ufrpe, "UFRPE, Recife")));

		Optional<Localizacao> r = service.obterCoordenadas("  UFRPE   Recife ");

		assertEquals(ufrpe, r.orElseThrow().coordenadas());
		verifyNoInteractions(provedor);
	}

	@Test
	void faltaNoCacheConsultaOProvedorEGravaOResultado() {
		when(cache.findByChave("endereco:ufrpe")).thenReturn(Optional.empty());
		when(provedor.buscarPorEndereco("UFRPE")).thenReturn(Optional.of(new Localizacao(ufrpe, "UFRPE, Recife")));

		service.obterCoordenadas("UFRPE");

		ArgumentCaptor<CacheGeocodificacao> gravado = ArgumentCaptor.forClass(CacheGeocodificacao.class);
		verify(cache).save(gravado.capture());
		assertEquals("endereco:ufrpe", gravado.getValue().getChave());
		assertEquals("UFRPE, Recife", gravado.getValue().getEndereco());
	}

	@Test
	void resultadoVazioDoProvedorNaoEhGravado() {
		when(cache.findByChave(any())).thenReturn(Optional.empty());
		when(provedor.buscarPorCoordenadas(ufrpe)).thenReturn(Optional.empty());

		assertTrue(service.obterEndereco(ufrpe).isEmpty());
		verify(cache, never()).save(any());
	}

	@Test
	void enderecoPorCoordenadasUsaChaveArredondadaEDevolveAsCoordenadasPedidas() {
		when(cache.findByChave("coordenadas:-8.01761,-34.94947"))
				.thenReturn(Optional.of(new CacheGeocodificacao("x", ufrpe, "UFRPE, Recife")));

		Coordenadas parecida = new Coordenadas(-8.017612, -34.949471);
		Localizacao r = service.obterEndereco(parecida).orElseThrow();

		assertEquals("UFRPE, Recife", r.endereco());
		assertEquals(parecida, r.coordenadas());
	}

	@Test
	void provedorForaDoArSemCacheRepassaAExcecao() {
		when(cache.findByChave(any())).thenReturn(Optional.empty());
		when(provedor.buscarPorEndereco("UFRPE")).thenThrow(new ProvedorIndisponivelException(new RuntimeException()));

		assertThrows(ProvedorIndisponivelException.class, () -> service.obterCoordenadas("UFRPE"));
	}

}
