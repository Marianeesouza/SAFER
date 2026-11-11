package ufrpe.br.safer.ocorrencias.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ufrpe.br.safer.ocorrencias.dto.CampusValidationResponseDTO;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
@Slf4j
public class GeocodificacaoClient {

    private final RestClient restClient;

    public GeocodificacaoClient(@Value("${safer.services.geocodificacao-url:http://localhost:8083}") String baseUrl) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fabrica)
                .build();
    }

    public boolean validarPontoNoCampus(Double latitude, Double longitude) {
        try {
            CampusValidationResponseDTO response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/geocoding/campus/validate")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .build())
                    .retrieve()
                    .body(CampusValidationResponseDTO.class);

            return response != null && response.insideCampus();
        } catch (RestClientException e) {
            log.warn("Geocodificação indisponível ({}): coordenadas não validadas, registro segue sem a checagem do campus.", e.getMessage());
            return true;
        }
    }
}
