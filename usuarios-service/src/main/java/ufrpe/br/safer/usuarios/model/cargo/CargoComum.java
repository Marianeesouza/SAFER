package ufrpe.br.safer.usuarios.model.cargo;

import lombok.RequiredArgsConstructor;
import ufrpe.br.safer.usuarios.model.Cargo;
import ufrpe.br.safer.usuarios.model.Usuario;

@RequiredArgsConstructor
public class CargoComum implements CargoUsuario {

    private final Usuario usuario;

    @Override
    public Cargo getTipoCargo() {
        return Cargo.COMUM;
    }

    @Override
    public boolean podeAssumirOcorrencia() {
        return false;
    }

    @Override
    public boolean podeAtualizarOcorrencia(Long analistaResponsavelId) {
        return false;
    }

    @Override
    public boolean podeCancelarOcorrencia(Long criadorOcorrenciaId) {
        if (criadorOcorrenciaId == null || usuario == null || usuario.getId() == null) {
            return false;
        }
        return usuario.getId().equals(criadorOcorrenciaId);
    }

    @Override
    public boolean notificarNovaOcorrencia() {
        return false;
    }

    @Override
    public boolean podeMudarPara(Cargo novoCargo) {
        return novoCargo == Cargo.ANALISTA;
    }
}
