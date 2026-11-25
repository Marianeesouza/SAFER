package ufrpe.br.safer.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class AutenticacaoJwtFilterTest {

    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-32-caracteres!!";
    private static final SecretKey CHAVE = Keys.hmacShaKeyFor(SEGREDO.getBytes(StandardCharsets.UTF_8));

    private final AutenticacaoJwtFilter filtro = new AutenticacaoJwtFilter(SEGREDO);

    private String token(long validadeMs, SecretKey chave) {
        Date agora = new Date();
        return Jwts.builder()
                .subject("analista@ufrpe.br")
                .claim("userId", 7L)
                .claim("nome", "João Ávila")
                .claim("cargo", "ANALISTA")
                .issuedAt(agora)
                .expiration(new Date(agora.getTime() + validadeMs))
                .signWith(chave)
                .compact();
    }

    private MockHttpServletRequest requisicao(String metodo, String caminho, String bearer) {
        MockHttpServletRequest req = new MockHttpServletRequest(metodo, caminho);
        if (bearer != null) {
            req.addHeader("Authorization", "Bearer " + bearer);
        }
        return req;
    }

    private HttpServletRequest executar(MockHttpServletRequest req, MockHttpServletResponse res) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filtro.doFilter(req, res, chain);
        return (HttpServletRequest) chain.getRequest();
    }

    @Test
    @DisplayName("Token válido em rota protegida repassa a identidade e remove o token")
    void tokenValidoRepassaIdentidade() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        HttpServletRequest repassada = executar(requisicao("PATCH", "/api/ocorrencias/1/status", token(60_000, CHAVE)), res);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(repassada).isNotNull();
        assertThat(repassada.getHeader("X-User-Id")).isEqualTo("7");
        assertThat(repassada.getHeader("X-User-Email")).isEqualTo("analista@ufrpe.br");
        assertThat(repassada.getHeader("X-User-Cargo")).isEqualTo("ANALISTA");
        assertThat(java.net.URLDecoder.decode(repassada.getHeader("X-User-Nome"), StandardCharsets.UTF_8)).isEqualTo("João Ávila");
        assertThat(repassada.getHeader("Authorization")).isNull();
    }

    @Test
    @DisplayName("Rota protegida sem token retorna 401 e não chega ao serviço")
    void semTokenEmRotaProtegidaRetorna401() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        HttpServletRequest repassada = executar(requisicao("GET", "/api/ocorrencias/em-aberto", null), res);

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(repassada).isNull();
        assertThat(res.getContentAsString()).contains("\"status\":401");
    }

    @Test
    @DisplayName("Token expirado ou com assinatura inválida retorna 401 em rota protegida")
    void tokenInvalidoRetorna401() throws Exception {
        SecretKey outra = Keys.hmacShaKeyFor("outra-chave-com-pelo-menos-32-caracteres-aqui".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse expirado = new MockHttpServletResponse();
        executar(requisicao("GET", "/api/usuarios/me", token(-1_000, CHAVE)), expirado);
        MockHttpServletResponse forjado = new MockHttpServletResponse();
        executar(requisicao("GET", "/api/usuarios/me", token(60_000, outra)), forjado);

        assertThat(expirado.getStatus()).isEqualTo(401);
        assertThat(forjado.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Cabeçalhos X-User-* enviados pelo cliente são descartados (sem forjar identidade)")
    void cabecalhosForjadosSaoDescartados() throws Exception {
        MockHttpServletRequest req = requisicao("POST", "/api/ocorrencias", null);
        req.addHeader("X-User-Id", "1");
        req.addHeader("X-User-Cargo", "ADMINISTRADOR");

        MockHttpServletResponse res = new MockHttpServletResponse();
        HttpServletRequest repassada = executar(req, res);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(repassada.getHeader("X-User-Id")).isNull();
        assertThat(repassada.getHeader("X-User-Cargo")).isNull();
        assertThat(java.util.Collections.list(repassada.getHeaderNames())).noneMatch(n -> n.toLowerCase().startsWith("x-user-"));
    }

    @Test
    @DisplayName("Identidade vinda do token substitui a forjada pelo cliente")
    void identidadeDoTokenSubstituiAForjada() throws Exception {
        MockHttpServletRequest req = requisicao("PATCH", "/api/ocorrencias/1/cancelar", token(60_000, CHAVE));
        req.addHeader("X-User-Cargo", "ADMINISTRADOR");

        HttpServletRequest repassada = executar(req, new MockHttpServletResponse());

        assertThat(repassada.getHeader("X-User-Cargo")).isEqualTo("ANALISTA");
        assertThat(java.util.Collections.list(repassada.getHeaders("X-User-Cargo"))).containsExactly("ANALISTA");
    }

    @Test
    @DisplayName("Rotas públicas passam sem token")
    void rotasPublicasPassamSemToken() throws Exception {
        String[][] publicas = {
                {"POST", "/api/auth/login"}, {"POST", "/api/usuarios"}, {"POST", "/api/ocorrencias"},
                {"GET", "/api/ocorrencias/protocolo/OC-1"}, {"GET", "/api/geocoding/campus/validate"},
                {"OPTIONS", "/api/ocorrencias/1"},
        };
        for (String[] rota : publicas) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            HttpServletRequest repassada = executar(requisicao(rota[0], rota[1], null), res);
            assertThat(res.getStatus()).as(rota[0] + " " + rota[1]).isEqualTo(200);
            assertThat(repassada).as(rota[0] + " " + rota[1]).isNotNull();
        }
    }

    @Test
    @DisplayName("Token inválido em rota pública é ignorado: segue como visitante")
    void tokenInvalidoEmRotaPublicaSegueComoVisitante() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        HttpServletRequest repassada = executar(requisicao("POST", "/api/ocorrencias", token(-1_000, CHAVE)), res);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(repassada.getHeader("X-User-Id")).isNull();
        assertThat(repassada.getHeader("Authorization")).isNull();
    }

    @Test
    @DisplayName("Rotas que parecem públicas mas usam outro método continuam protegidas")
    void metodoDiferenteContinuaProtegido() {
        assertThat(AutenticacaoJwtFilter.ehRotaPublica("GET", "/api/ocorrencias")).isFalse();
        assertThat(AutenticacaoJwtFilter.ehRotaPublica("GET", "/api/usuarios")).isFalse();
        assertThat(AutenticacaoJwtFilter.ehRotaPublica("DELETE", "/api/usuarios")).isFalse();
        assertThat(AutenticacaoJwtFilter.ehRotaPublica("GET", "/api/ocorrencias/em-aberto")).isFalse();
    }
}
