package ufrpe.br.safer.usuarios.model.cargo;

import ufrpe.br.safer.usuarios.model.Cargo;
import ufrpe.br.safer.usuarios.model.Usuario;

public final class CargoUsuarioFactory {

    private CargoUsuarioFactory() {
    }

    public static CargoUsuario criar(Usuario usuario, Cargo cargo) {
        Cargo tipo = cargo != null ? cargo : Cargo.COMUM;
        return switch (tipo) {
            case COMUM -> new CargoComum(usuario);
            case ANALISTA -> new CargoAnalista(usuario);
            case ADMINISTRADOR -> new CargoAdministrador(usuario);
        };
    }
}
