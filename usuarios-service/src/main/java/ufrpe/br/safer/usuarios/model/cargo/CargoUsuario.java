package ufrpe.br.safer.usuarios.model.cargo;

import ufrpe.br.safer.usuarios.model.Cargo;

public interface CargoUsuario {

    Cargo getTipoCargo();

    boolean podeAssumirOcorrencia();

    boolean podeAtualizarOcorrencia(Long analistaResponsavelId);

    boolean podeCancelarOcorrencia(Long criadorOcorrenciaId);

    boolean notificarNovaOcorrencia();

    boolean podeMudarPara(Cargo novoCargo);
}
