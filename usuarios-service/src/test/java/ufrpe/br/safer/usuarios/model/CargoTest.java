package ufrpe.br.safer.usuarios.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CargoTest {

    @Test
    void comumNaoRecebeAvisoDeNovaOcorrencia() {
        assertFalse(Cargo.COMUM.notificarNovaOcorrencia());
    }

    @Test
    void analistaEAdministradorRecebemAvisoDeNovaOcorrencia() {
        assertTrue(Cargo.ANALISTA.notificarNovaOcorrencia());
        assertTrue(Cargo.ADMINISTRADOR.notificarNovaOcorrencia());
    }

    @Test
    void usuarioPerguntaAoSeuCargo() {
        assertTrue(Usuario.builder().cargo(Cargo.ANALISTA).build().notificarNovaOcorrencia());
        assertFalse(Usuario.builder().cargo(Cargo.COMUM).build().notificarNovaOcorrencia());
    }

    @Test
    void usuarioSemCargoAindaNaoRecebeAviso() {
        assertFalse(Usuario.builder().build().notificarNovaOcorrencia());
    }
}
