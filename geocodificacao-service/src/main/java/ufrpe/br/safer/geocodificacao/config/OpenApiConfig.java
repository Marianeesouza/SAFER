package ufrpe.br.safer.geocodificacao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI openApi() {
		return new OpenAPI().info(new Info()
				.title("SAFER - Serviço de Geocodificação")
				.description("Conversão entre endereços e coordenadas geográficas, com cache próprio.")
				.version("v1"));
	}

}
