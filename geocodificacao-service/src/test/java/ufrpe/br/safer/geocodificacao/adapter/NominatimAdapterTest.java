package ufrpe.br.safer.geocodificacao.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import ufrpe.br.safer.geocodificacao.model.Coordenadas;
import ufrpe.br.safer.geocodificacao.model.Localizacao;

class NominatimAdapterTest {

	private MockRestServiceServer servidor;
	private NominatimAdapter adapter;

	@BeforeEach
	void preparar() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://nominatim.test");
		servidor = MockRestServiceServer.bindTo(builder).build();
		adapter = new NominatimAdapter(builder.build());
	}

	@Test
	void buscaPorEnderecoConverteLatLonETexto() {
		servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("http://nominatim.test/search")))
				.andRespond(withSuccess("""
						[{"place_id": 1, "lat": "-8.0176", "lon": "-34.9495", "display_name": "UFRPE, Recife"}]
						""", MediaType.APPLICATION_JSON));

		Optional<Localizacao> r = adapter.buscarPorEndereco("UFRPE");

		assertEquals(new Coordenadas(-8.0176, -34.9495), r.orElseThrow().coordenadas());
		assertEquals("UFRPE, Recife", r.get().endereco());
	}

	@Test
	void buscaReversaSemEnderecoRetornaVazio() {
		servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("http://nominatim.test/reverse")))
				.andRespond(withSuccess("{\"error\": \"Unable to geocode\"}", MediaType.APPLICATION_JSON));

		assertTrue(adapter.buscarPorCoordenadas(new Coordenadas(0, 0)).isEmpty());
	}

	@Test
	void erroDoProvedorViraProvedorIndisponivel() {
		servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("http://nominatim.test/search")))
				.andRespond(withServerError());

		assertThrows(ProvedorIndisponivelException.class, () -> adapter.buscarPorEndereco("x"));
	}

}
