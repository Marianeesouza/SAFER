package ufrpe.br.safer.notificacoes.adapter;

public interface EmailSender {
    void enviar(String destinatario, String assunto, String corpo);
}
