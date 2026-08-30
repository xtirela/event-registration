package com.eventreg.exception;

import org.springframework.http.HttpStatus;

public class EventRegistrationNotFoundException extends EventRegException {
  public EventRegistrationNotFoundException(Long eventRegId, String operation) {
    super("Registration with id " + eventRegId + " not found", operation, HttpStatus.NOT_FOUND);
  }
}
