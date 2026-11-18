package ufrpe.br.safer.ocorrencias.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ufrpe.br.safer.ocorrencias.dto.DestinatarioDTO;

import java.util.List;

@Component
@Slf4j
public class UsuariosClient {

    // Nome do serviço no Eureka (spring.application.name do usuarios-service).
    private static final String BASE_URL = "http://usuarios-service";

    private final RestClient restClient;

    public UsuariosClient(@LoadBalanced RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
    }

    public List<DestinatarioDTO> listarDestinatariosDeNovasOcorrencias() {
        try {
            List<DestinatarioDTO> destinatarios = restClient.get()
                    .uri("/internal/usuarios/destinatarios-novas-ocorrencias")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<DestinatarioDTO>>() {});
            return destinatarios != null ? destinatarios : List.of();
        } catch (RestClientException e) {
            log.warn("Usuarios indisponível ({}): analistas não serão avisados desta ocorrência.", e.getMessage());
            return List.of();
        }
    }
}
