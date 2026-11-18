package ufrpe.br.safer.usuarios.model.cargo;

import lombok.RequiredArgsConstructor;
import ufrpe.br.safer.usuarios.model.Cargo;
import ufrpe.br.safer.usuarios.model.Usuario;

@RequiredArgsConstructor
public class CargoAnalista implements CargoUsuario {

    private final Usuario usuario;

    @Override
    public Cargo getTipoCargo() {
        return Cargo.ANALISTA;
    }

    @Override
    public boolean podeAssumirOcorrencia() {
        return true;
    }

    @Override
    public boolean podeAtualizarOcorrencia(Long analistaResponsavelId) {
        if (analistaResponsavelId == null || usuario == null || usuario.getId() == null) {
            return false;
        }
        return usuario.getId().equals(analistaResponsavelId);
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
        return true;
    }

    @Override
    public boolean podeMudarPara(Cargo novoCargo) {
        return novoCargo == Cargo.ADMINISTRADOR || novoCargo == Cargo.COMUM;
    }
}
