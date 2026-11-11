package ufrpe.br.safer.ocorrencias.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ocorrenciasOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SAFER — Serviço de Ocorrências")
                        .description("API REST para registro, tramitação, consulta e auditoria de ocorrências de fauna na UFRPE.")
                        .version("1.0.0")
                        .contact(new Contact().name("Equipe SAFER — APSOO").email("safer@ufrpe.br")));
    }
}
