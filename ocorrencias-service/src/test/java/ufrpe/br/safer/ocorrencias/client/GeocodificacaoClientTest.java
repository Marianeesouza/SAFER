package ufrpe.br.safer.ocorrencias.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeocodificacaoClientTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
    private final GeocodificacaoClient client = new GeocodificacaoClient(builder);

    @Test
    @DisplayName("Chama o servico pelo nome do Eureka e devolve o resultado da validacao")
    void chamaOServicoPeloNome() {
        servidor.expect(requestTo("http://geocodificacao-service/geocoding/campus/validate?latitude=-8.0163&longitude=-34.9501"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"insideCampus\":false}", MediaType.APPLICATION_JSON));

        assertThat(client.validarPontoNoCampus(-8.0163, -34.9501)).isFalse();

        servidor.verify();
    }

    @Test
    @DisplayName("Se a geocodificacao falhar, segue sem a checagem do campus")
    void falhaDoServicoNaoBloqueiaORegistro() {
        servidor.expect(requestTo("http://geocodificacao-service/geocoding/campus/validate?latitude=-8.0&longitude=-34.0"))
                .andRespond(withServerError());

        assertThat(client.validarPontoNoCampus(-8.0, -34.0)).isTrue();
    }
}
