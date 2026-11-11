package ufrpe.br.safer.ocorrencias.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ufrpe.br.safer.ocorrencias.exception.OcorrenciaJaAssumidaException;
import ufrpe.br.safer.ocorrencias.exception.OcorrenciaNaoEncontradaException;
import ufrpe.br.safer.ocorrencias.exception.TransicaoStatusInvalidaException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(OcorrenciaNaoEncontradaException.class)
    public ProblemDetail handleOcorrenciaNaoEncontrada(OcorrenciaNaoEncontradaException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Ocorrência não encontrada");
        problem.setType(URI.create("https://safer.ufrpe.br/errors/not-found"));
        return problem;
    }

    @ExceptionHandler(OcorrenciaJaAssumidaException.class)
    public ProblemDetail handleOcorrenciaJaAssumida(OcorrenciaJaAssumidaException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Conflito de atendimento (RN03)");
        problem.setType(URI.create("https://safer.ufrpe.br/errors/conflict"));
        return problem;
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(org.springframework.orm.ObjectOptimisticLockingFailureException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A ocorrência foi alterada concomitantemente por outra operação. Por favor, recarregue os dados e tente novamente.");
        problem.setTitle("Conflito de concorrência");
        problem.setType(URI.create("https://safer.ufrpe.br/errors/concurrency-conflict"));
        return problem;
    }

    @ExceptionHandler(TransicaoStatusInvalidaException.class)
    public ProblemDetail handleTransicaoInvalida(TransicaoStatusInvalidaException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problem.setTitle("Transição de status inválida (RN03)");
        problem.setType(URI.create("https://safer.ufrpe.br/errors/unprocessable-entity"));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Regra de negócio violada");
        problem.setType(URI.create("https://safer.ufrpe.br/errors/bad-request"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados de entrada inválidos");
        problem.setTitle("Erro de validação");
        problem.setType(URI.create("https://safer.ufrpe.br/errors/validation"));

        Map<String, String> erros = new HashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            erros.put(fe.getField(), fe.getDefaultMessage());
        }
        problem.setProperty("erros", erros);
        return problem;
    }
}
