package ufrpe.br.safer.geocodificacao.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import ufrpe.br.safer.geocodificacao.adapter.ProvedorIndisponivelException;

@RestControllerAdvice
public class ErrorHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(ProvedorIndisponivelException.class)
	ProblemDetail provedorIndisponivel(ProvedorIndisponivelException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
	}

}
