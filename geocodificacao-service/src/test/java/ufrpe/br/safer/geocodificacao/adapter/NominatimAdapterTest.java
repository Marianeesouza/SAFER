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

import ufrpe.br.safer.geocodificacao.config.CampusConfig;
import ufrpe.br.safer.geocodificacao.model.Coordinates;
import ufrpe.br.safer.geocodificacao.model.Location;

class NominatimAdapterTest {

	private MockRestServiceServer servidor;
	private NominatimAdapter adapter;

	@BeforeEach
	void preparar() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://nominatim.test");
		servidor = MockRestServiceServer.bindTo(builder).build();
		adapter = new NominatimAdapter(builder.build(), new CampusConfig(-8.017611, -34.949472, 1200));
	}

	@Test
	void buscaPorEnderecoLimitaAAreaDoCampus() {
		servidor.expect(requestTo(org.hamcrest.Matchers.allOf(
						org.hamcrest.Matchers.containsString("/search"),
						org.hamcrest.Matchers.containsString("bounded=1"),
						org.hamcrest.Matchers.containsString("viewbox="))))
				.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		assertTrue(adapter.buscarPorEndereco("Biblioteca Central").isEmpty());
		servidor.verify();
	}

	@Test
	void buscaPorEnderecoConverteLatLonETexto() {
		servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("http://nominatim.test/search")))
				.andRespond(withSuccess("""
						[{"place_id": 1, "lat": "-8.0176", "lon": "-34.9495", "display_name": "UFRPE, Recife"}]
						""", MediaType.APPLICATION_JSON));

		Optional<Location> r = adapter.buscarPorEndereco("UFRPE");

		assertEquals(new Coordinates(-8.0176, -34.9495), r.orElseThrow().coordinates());
		assertEquals("UFRPE, Recife", r.get().address());
	}

	@Test
	void buscaReversaSemEnderecoRetornaVazio() {
		servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("http://nominatim.test/reverse")))
				.andRespond(withSuccess("{\"error\": \"Unable to geocode\"}", MediaType.APPLICATION_JSON));

		assertTrue(adapter.buscarPorCoordenadas(new Coordinates(0, 0)).isEmpty());
	}

	@Test
	void respostaIncompletaViraProvedorIndisponivel() {
		servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("http://nominatim.test/search")))
				.andRespond(withSuccess("[{\"display_name\": \"Sem coordenadas\"}]", MediaType.APPLICATION_JSON));

		assertThrows(ProvedorIndisponivelException.class, () -> adapter.buscarPorEndereco("x"));
	}

	@Test
	void erroDoProvedorViraProvedorIndisponivel() {
		servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("http://nominatim.test/search")))
				.andRespond(withServerError());

		assertThrows(ProvedorIndisponivelException.class, () -> adapter.buscarPorEndereco("x"));
	}

}
