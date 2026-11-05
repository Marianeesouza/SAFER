package Componentes;

import Entidades.Ocorrencia.Ocorrencia;
import Entidades.Usuario.Usuario;

/**
 * +cadastrarOcorrência(dadosResgate: DadosResgateDTO): Ocorrencia
 * +consultarOcorrenciaPorProtocolo(protocolo: String): Ocorrencia
 * +atualizarStatusOcorrencia(idOcorrencia: long, novoStatus: StatusOcorrencia, justificativa: String): void
 * +cancelarOcorrencia(idOcorrencia: long, justificativa: String): boolean
 * +vincularOcorrencia(idOcorrencia: long, idUsuario: long): void
 * +autenticarUsuario(login: String, senha: String): Usuario
 * +cadastrarUsuario(usuario: Usuario): Usuario
 */
public interface IGateway {

	public abstract Ocorrencia cadastrarOcorrencia();

	public abstract Ocorrencia consultarOcorrenciaProtocolo();

	public abstract Ocorrencia consultarOcorrenciaId();

	public abstract Ocorrencia atualizarStatusOcorrencia();

	public abstract boolean cancelarOcorrencia();

	public abstract Ocorrencia vincularOcorrencia();

	public abstract Usuario cadastrarUsuario();

	public abstract Usuario autenticarUsuario();

}
