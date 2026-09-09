package com.eventreg.eventservice.exception;

import org.springframework.http.HttpStatus;

public class EventRegException extends RuntimeException {

  private final String operation;
  private final HttpStatus status;

  public EventRegException(String message, HttpStatus status) {
    super(message);
    this.operation = null;
    this.status = status;
  }

  public EventRegException(String message, String operation, HttpStatus status) {
    super(message);
    this.operation = operation;
    this.status = status;
  }

  public EventRegException(String message, String operation, HttpStatus status, Throwable cause) {
    super(message, cause);
    this.operation = operation;
    this.status = status;
  }

  // Конструкторы без статуса (по умолчанию 500)
  public EventRegException(String message) {
    super(message);
    this.operation = null;
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public EventRegException(String message, String operation) {
    super(message);
    this.operation = operation;
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public EventRegException(String message, Throwable cause) {
    super(message, cause);
    this.operation = null;
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

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
