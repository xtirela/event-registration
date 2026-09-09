package com.eventreg.eventregistrationservice.exception;

import org.springframework.http.HttpStatus;

/** Base exception for event registration errors carrying an HTTP status and operation. */
public class EventRegException extends RuntimeException {

  private final String operation;
  private final HttpStatus status;

  /** Creates an exception with a message and an HTTP status. */
  public EventRegException(String message, HttpStatus status) {
    super(message);
    this.operation = null;
    this.status = status;
  }

  /** Creates an exception with a message, an operation, and an HTTP status. */
  public EventRegException(String message, String operation, HttpStatus status) {
    super(message);
    this.operation = operation;
    this.status = status;
  }

  /** Creates an exception with a message, an operation, an HTTP status, and a cause. */
  public EventRegException(String message, String operation, HttpStatus status, Throwable cause) {
    super(message, cause);
    this.operation = operation;
    this.status = status;
  }

  // Конструкторы без статуса (по умолчанию 500)
  /** Creates an exception with a message and a default internal server error status. */
  public EventRegException(String message) {
    super(message);
    this.operation = null;
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  /** Creates an exception with a message, an operation, and a default 500 status. */
  public EventRegException(String message, String operation) {
    super(message);
    this.operation = operation;
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  /** Creates an exception with a message, a cause, and a default internal server error status. */
  public EventRegException(String message, Throwable cause) {
    super(message, cause);
    this.operation = null;
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  /** Creates an exception with a cause and a default internal server error status. */
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
