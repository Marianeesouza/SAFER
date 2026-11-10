package ufrpe.br.safer.geocodificacao.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class NominatimConfig {

	@Bean
	RestClient nominatimHttp(@Value("${safer.nominatim.url}") String url,
			@Value("${safer.nominatim.user-agent}") String userAgent) {
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
		fabrica.setReadTimeout(Duration.ofSeconds(8));
		// A política de uso do Nominatim exige um User-Agent que identifique a aplicação.
		return RestClient.builder()
				.baseUrl(url)
				.defaultHeader(HttpHeaders.USER_AGENT, userAgent)
				.requestFactory(fabrica)
				.build();
	}

}
