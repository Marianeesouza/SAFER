package ufrpe.br.safer.ocorrencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ufrpe.br.safer.ocorrencias.model.Ocorrencia;
import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {

    Optional<Ocorrencia> findByProtocolo(String protocolo);

    boolean existsByProtocolo(String protocolo);

    List<Ocorrencia> findByStatusOrderByCriadoEmDesc(StatusOcorrencia status);

    List<Ocorrencia> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    List<Ocorrencia> findAllByOrderByCriadoEmDesc();

    @Modifying
    @Query("UPDATE Ocorrencia o SET o.analistaResponsavelId = :analistaId, o.status = :novoStatus, o.atualizadoEm = :agora " +
           "WHERE o.id = :id AND o.status = :statusEsperado AND o.analistaResponsavelId IS NULL")
    int vincularAnalistaAtomicamente(
            @Param("id") Long id,
            @Param("analistaId") Long analistaId,
            @Param("novoStatus") StatusOcorrencia novoStatus,
            @Param("statusEsperado") StatusOcorrencia statusEsperado,
            @Param("agora") LocalDateTime agora
    );

    @Modifying
    @Query("UPDATE Ocorrencia o SET o.status = :statusCancelado, o.atualizadoEm = :agora " +
           "WHERE o.id = :id AND o.status = :statusEsperado")
    int cancelarOcorrenciaAtomicamente(
            @Param("id") Long id,
            @Param("statusCancelado") StatusOcorrencia statusCancelado,
            @Param("statusEsperado") StatusOcorrencia statusEsperado,
            @Param("agora") LocalDateTime agora
    );
}
