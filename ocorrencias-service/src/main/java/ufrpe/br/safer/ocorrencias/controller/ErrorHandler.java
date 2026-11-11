package ufrpe.br.safer.ocorrencias.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ufrpe.br.safer.ocorrencias.exception.OcorrenciaJaAssumidaException;
import ufrpe.br.safer.ocorrencias.exception.OcorrenciaNaoEncontradaException;
import ufrpe.br.safer.ocorrencias.exception.BusinessRuleException;
import ufrpe.br.safer.ocorrencias.exception.TransicaoStatusInvalidaException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ErrorHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(OcorrenciaNaoEncontradaException.class)
    public ProblemDetail handleOcorrenciaNaoEncontrada(OcorrenciaNaoEncontradaException e) {
        return problema(HttpStatus.NOT_FOUND, "Ocorrência não encontrada", "not-found", e.getMessage());
    }

    @ExceptionHandler(OcorrenciaJaAssumidaException.class)
    public ProblemDetail handleOcorrenciaJaAssumida(OcorrenciaJaAssumidaException e) {
        return problema(HttpStatus.CONFLICT, "Conflito de atendimento (RN03)", "conflict", e.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        return problema(HttpStatus.CONFLICT, "Conflito de concorrência", "concurrency-conflict",
                "A ocorrência foi alterada concomitantemente por outra operação. Por favor, recarregue os dados e tente novamente.");
    }

    @ExceptionHandler(TransicaoStatusInvalidaException.class)
    public ProblemDetail handleTransicaoInvalida(TransicaoStatusInvalidaException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Transição de status inválida (RN03)", "unprocessable-entity", e.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException e) {
        return problema(HttpStatus.BAD_REQUEST, "Regra de negócio violada", "bad-request", e.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = problema(HttpStatus.BAD_REQUEST, "Erro de validação", "validation", "Dados de entrada inválidos");
        Map<String, String> erros = new HashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            erros.put(fe.getField(), fe.getDefaultMessage());
        }
        problem.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(problem);
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String tipo, String detalhe) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalhe);
        problem.setTitle(titulo);
        problem.setType(URI.create("https://safer.ufrpe.br/errors/" + tipo));
        return problem;
    }
}
