package com.eventreg.exception;

import org.springframework.http.HttpStatus;

public class ParticipantNotFoundException extends EventRegException {
  public ParticipantNotFoundException(Long participantID, String operation) {
    super("Participant with id " + participantID + " not found", operation, HttpStatus.NOT_FOUND);
  }
}
