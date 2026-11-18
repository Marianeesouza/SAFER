package ufrpe.br.safer.usuarios.model;

// Aplicacao do padrao State
public enum Cargo {
    COMUM {
        @Override
        public boolean notificarNovaOcorrencia() {
            return false;
        }
    },
    ANALISTA {
        @Override
        public boolean notificarNovaOcorrencia() {
            return true;
        }
    },
    ADMINISTRADOR {
        @Override
        public boolean notificarNovaOcorrencia() {
            return true;
        }
    };

    public abstract boolean notificarNovaOcorrencia();

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
