package ufrpe.br.safer.usuarios.model;

public enum Cargo {
    COMUM,
    ANALISTA,
    ADMINISTRADOR;

    /**
     * Valida a transição de cargo conforme RN04:
     * - COMUM -> pode ser promovido a ANALISTA
     * - ANALISTA -> pode ser promovido a ADMINISTRADOR ou rebaixado a COMUM
     * - ADMINISTRADOR -> não pode ser rebaixado
     */
    public boolean podeAlterarPara(Cargo novoCargo) {
        if (novoCargo == null || novoCargo == this) {
            return false;
        }
        return switch (this) {
            case COMUM -> novoCargo == ANALISTA;
            case ANALISTA -> novoCargo == ADMINISTRADOR || novoCargo == COMUM;
            case ADMINISTRADOR -> false;
        };
    }
}
