package ufrpe.br.safer.ocorrencias.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtService {

    private final SecretKey secretKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public UsuarioAutenticado validarETrazerUsuario(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = claims.get("userId", Long.class);
            if (userId == null) {
                Object rawId = claims.get("userId");
                if (rawId instanceof Number num) {
                    userId = num.longValue();
                }
            }

            String email = claims.getSubject();
            String nome = claims.get("nome", String.class);
            String cargo = claims.get("cargo", String.class);

            if (userId == null || cargo == null) {
                log.warn("Token JWT sem claims obrigatórias (userId ou cargo).");
                return null;
            }

            return new UsuarioAutenticado(userId, email, nome, cargo);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Falha na validação do token JWT: {}", e.getMessage());
            return null;
        }
    }
}
