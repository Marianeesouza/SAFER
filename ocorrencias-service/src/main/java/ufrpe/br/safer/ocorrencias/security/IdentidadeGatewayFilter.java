package ufrpe.br.safer.ocorrencias.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Monta a autenticação a partir da identidade que o API Gateway já verificou.
 *
 * <p>O token JWT é validado apenas no gateway, que descarta qualquer {@code X-User-*} enviado pelo cliente
 * e define os valores abaixo a partir do token. Este serviço não valida token nem conhece a chave de
 * assinatura. Isso só é seguro porque o serviço não publica porta: todo acesso externo passa pelo gateway.</p>
 */
@Component
public class IdentidadeGatewayFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_NOME = "X-User-Nome";
    public static final String HEADER_USER_CARGO = "X-User-Cargo";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        UsuarioAutenticado usuario = lerIdentidade(request);

        if (usuario != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    usuario,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + usuario.cargo().toUpperCase()))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private UsuarioAutenticado lerIdentidade(HttpServletRequest request) {
        String id = request.getHeader(HEADER_USER_ID);
        String cargo = request.getHeader(HEADER_USER_CARGO);
        if (id == null || cargo == null || cargo.isBlank()) {
            return null;
        }
        try {
            String nome = request.getHeader(HEADER_USER_NOME);
            return new UsuarioAutenticado(
                    Long.valueOf(id.trim()),
                    request.getHeader(HEADER_USER_EMAIL),
                    nome == null ? null : URLDecoder.decode(nome, StandardCharsets.UTF_8),
                    cargo.trim()
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
