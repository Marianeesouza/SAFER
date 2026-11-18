package ufrpe.br.safer.ocorrencias.messaging;

import ufrpe.br.safer.ocorrencias.dto.DestinatarioDTO;
import ufrpe.br.safer.ocorrencias.messaging.command.EnviarEmailCommand;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaCanceladaEvent;
import ufrpe.br.safer.ocorrencias.messaging.event.OcorrenciaRegistradaEvent;
import ufrpe.br.safer.ocorrencias.messaging.event.StatusOcorrenciaAlteradoEvent;
import ufrpe.br.safer.ocorrencias.model.StatusOcorrencia;

final class EmailOcorrenciaBuilder {

    private static final String ASSINATURA = "\n\nEquipe SAFER - UFRPE";

    private EmailOcorrenciaBuilder() {
    }

    static EnviarEmailCommand registro(OcorrenciaRegistradaEvent evento) {
        String corpo = """
                Olá, %s.

                Sua ocorrência foi registrada e será analisada pela nossa equipe.

                Protocolo: %s
                Animal: %s
                Local: %s

                Guarde o protocolo para acompanhar o andamento.""".formatted(
                evento.comunicanteNome(), evento.protocolo(), evento.descricaoAnimal(), evento.endereco());
        return new EnviarEmailCommand(
                "ocorrencia-" + evento.ocorrenciaId() + "-registrada",
                evento.comunicanteEmail(),
                "SAFER - Ocorrência " + evento.protocolo() + " registrada",
                corpo + ASSINATURA);
    }

    static EnviarEmailCommand novaOcorrenciaParaAnalista(
            OcorrenciaRegistradaEvent evento, DestinatarioDTO analista, String urlSistema) {
        String corpo = """
                Olá, %s.

                Uma nova ocorrência aguarda atendimento.

                Protocolo: %s
                Animal: %s
                Local: %s

                Acesse o sistema para assumir o atendimento: %s""".formatted(
                analista.nome(), evento.protocolo(), evento.descricaoAnimal(), evento.endereco(), urlSistema);
        return new EnviarEmailCommand(
                "ocorrencia-" + evento.ocorrenciaId() + "-analista-" + analista.id(),
                analista.email(),
                "SAFER - Nova ocorrência " + evento.protocolo() + " aguardando atendimento",
                corpo + ASSINATURA);
    }

    static EnviarEmailCommand statusAlterado(StatusOcorrenciaAlteradoEvent evento) {
        String corpo = """
                Olá.

                Houve uma atualização na sua ocorrência %s.

                Situação atual: %s""".formatted(evento.protocolo(), descricao(evento.novoStatus()));
        return new EnviarEmailCommand(
                "ocorrencia-" + evento.ocorrenciaId() + "-status-" + evento.novoStatus().name(),
                evento.comunicanteEmail(),
                "SAFER - Atualização da ocorrência " + evento.protocolo(),
                corpo + ASSINATURA);
    }

    static EnviarEmailCommand cancelamento(OcorrenciaCanceladaEvent evento) {
        String corpo = """
                Olá.

                Sua ocorrência %s foi cancelada.

                Motivo: %s""".formatted(evento.protocolo(), evento.justificativa());
        return new EnviarEmailCommand(
                "ocorrencia-" + evento.ocorrenciaId() + "-cancelada",
                evento.comunicanteEmail(),
                "SAFER - Ocorrência " + evento.protocolo() + " cancelada",
                corpo + ASSINATURA);
    }

    // Sem default: um status novo no enum quebra a compilação até ganhar um texto.
    static String descricao(StatusOcorrencia status) {
        return switch (status) {
            case EM_ABERTO -> "aguardando atendimento";
            case EM_ATENDIMENTO -> "em atendimento pela nossa equipe";
            case NECESSITA_ATENDIMENTO_MEDICO -> "o animal precisa de atendimento médico";
            case FINALIZADO_COM_SUCESSO -> "finalizada com sucesso";
            case FINALIZADO_COM_COMPLICACOES -> "finalizada, mas houve complicações no atendimento";
            case CANCELADO -> "cancelada";
        };
    }
}
