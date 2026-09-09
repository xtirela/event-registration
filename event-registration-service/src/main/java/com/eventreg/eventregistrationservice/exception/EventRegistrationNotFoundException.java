package com.eventreg.eventregistrationservice.exception;

import org.springframework.http.HttpStatus;

/** Thrown when an event registration cannot be found. */
public class EventRegistrationNotFoundException extends EventRegException {
  public EventRegistrationNotFoundException(Long eventRegId, String operation) {
    super("Registration with id " + eventRegId + " not found", operation, HttpStatus.NOT_FOUND);
  }
}
