package com.eventreg.exception;

import org.springframework.http.HttpStatus;

public class EventCapacityExceededException extends EventRegException {
  public EventCapacityExceededException(Long eventId, String operation) {
    super("Event capacity exceeded for event with id " + eventId, operation, HttpStatus.CONFLICT);
  }
}
