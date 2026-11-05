package Entidades.Ocorrencia;

import Entidades.Usuario.Usuario;
import types.String;
import types.Foto;
import Entidades.Util.StatusOcorrencia;

public class CadastroOcorrencia {

	private static CadastroOcorrencia instanciaCadastroOcorrencia;

	private IRepositorioOcorrencia iRepositorioOcorrencia;

	public static CadastroOcorrencia getInstanciaCadastroOcorrencia() {
		return null;
	}

	public Ocorrencia criarOcorrencia(Usuario usuario, DadosComunicante dadosComunicante, DadosLocalizacao dadosLocalizacao, String animal, Foto[] fotos) {
		return null;
	}

	public Ocorrencia buscarOcorrenciaPorId(long idOcorrencia) {
		return null;
	}

	public Ocorrencia buscarPorProtocolo(String numeroProtocolo) {
		return null;
	}

	public Ocorrencia[] buscarOcorrenciasEmAberto() {
		return null;
	}

	public Ocorrencia[] buscarOcorrenciasPorUsuario(long idUsuario) {
		return null;
	}

	public Ocorrencia vincularAnalista(Ocorrencia ocorrencia, int analista) {
		return null;
	}

	public void atualizarStatus(Ocorrencia ocorrencia, StatusOcorrencia novoStatus) {

	}

	public Ocorrencia atualizarOcorrencia(Ocorrencia ocorrencia) {
		return null;
	}

	public void cancelarOcorrencia(Ocorrencia ocorrencia) {

	}

}
