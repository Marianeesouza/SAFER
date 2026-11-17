package ufrpe.br.safer.usuarios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI usuariosOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SAFER - Serviço de Usuários e Autenticação")
                        .description("API responsável pelo cadastro de usuários, autenticação JWT, consulta de perfil e gerenciamento de cargos.")
                        .version("v1"));
    }
}
