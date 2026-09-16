package com.eventreg.userservice.controller.handler;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

/**
 * Central exception handler that converts errors into RFC 7807 Problem Details. Mirrors the handler
 * style of the other services; kept to the exceptions this service can actually throw (no DB, no
 * Feign, no Spring Security).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** Handles bean-validation failures on request bodies. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex, WebRequest webRequest) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getMessage());

    problemDetail.setType(URI.create("https://api.eventreg.com/errors/method-not-valid"));
    problemDetail.setTitle("Method Argument Not valid");
    problemDetail.setInstance(URI.create(webRequest.getDescription(false)));
    return problemDetail;
  }

  /** Handles malformed or unreadable HTTP request bodies. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex, WebRequest webRequest) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());

    problemDetail.setType(URI.create("https://api.eventreg.com/errors/http-message-not-readable"));
    problemDetail.setTitle("Bad Request");
    problemDetail.setInstance(URI.create(webRequest.getDescription(false)));
    return problemDetail;
  }

  /** Handles HTTP status errors thrown by the Keycloak client (401/409/503). */
  @ExceptionHandler(ResponseStatusException.class)
  public ProblemDetail handleResponseStatusException(
      ResponseStatusException ex, WebRequest webRequest) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());

    problemDetail.setType(URI.create("https://api.eventreg.com/errors/http-status"));
    problemDetail.setTitle("Request failed");
    problemDetail.setInstance(URI.create(webRequest.getDescription(false)));
    return problemDetail;
  }

  /** Handles open Keycloak circuit breaker. */
  @ExceptionHandler(CallNotPermittedException.class)
  public ProblemDetail handleCallNotPermittedException(
      CallNotPermittedException ex, WebRequest webRequest) {
    log.error("Circuit breaker open for Keycloak call", ex);

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "circuit breaker is open");

    problemDetail.setType(URI.create("https://api.eventreg.com/errors/upstream-unavailable"));
    problemDetail.setTitle("Service Unavailable");
    problemDetail.setInstance(URI.create(webRequest.getDescription(false)));
    return problemDetail;
  }

  /** Catches any unhandled exception and returns a 500 Problem Detail. */
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGenericException(Exception ex, WebRequest webRequest) {
    log.error("Unexpected error", ex);

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "unexpected error occured");

    problemDetail.setType(URI.create("https://api.eventreg.com/errors/internal-server-error"));
    problemDetail.setTitle("Internal Server Error");
    problemDetail.setInstance(URI.create(webRequest.getDescription(false)));
    return problemDetail;
  }
}
