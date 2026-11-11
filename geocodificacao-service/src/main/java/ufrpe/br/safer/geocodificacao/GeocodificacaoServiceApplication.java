package ufrpe.br.safer.geocodificacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GeocodificacaoServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GeocodificacaoServiceApplication.class, args);
	}

}
