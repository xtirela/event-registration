package com.eventreg.eventregistrationservice.model.enums;

/** Possible states of an event registration. */
public enum EventRegistrationStatus {
  ACCEPTED,
  PENDING,
  DENIED,
  CANCELLED,
  NOT_FOUND,
  DEPRECATED,
  WAITING;

  /** Parses a case-insensitive status string, returning null when unknown or null. */
  public static EventRegistrationStatus fromString(String input) {
    if (input == null) {
      return null;
    }
    try {
      return EventRegistrationStatus.valueOf(input.toUpperCase());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
