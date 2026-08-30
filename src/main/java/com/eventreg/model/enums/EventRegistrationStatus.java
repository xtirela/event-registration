package com.eventreg.model.enums;

public enum EventRegistrationStatus {
  ACCEPTED,
  PENDING,
  DENIED,
  CANCELLED,
  NOT_FOUND,
  DEPRECATED,
  WAITING;

  public static EventRegistrationStatus fromString(String input) {
    if (input == null) return null;
    try {
      return EventRegistrationStatus.valueOf(input.toUpperCase());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
