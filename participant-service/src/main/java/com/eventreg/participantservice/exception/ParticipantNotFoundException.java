package com.eventreg.participantservice.exception;

import org.springframework.http.HttpStatus;

/** Exception thrown when a requested participant cannot be found. */
public class ParticipantNotFoundException extends EventRegException {
  public ParticipantNotFoundException(Long participantId, String operation) {
    super("Participant with id " + participantId + " not found", operation, HttpStatus.NOT_FOUND);
  }
}
