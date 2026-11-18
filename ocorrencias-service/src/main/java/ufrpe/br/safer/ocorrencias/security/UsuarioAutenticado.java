package ufrpe.br.safer.ocorrencias.security;

public record UsuarioAutenticado(
        Long id,
        String email,
        String nome,
        String cargo
) {
    public boolean ehAdministrador() {
        return "ADMINISTRADOR".equalsIgnoreCase(cargo);
    }

    public boolean ehAnalista() {
        return "ANALISTA".equalsIgnoreCase(cargo);
    }

    public boolean ehComum() {
        return "COMUM".equalsIgnoreCase(cargo);
    }
}
