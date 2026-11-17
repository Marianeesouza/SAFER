package ufrpe.br.safer.notificacoes.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NotificacaoTest {

    @Test
    void primeiraTentativaBemSucedidaFicaEnviada() {
        var notificacao = Notificacao.primeiraTentativa("chave-1", "ana@ufrpe.br", "Assunto", true);

        assertEquals(StatusNotificacao.ENVIADA, notificacao.getStatus());
        assertEquals(1, notificacao.getTentativas());
        assertTrue(notificacao.foiEnviada());
        assertEquals("chave-1", notificacao.getChaveIdempotencia());
    }

    @Test
    void falhaSeguidaDeSucessoContaTentativasEMudaStatus() {
        var notificacao = Notificacao.primeiraTentativa("chave-1", "ana@ufrpe.br", "Assunto", false);

        assertEquals(StatusNotificacao.FALHA, notificacao.getStatus());
        assertFalse(notificacao.foiEnviada());

        notificacao.registrarTentativa(true);

        assertEquals(StatusNotificacao.ENVIADA, notificacao.getStatus());
        assertEquals(2, notificacao.getTentativas());
    }
}
