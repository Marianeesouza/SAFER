package ufrpe.br.safer.notificacoes.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ufrpe.br.safer.notificacoes.model.Notificacao;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    Optional<Notificacao> findByChaveIdempotencia(String chaveIdempotencia);
}
