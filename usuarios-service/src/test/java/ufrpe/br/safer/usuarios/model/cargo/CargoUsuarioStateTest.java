package ufrpe.br.safer.usuarios.model.cargo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ufrpe.br.safer.usuarios.exception.AlteracaoCargoInvalidaException;
import ufrpe.br.safer.usuarios.model.Cargo;
import ufrpe.br.safer.usuarios.model.Usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CargoUsuarioStateTest {

    private Usuario usuarioComum;
    private Usuario usuarioAnalista;
    private Usuario usuarioAdmin;

    @BeforeEach
    void setUp() {
        usuarioComum = Usuario.builder().id(1L).nome("Comum").cargo(Cargo.COMUM).build();
        usuarioAnalista = Usuario.builder().id(2L).nome("Analista").cargo(Cargo.ANALISTA).build();
        usuarioAdmin = Usuario.builder().id(3L).nome("Admin").cargo(Cargo.ADMINISTRADOR).build();
    }

    @Test
    @DisplayName("CargoComum: regras de permissão e transição")
    void testCargoComum() {
        assertFalse(usuarioComum.podeAssumirOcorrencia());
        assertFalse(usuarioComum.podeAtualizarOcorrencia(1L));
        assertFalse(usuarioComum.notificarNovaOcorrencia());

        // Cancelar: apenas se for o criador
        assertTrue(usuarioComum.podeCancelarOcorrencia(1L));
        assertFalse(usuarioComum.podeCancelarOcorrencia(2L));

        // Transição: COMUM só pode mudar para ANALISTA
        assertTrue(usuarioComum.getCargoState().podeMudarPara(Cargo.ANALISTA));
        assertFalse(usuarioComum.getCargoState().podeMudarPara(Cargo.ADMINISTRADOR));
        assertFalse(usuarioComum.getCargoState().podeMudarPara(Cargo.COMUM));

        usuarioComum.mudarCargo(Cargo.ANALISTA);
        assertEquals(Cargo.ANALISTA, usuarioComum.getCargo());
        assertTrue(usuarioComum.getCargo().ehAnalista());
    }

    @Test
    @DisplayName("Usuário sem cargo definido é tratado como comum e não recebe aviso")
    void usuarioSemCargoNaoRecebeAviso() {
        assertFalse(Usuario.builder().build().notificarNovaOcorrencia());
    }

    @Test
    @DisplayName("CargoAnalista: regras de permissão e transição")
    void testCargoAnalista() {
        assertTrue(usuarioAnalista.podeAssumirOcorrencia());
        assertTrue(usuarioAnalista.notificarNovaOcorrencia());

        // Atualizar: apenas se for o analista responsável
        assertTrue(usuarioAnalista.podeAtualizarOcorrencia(2L));
        assertFalse(usuarioAnalista.podeAtualizarOcorrencia(99L));

        // Cancelar: apenas se for o criador
        assertTrue(usuarioAnalista.podeCancelarOcorrencia(2L));
        assertFalse(usuarioAnalista.podeCancelarOcorrencia(1L));

        // Transição: ANALISTA pode ir para ADMIN ou COMUM
        assertTrue(usuarioAnalista.getCargoState().podeMudarPara(Cargo.ADMINISTRADOR));
        assertTrue(usuarioAnalista.getCargoState().podeMudarPara(Cargo.COMUM));
        assertFalse(usuarioAnalista.getCargoState().podeMudarPara(Cargo.ANALISTA));
    }

    @Test
    @DisplayName("CargoAdministrador: superpoderes e impossibilidade de rebaixamento")
    void testCargoAdministrador() {
        assertTrue(usuarioAdmin.podeAssumirOcorrencia());
        assertTrue(usuarioAdmin.podeAtualizarOcorrencia(99L));
        assertTrue(usuarioAdmin.podeCancelarOcorrencia(99L));
        assertTrue(usuarioAdmin.notificarNovaOcorrencia());

        // Transição: ADMINISTRADOR não pode ser rebaixado (RN04)
        assertFalse(usuarioAdmin.getCargoState().podeMudarPara(Cargo.ANALISTA));
        assertFalse(usuarioAdmin.getCargoState().podeMudarPara(Cargo.COMUM));

        assertThrows(AlteracaoCargoInvalidaException.class, () -> usuarioAdmin.mudarCargo(Cargo.COMUM));
    }
}
