package com.eventreg.participantservice.exception;

import org.springframework.http.HttpStatus;

/** Base exception for participant-service domain errors. */
public class EventRegException extends RuntimeException {

  private final String operation;
  private final HttpStatus status;

  /** Creates an exception with a message and explicit HTTP status. */
  public EventRegException(String message, HttpStatus status) {
    super(message);
    this.operation = null;
    this.status = status;
  }

  /** Creates an exception with a message, operation name, and HTTP status. */
  public EventRegException(String message, String operation, HttpStatus status) {
    super(message);
    this.operation = operation;
    this.status = status;
  }

  /** Creates an exception with a message, operation name, HTTP status, and cause. */
  public EventRegException(String message, String operation, HttpStatus status, Throwable cause) {
    super(message, cause);
    this.operation = operation;
    this.status = status;
  }

  // Конструкторы без статуса (по умолчанию 500)

  /** Creates an exception with a message and default 500 status. */
  public EventRegException(String message) {
    super(message);
    this.operation = null;
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  /** Creates an exception with a message, operation name, and default 500 status. */
  public EventRegException(String message, String operation) {
    super(message);
    this.operation = operation;
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  /** Creates an exception with a message, cause, and default 500 status. */
  public EventRegException(String message, Throwable cause) {
    super(message, cause);
    this.operation = null;
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  /** Creates an exception with a cause and default 500 status. */
  public EventRegException(Throwable cause) {
    super(cause);
    this.operation = null;
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public String getOperation() {
    return operation;
  }

  public HttpStatus getStatus() {
    return status;
  }
}
