package ufrpe.br.safer.ocorrencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ufrpe.br.safer.ocorrencias.model.HistoricoOcorrencia;

import java.util.List;

@Repository
public interface HistoricoOcorrenciaRepository extends JpaRepository<HistoricoOcorrencia, Long> {

    List<HistoricoOcorrencia> findByOcorrenciaIdOrderByDataHoraAsc(Long ocorrenciaId);
}
