package com.eventreg.exception;

import org.springframework.http.HttpStatus;

public class DuplicateException extends EventRegException {
  public DuplicateException(String message, String operation) {
    super(message, operation, HttpStatus.CONFLICT);
  }
}
