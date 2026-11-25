package ufrpe.br.safer.gateway.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Requisição com cabeçalhos substituídos: remove os nomes informados (cabeçalhos que o cliente não pode
 * controlar) e acrescenta os que o gateway define.
 */
class RequisicaoComIdentidade extends HttpServletRequestWrapper {

    private final Map<String, String> adicionados = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> removidos = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    RequisicaoComIdentidade(HttpServletRequest request, Map<String, String> adicionados, String... removidos) {
        super(request);
        this.adicionados.putAll(adicionados);
        this.removidos.addAll(List.of(removidos));
    }

    @Override
    public String getHeader(String nome) {
        if (adicionados.containsKey(nome)) {
            return adicionados.get(nome);
        }
        return removidos.contains(nome) ? null : super.getHeader(nome);
    }

    @Override
    public Enumeration<String> getHeaders(String nome) {
        if (adicionados.containsKey(nome)) {
            return Collections.enumeration(List.of(adicionados.get(nome)));
        }
        return removidos.contains(nome) ? Collections.emptyEnumeration() : super.getHeaders(nome);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> nomes = new LinkedHashSet<>();
        for (String nome : Collections.list(super.getHeaderNames())) {
            if (!removidos.contains(nome)) {
                nomes.add(nome);
            }
        }
        nomes.addAll(adicionados.keySet());
        return Collections.enumeration(nomes);
    }
}
