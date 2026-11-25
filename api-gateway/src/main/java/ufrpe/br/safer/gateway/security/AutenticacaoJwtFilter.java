package ufrpe.br.safer.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ponto único de autenticação do SAFER.
 *
 * <p>Valida o token JWT (assinatura e validade) antes de rotear a chamada. Se o token é válido, repassa
 * aos serviços a identidade já verificada em cabeçalhos {@code X-User-*}; o token em si não segue adiante.
 * Qualquer cabeçalho {@code X-User-*} enviado pelo cliente é descartado, para que ninguém forje identidade.
 * As regras por cargo continuam em cada serviço.</p>
 */
@Component
public class AutenticacaoJwtFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_NOME = "X-User-Nome";
    public static final String HEADER_USER_CARGO = "X-User-Cargo";

    private static final Logger log = LoggerFactory.getLogger(AutenticacaoJwtFilter.class);
    private static final String BEARER = "Bearer ";
    private static final String PREFIXO_API = "/api/";

    private final SecretKey chave;

    public AutenticacaoJwtFilter(@Value("${jwt.secret}") String secret) {
        this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Identidade identidade = extrairIdentidade(request.getHeader(HttpHeaders.AUTHORIZATION));

        String caminho = request.getRequestURI();
        if (caminho.startsWith(PREFIXO_API) && identidade == null && !ehRotaPublica(request.getMethod(), caminho)) {
            responderNaoAutorizado(response);
            return;
        }

        Map<String, String> adicionados = new LinkedHashMap<>();
        if (identidade != null) {
            adicionados.put(HEADER_USER_ID, String.valueOf(identidade.id()));
            adicionados.put(HEADER_USER_EMAIL, identidade.email());
            adicionados.put(HEADER_USER_NOME, URLEncoder.encode(identidade.nome(), StandardCharsets.UTF_8));
            adicionados.put(HEADER_USER_CARGO, identidade.cargo());
        }

        chain.doFilter(
                new RequisicaoComIdentidade(request, adicionados,
                        HttpHeaders.AUTHORIZATION, HEADER_USER_ID, HEADER_USER_EMAIL, HEADER_USER_NOME, HEADER_USER_CARGO),
                response);
    }

    /** Rotas que visitantes podem chamar sem token. Se um token vier junto e for válido, a identidade é repassada. */
    static boolean ehRotaPublica(String metodo, String caminho) {
        String c = caminho.length() > 1 && caminho.endsWith("/") ? caminho.substring(0, caminho.length() - 1) : caminho;
        if (HttpMethod.OPTIONS.matches(metodo)) {
            return true;
        }
        if (HttpMethod.POST.matches(metodo)) {
            return c.equals("/api/auth/login") || c.equals("/api/usuarios") || c.equals("/api/ocorrencias");
        }
        if (HttpMethod.GET.matches(metodo)) {
            return c.startsWith("/api/ocorrencias/protocolo/") || c.startsWith("/api/geocoding/");
        }
        return false;
    }

    private Identidade extrairIdentidade(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER)) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(authorization.substring(BEARER.length()).trim())
                    .getPayload();

            Object id = claims.get("userId");
            String cargo = claims.get("cargo", String.class);
            if (!(id instanceof Number numero) || cargo == null) {
                log.warn("Token JWT sem as claims obrigatórias (userId e cargo).");
                return null;
            }
            String nome = claims.get("nome", String.class);
            return new Identidade(numero.longValue(), claims.getSubject(), nome == null ? "" : nome, cargo);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT rejeitado: {}", e.getMessage());
            return null;
        }
    }

    private void responderNaoAutorizado(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":401,\"erro\":\"Não autorizado\","
                + "\"mensagem\":\"Autenticação obrigatória: token ausente, inválido ou expirado.\","
                + "\"timestamp\":\"" + LocalDateTime.now() + "\"}");
    }

    private record Identidade(long id, String email, String nome, String cargo) {
    }
}
