package ufrpe.br.safer.geocodificacao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ufrpe.br.safer.geocodificacao.adapter.ProvedorIndisponivelException;
import ufrpe.br.safer.geocodificacao.model.Coordinates;
import ufrpe.br.safer.geocodificacao.model.Location;
import ufrpe.br.safer.geocodificacao.service.GeocodificacaoService;

class GeocodificacaoControllerTest {

	private final GeocodificacaoService service = mock(GeocodificacaoService.class);
	private MockMvc mvc;

	@BeforeEach
	void preparar() {
		mvc = MockMvcBuilders.standaloneSetup(new GeocodificacaoController(service))
				.setControllerAdvice(new ErrorHandler())
				.build();
	}

	@Test
	void coordenadasDeEnderecoEncontrado() throws Exception {
		when(service.obterCoordenadas("UFRPE"))
				.thenReturn(Optional.of(new Location(new Coordinates(-8.0176, -34.9495), "UFRPE, Recife")));

		mvc.perform(get("/geocoding/coordinates").param("address", "UFRPE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.coordinates.latitude").value(-8.0176))
				.andExpect(jsonPath("$.address").value("UFRPE, Recife"));
	}

	@Test
	void enderecoNaoEncontradoRetorna404() throws Exception {
		when(service.obterCoordenadas(any())).thenReturn(Optional.empty());

		mvc.perform(get("/geocoding/coordinates").param("address", "inexistente"))
				.andExpect(status().isNotFound());
	}

	@Test
	void latitudeForaDaFaixaRetorna400() throws Exception {
		mvc.perform(get("/geocoding/address").param("latitude", "91").param("longitude", "0"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void parametroAusenteRetorna400() throws Exception {
		mvc.perform(get("/geocoding/coordinates")).andExpect(status().isBadRequest());
	}

	@Test
	void provedorIndisponivelRetorna503() throws Exception {
		when(service.obterEndereco(any())).thenThrow(new ProvedorIndisponivelException(new RuntimeException()));

		mvc.perform(get("/geocoding/address").param("latitude", "-8").param("longitude", "-34"))
				.andExpect(status().isServiceUnavailable());
	}

}
