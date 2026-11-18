package ufrpe.br.safer.ocorrencias.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ufrpe.br.safer.ocorrencias.client.dto.PermissaoResponseDTO;
import ufrpe.br.safer.ocorrencias.client.dto.TipoAcaoOcorrencia;
import ufrpe.br.safer.ocorrencias.client.dto.ValidarPermissaoDTO;
import ufrpe.br.safer.ocorrencias.exception.AcessoNegadoException;
import ufrpe.br.safer.ocorrencias.exception.ServicoIndisponivelException;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
@Slf4j
public class UsuariosClient {

    private final RestClient restClient;

    public UsuariosClient(
            @Value("${safer.services.usuarios-url:http://usuarios-service}") String baseUrl,
            RestClient.Builder builder
    ) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
        );
        factory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public void validarPermissaoOuFalhar(
            Long usuarioId,
            TipoAcaoOcorrencia acao,
            Long criadorId,
            Long analistaId,
            String tokenBearer
    ) {
        ValidarPermissaoDTO dto = new ValidarPermissaoDTO(acao, criadorId, analistaId);

        try {
            PermissaoResponseDTO resposta = restClient.post()
                    .uri("/usuarios/{id}/permissoes/validar", usuarioId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenBearer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .body(PermissaoResponseDTO.class);

            if (resposta == null || !resposta.permitido()) {
                String motivo = (resposta != null && resposta.motivoRecusa() != null)
                        ? resposta.motivoRecusa()
                        : "Permissão negada pelo serviço de controle de acesso.";
                log.warn("Ação {} negada para o usuário {}: {}", acao, usuarioId, motivo);
                throw new AcessoNegadoException(motivo);
            }
        } catch (AcessoNegadoException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Falha ao comunicar com usuarios-service para validação de permissão: {}", e.getMessage());
            // Falhar fechado (fail-closed)
            throw new ServicoIndisponivelException(
                    "Serviço de validação de usuários temporariamente indisponível. Operação abortada por segurança."
            );
        }
    }
}
