package ufrpe.br.safer.ocorrencias.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StatusOcorrenciaConverter implements AttributeConverter<StatusOcorrencia, Integer> {

    @Override
    public Integer convertToDatabaseColumn(StatusOcorrencia status) {
        return status == null ? null : status.getCodigo();
    }

    @Override
    public StatusOcorrencia convertToEntityAttribute(Integer codigo) {
        return codigo == null ? null : StatusOcorrencia.deCodigo(codigo);
    }
}
