package ufrpe.br.safer.ocorrencias.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ufrpe.br.safer.ocorrencias.security.IdentidadeGatewayFilter;

import java.time.LocalDateTime;
import java.util.Map;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final IdentidadeGatewayFilter identidadeGatewayFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Registro de ocorrência público (visitante ou autenticado)
                        .requestMatchers(HttpMethod.POST, "/ocorrencias").permitAll()
                        // Consulta pública por protocolo (CDU06)
                        .requestMatchers(HttpMethod.GET, "/ocorrencias/protocolo/**").permitAll()
                        // Documentação e Actuator
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
                        // Triagem e listagem geral restritas a ANALISTA e ADMINISTRADOR
                        .requestMatchers(HttpMethod.GET, "/ocorrencias/em-aberto").hasAnyRole("ANALISTA", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/ocorrencias").hasAnyRole("ANALISTA", "ADMINISTRADOR")
                        // Tramitação
                        .requestMatchers(HttpMethod.POST, "/ocorrencias/*/vincular-analista").hasAnyRole("ANALISTA", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/ocorrencias/*/status").hasAnyRole("ANALISTA", "ADMINISTRADOR")
                        // Cancelamento e consultas individuais
                        .requestMatchers(HttpMethod.PATCH, "/ocorrencias/*/cancelar").authenticated()
                        .requestMatchers(HttpMethod.GET, "/ocorrencias/usuario/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/ocorrencias/*").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            Map<String, Object> body = Map.of(
                                    "status", HttpServletResponse.SC_UNAUTHORIZED,
                                    "erro", "Não autorizado",
                                    "mensagem", "Autenticação obrigatória para acessar este recurso.",
                                    "timestamp", LocalDateTime.now().toString()
                            );
                            response.getWriter().write(objectMapper.writeValueAsString(body));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            Map<String, Object> body = Map.of(
                                    "status", HttpServletResponse.SC_FORBIDDEN,
                                    "erro", "Acesso proibido",
                                    "mensagem", "Você não tem permissão para realizar esta ação.",
                                    "timestamp", LocalDateTime.now().toString()
                            );
                            response.getWriter().write(objectMapper.writeValueAsString(body));
                        })
                )
                .addFilterBefore(identidadeGatewayFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
