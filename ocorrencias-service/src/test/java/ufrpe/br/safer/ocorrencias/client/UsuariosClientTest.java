package ufrpe.br.safer.ocorrencias.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ufrpe.br.safer.ocorrencias.dto.DestinatarioDTO;

class UsuariosClientTest {

    private static final String URL = "http://usuarios-service/internal/usuarios/destinatarios-novas-ocorrencias";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
    private final UsuariosClient client = new UsuariosClient(builder);

    @Test
    @DisplayName("Chama o usuarios-service pelo nome do Eureka e devolve os destinatarios")
    void devolveOsDestinatarios() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"id\":1,\"nome\":\"Admin\",\"email\":\"admin@ufrpe.br\"},"
                                + "{\"id\":2,\"nome\":\"Ana\",\"email\":\"ana@ufrpe.br\"}]",
                        MediaType.APPLICATION_JSON));

        List<DestinatarioDTO> destinatarios = client.listarDestinatariosDeNovasOcorrencias();

        assertThat(destinatarios).containsExactly(
                new DestinatarioDTO(1L, "Admin", "admin@ufrpe.br"),
                new DestinatarioDTO(2L, "Ana", "ana@ufrpe.br"));
        servidor.verify();
    }

    @Test
    @DisplayName("Se o usuarios-service falhar, devolve lista vazia em vez de quebrar o registro")
    void falhaDoServicoDevolveListaVazia() {
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        assertThat(client.listarDestinatariosDeNovasOcorrencias()).isEmpty();
    }
}
