package com.eventreg.eventservice.model.enums;

public enum EventReservationStatus {
  ALL_RESERVED,
  RESERVATIONS_CLOSED,
  RESERVATIONS_OPEN,
  CONFIRMATION_REQUIRED,
  WAITLIST;

  public static EventReservationStatus fromString(String input) {
    if (input == null) return null;
    try {
      return EventReservationStatus.valueOf(input.toUpperCase());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
