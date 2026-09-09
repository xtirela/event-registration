package com.eventreg.participantservice.controller.handler;

import com.eventreg.participantservice.exception.EventRegException;
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

/** Central exception handler that converts domain exceptions into RFC 7807 Problem Details. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** Handles domain-specific EventRegException instances. */
  @ExceptionHandler(EventRegException.class)
  public ProblemDetail handleEventRegException(EventRegException ex, WebRequest webRequest) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());

    problemDetail.setType(
        URI.create("https://api.eventreg.com/errors/" + ex.getClass().getSimpleName()));
    problemDetail.setTitle(ex.getClass().getSimpleName());
    problemDetail.setInstance(URI.create(webRequest.getDescription(false)));

    return problemDetail;
  }

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

  /** Handles Spring Security access-denied errors. */
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

  /** Handles database constraint-violation errors. */
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

  /** Handles invalid property-reference errors from Spring Data. */
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

  /** Handles invalid-data-access API usage errors from Spring Data. */
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

  /** Handles authorization-denied errors from the security filter chain. */
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

  /** Handles bad-credential authentication errors. */
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
