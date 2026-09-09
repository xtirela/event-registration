package com.eventreg.eventservice.exception;

import org.springframework.http.HttpStatus;

public class EventNotFoundException extends EventRegException {
  public EventNotFoundException(Long eventId, String operation) {
    super("Event with id " + eventId + " not found", operation, HttpStatus.NOT_FOUND);
  }

  public EventNotFoundException(Long eventId, Long eventRegistrationId, String operation) {
    super(
        "Event with id " + eventId + " does not exist for registration " + eventRegistrationId,
        operation,
        HttpStatus.NOT_FOUND);
  }
}
