package ufrpe.br.safer.geocodificacao.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import ufrpe.br.safer.geocodificacao.model.Coordinates;
import ufrpe.br.safer.geocodificacao.model.Location;

@Component
public class NominatimAdapter implements ProvedorGeocodificacao {

	record Lugar(String lat, String lon, String display_name) {
	}

	private final RestClient http;

	public NominatimAdapter(RestClient nominatimHttp) {
		this.http = nominatimHttp;
	}

	@Override
	public Optional<Location> buscarPorEndereco(String endereco) {
		try {
			List<Lugar> lugares = http.get()
					.uri(u -> u.path("/search")
							.queryParam("q", endereco)
							.queryParam("format", "jsonv2")
							.queryParam("accept-language", "pt-BR")
							.queryParam("limit", 1)
							.build())
					.retrieve()
					.body(new ParameterizedTypeReference<List<Lugar>>() { });
			if (lugares == null || lugares.isEmpty()) {
				return Optional.empty();
			}
			Lugar lugar = lugares.get(0);
			if (lugar.lat() == null || lugar.lon() == null || lugar.display_name() == null) {
				throw new ProvedorIndisponivelException(new IllegalStateException("Incomplete Nominatim response"));
			}
			// Número malformado em lat/lon: resposta inválida do provedor, tratada como indisponibilidade.
			Coordinates coordenadas = new Coordinates(Double.parseDouble(lugar.lat()), Double.parseDouble(lugar.lon()));
			return Optional.of(new Location(coordenadas, lugar.display_name()));
		} catch (RestClientException | IllegalArgumentException e) {
			throw new ProvedorIndisponivelException(e);
		}
	}

	@Override
	public Optional<Location> buscarPorCoordenadas(Coordinates coordenadas) {
		try {
			Lugar lugar = http.get()
					.uri(u -> u.path("/reverse")
							.queryParam("lat", coordenadas.latitude())
							.queryParam("lon", coordenadas.longitude())
							.queryParam("format", "jsonv2")
							.queryParam("accept-language", "pt-BR")
							.build())
					.retrieve()
					.body(Lugar.class);
			// Sem endereço para o ponto, o Nominatim responde 200 com {"error": "..."}.
			if (lugar == null || lugar.display_name() == null) {
				return Optional.empty();
			}
			return Optional.of(new Location(coordenadas, lugar.display_name()));
		} catch (RestClientException e) {
			throw new ProvedorIndisponivelException(e);
		}
	}

}
