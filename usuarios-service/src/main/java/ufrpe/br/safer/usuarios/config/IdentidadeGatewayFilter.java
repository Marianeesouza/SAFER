package ufrpe.br.safer.usuarios.config;

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
import java.util.List;

/**
 * Monta a autenticação a partir da identidade que o API Gateway já verificou.
 *
 * <p>Este serviço emite o token JWT no login, mas não o valida: o gateway valida o token, descarta qualquer
 * {@code X-User-*} enviado pelo cliente e define os cabeçalhos abaixo. Isso só é seguro porque o serviço não
 * publica porta, de modo que todo acesso externo passa pelo gateway.</p>
 */
@Component
public class IdentidadeGatewayFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_CARGO = "X-User-Cargo";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String email = request.getHeader(HEADER_USER_EMAIL);
        String cargo = request.getHeader(HEADER_USER_CARGO);

        if (email != null && !email.isBlank() && cargo != null && !cargo.isBlank()
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + cargo.trim().toUpperCase()))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
