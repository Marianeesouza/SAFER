package ufrpe.br.safer.ocorrencias.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ufrpe.br.safer.ocorrencias.dto.CampusValidationResponseDTO;

@Component
@Slf4j
public class GeocodificacaoClient {

    private final RestClient restClient;

    public GeocodificacaoClient(@Value("${safer.services.geocodificacao-url:http://localhost:8083}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
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
        } catch (Exception e) {
            log.warn("Não foi possível validar coordenadas junto ao geocodificacao-service: {}. Permitindo registro com aviso.", e.getMessage());
            return true;
        }
    }
}
