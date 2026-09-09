package com.eventreg.eventregistrationservice.controller.handler;

import com.eventreg.eventregistrationservice.exception.EventRegException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/** Translates exceptions into structured ProblemDetail responses. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** Maps an EventRegException to a ProblemDetail with its status. */
  @ExceptionHandler(EventRegException.class)
  public ProblemDetail handleEventRegException(EventRegException ex, WebRequest webRequest) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());

    problemDetail.setType(
        URI.create("https://api.eventreg.com/errors/" + ex.getClass().getSimpleName()));
    problemDetail.setTitle(ex.getClass().getSimpleName());
    problemDetail.setInstance(URI.create(webRequest.getDescription(false)));

    return problemDetail;
  }

  /** Maps a validation failure to a bad request ProblemDetail. */
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

  /** Maps an access denial to a forbidden ProblemDetail. */
  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDeniedException(
      AccessDeniedException ex, WebRequest webRequest) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());

    problemDetail.setType(URI.create("https://api.eventreg.com/errors/access-denied"));
    problemDetail.setTitle("Forbidden");
    problemDetail.setInstance(URI.create(webRequest.getDescription(false)));
    return problemDetail;
  }

  /** Maps a data integrity violation to a conflict ProblemDetail. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrityViolationException(
      DataIntegrityViolationException ex, WebRequest webRequest) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());

    problemDetail.setType(URI.create("https://api.eventreg.com/errors/data-integrity-violation"));
    problemDetail.setTitle("Conflict");
    problemDetail.setInstance(URI.create(webRequest.getDescription(false)));
    return problemDetail;
  }

  /** Maps an invalid sort property reference to a bad request ProblemDetail. */
  @ExceptionHandler(PropertyReferenceException.class)
  public ProblemDetail handlePropertyReferenceException(
      PropertyReferenceException ex, WebRequest webRequest) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());

    problemDetail.setType(URI.create("https://api.eventreg.com/errors/property-reference"));
    problemDetail.setTitle("Bad Request");
    problemDetail.setInstance(URI.create(webRequest.getDescription(false)));
    return problemDetail;
  }

  /** Maps an invalid data access usage to a bad request ProblemDetail. */
  @ExceptionHandler(InvalidDataAccessApiUsageException.class)
  public ProblemDetail handleInvalidDataAccessApiUsageException(
      InvalidDataAccessApiUsageException ex, WebRequest webRequest) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());

    problemDetail.setType(
        URI.create("https://api.eventreg.com/errors/invalid-data-access-api-usage"));
    problemDetail.setTitle("Bad Request");
    problemDetail.setInstance(URI.create(webRequest.getDescription(false)));
    return problemDetail;
  }

  /** Maps an unreadable message body to a bad request ProblemDetail. */
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

  /** Maps an authorization denial to a forbidden ProblemDetail. */
  @ExceptionHandler(AuthorizationDeniedException.class)
  public ProblemDetail handleAuthorizationDeniedException(
      AuthorizationDeniedException ex, WebRequest webRequest) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());

    problemDetail.setType(URI.create("https://api.eventreg.com/errors/access-denied"));
    problemDetail.setTitle("Forbidden");
    problemDetail.setInstance(URI.create(webRequest.getDescription(false)));
    return problemDetail;
  }

  /** Maps bad credentials to an unauthorized ProblemDetail. */
  @ExceptionHandler(BadCredentialsException.class)
  public ProblemDetail handleBadCredentialsException(
      BadCredentialsException ex, WebRequest webRequest) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());

    problemDetail.setType(URI.create("https://api.eventreg.com/errors/access-denied"));
    problemDetail.setTitle("Forbidden");
    problemDetail.setInstance(URI.create(webRequest.getDescription(false)));
    return problemDetail;
  }

  /** Maps a downstream Feign failure to a service unavailable ProblemDetail. */
  @ExceptionHandler(FeignException.class)
  public ProblemDetail handleFeignException(FeignException ex, WebRequest webRequest) {
    log.error("Downstream call failed", ex);

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE, "upstream service unavailable");

    problemDetail.setType(URI.create("https://api.eventreg.com/errors/upstream-unavailable"));
    problemDetail.setTitle("Service Unavailable");
    problemDetail.setInstance(URI.create(webRequest.getDescription(false)));
    return problemDetail;
  }

  /** Maps an open circuit breaker to a service unavailable ProblemDetail. */
  @ExceptionHandler(CallNotPermittedException.class)
  public ProblemDetail handleCallNotPermittedException(
      CallNotPermittedException ex, WebRequest webRequest) {
    log.error("Circuit breaker open for downstream call", ex);

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "circuit breaker is open");

    problemDetail.setType(URI.create("https://api.eventreg.com/errors/upstream-unavailable"));
    problemDetail.setTitle("Service Unavailable");
    problemDetail.setInstance(URI.create(webRequest.getDescription(false)));
    return problemDetail;
  }

  /** Maps any unhandled exception to an internal server error ProblemDetail. */
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
