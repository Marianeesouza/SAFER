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
import ufrpe.br.safer.geocodificacao.config.CampusConfig;
import ufrpe.br.safer.geocodificacao.config.MapConfig;
import ufrpe.br.safer.geocodificacao.model.Coordinates;
import ufrpe.br.safer.geocodificacao.model.Location;
import ufrpe.br.safer.geocodificacao.service.GeocodificacaoService;

class GeocodificacaoControllerTest {

	private final GeocodificacaoService service = mock(GeocodificacaoService.class);
	private MockMvc mvc;

	@BeforeEach
	void preparar() {
		GeocodificacaoController controller = new GeocodificacaoController(service,
				new MapConfig("http://tiles/{z}/{x}/{y}.png", "© Test", 18),
				new CampusConfig(-8.017611, -34.949472, 1200));
		mvc = MockMvcBuilders.standaloneSetup(controller)
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
	void latitudeNaNRetorna400() throws Exception {
		mvc.perform(get("/geocoding/address").param("latitude", "NaN").param("longitude", "0"))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/geocoding/campus/validate").param("latitude", "NaN").param("longitude", "0"))
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

	@Test
	void returnsConfiguredMapSettings() throws Exception {
		mvc.perform(get("/geocoding/map/config"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.urlTemplate").value("http://tiles/{z}/{x}/{y}.png"))
				.andExpect(jsonPath("$.attribution").value("© Test"))
				.andExpect(jsonPath("$.maxZoom").value(18));
	}

	@Test
	void campusCheckAnswersInsideAndOutside() throws Exception {
		mvc.perform(get("/geocoding/campus/validate").param("latitude", "-8.017611").param("longitude", "-34.949472"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.insideCampus").value(true));
		mvc.perform(get("/geocoding/campus/validate").param("latitude", "-8.0").param("longitude", "-34.9"))
				.andExpect(jsonPath("$.insideCampus").value(false));
	}

	@Test
	void campusCheckWithInvalidLongitudeReturns400() throws Exception {
		mvc.perform(get("/geocoding/campus/validate").param("latitude", "0").param("longitude", "181"))
				.andExpect(status().isBadRequest());
	}

}
