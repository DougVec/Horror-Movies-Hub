package com.douglas.horrorhub.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    ProblemDetail tmdbNaoAchou(HttpClientErrorException.NotFound e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "filme não encontrado na TMDB");
    }

    @ExceptionHandler(HttpClientErrorException.TooManyRequests.class)
    ProblemDetail tmdbRateLimit(HttpClientErrorException.TooManyRequests e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "TMDB limitou as requisições, tente em instantes");
    }

    /** Corrida entre o "já existe?" e o insert: quem chega em segundo bate na constraint única do banco. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflito(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "registro duplicado");
    }

    /** Qualquer outra falha na TMDB (fora do ar, token inválido) vira 502, sem vazar detalhes. */
    @ExceptionHandler(RestClientException.class)
    ProblemDetail tmdbFalhou(RestClientException e) {
        log.error("falha ao chamar a TMDB", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "catálogo externo indisponível");
    }
}
