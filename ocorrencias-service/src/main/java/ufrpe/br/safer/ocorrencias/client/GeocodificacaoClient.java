package ufrpe.br.safer.ocorrencias.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ufrpe.br.safer.ocorrencias.dto.CampusValidationResponseDTO;

@Component
@Slf4j
public class GeocodificacaoClient {

    // Nome do serviço no Eureka (spring.application.name do geocodificacao-service).
    private static final String BASE_URL = "http://geocodificacao-service";

    private final RestClient restClient;

    public GeocodificacaoClient(@LoadBalanced RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
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
