package ufrpe.br.safer.usuarios.model.cargo;

import lombok.RequiredArgsConstructor;
import ufrpe.br.safer.usuarios.model.Cargo;
import ufrpe.br.safer.usuarios.model.Usuario;

@RequiredArgsConstructor
public class CargoAdministrador implements CargoUsuario {

    private final Usuario usuario;

    @Override
    public Cargo getTipoCargo() {
        return Cargo.ADMINISTRADOR;
    }

    @Override
    public boolean podeAssumirOcorrencia() {
        return true;
    }

    @Override
    public boolean podeAtualizarOcorrencia(Long analistaResponsavelId) {
        return true;
    }

    @Override
    public boolean podeCancelarOcorrencia(Long criadorOcorrenciaId) {
        return true;
    }

    @Override
    public boolean notificarNovaOcorrencia() {
        return true;
    }

    @Override
    public boolean podeMudarPara(Cargo novoCargo) {
        return false;
    }
}
