package Componentes;

import Entidades.Ocorrencia.Ocorrencia;

/**
 * +registrarOcorrencia(ocorrencia: Ocorrencia): Ocorrencia
 * +buscarPorProtocolo(protocolo: String): Ocorrencia
 * +buscarPorId(idOcorrencia: long): Ocorrencia
 * +atualizarStatus(idOcorrencia: long, novoStatus: StatusOcorrencia): void
 * +cancelarOcorrencia(idOcorrencia: long, justificativa: String): boolean
 * +vincularOcorrencia(idOcorrencia: long, idUsuario: long): void
 */
public interface IServiceOcorrencias {

	public abstract Ocorrencia registrarOcorrencia();

	public abstract Ocorrencia buscarPorId();

	public abstract Ocorrencia buscarPorProtocolo();

	public abstract Ocorrencia atualizarStatusOcorrencia();

	public abstract boolean cancelarOcorrencia();

	public abstract boolean vincularOcorrencia();

}
