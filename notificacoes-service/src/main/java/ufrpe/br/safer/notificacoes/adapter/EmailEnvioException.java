package ufrpe.br.safer.notificacoes.adapter;

public class EmailEnvioException extends RuntimeException {
    public EmailEnvioException(String message, Throwable cause) {
        super(message, cause);
    }
}
