package ufrpe.br.safer.ocorrencias.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ufrpe.br.safer.ocorrencias.model.Ocorrencia;
import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;

import java.util.List;
import java.util.Optional;

@Repository
public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {

    Optional<Ocorrencia> findByProtocolo(String protocolo);

    boolean existsByProtocolo(String protocolo);

    List<Ocorrencia> findByStatusOrderByCriadoEmDesc(StatusOcorrencia status);

    List<Ocorrencia> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    List<Ocorrencia> findAllByOrderByCriadoEmDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Ocorrencia o WHERE o.id = :id")
    Optional<Ocorrencia> findByIdForUpdate(@Param("id") Long id);
}
