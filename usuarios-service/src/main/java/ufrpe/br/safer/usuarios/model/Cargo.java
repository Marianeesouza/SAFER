package ufrpe.br.safer.usuarios.model;

public enum Cargo {
    COMUM,
    ANALISTA,
    ADMINISTRADOR;

    public boolean ehAdministrador() {
        return this == ADMINISTRADOR;
    }

    public boolean ehAnalista() {
        return this == ANALISTA;
    }

    public boolean ehComum() {
        return this == COMUM;
    }

    public boolean notificarNovaOcorrencia() {
        return this == ANALISTA || this == ADMINISTRADOR;
    }
}
