package ufrpe.br.safer.geocodificacao.adapter;

public class ProvedorIndisponivelException extends RuntimeException {

	public ProvedorIndisponivelException(Throwable causa) {
		super("Provedor de geocodificação indisponível", causa);
	}

}
