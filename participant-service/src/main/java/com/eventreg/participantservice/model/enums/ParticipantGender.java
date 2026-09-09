package com.eventreg.participantservice.model.enums;

/** Enumeration of supported participant genders. */
public enum ParticipantGender {
  MALE,
  FEMALE,
  NOT_SPECIFIED;

  /** Converts a string to the corresponding enum value, or null if invalid. */
  public static ParticipantGender fromString(String input) {
    if (input == null) {
      return null;
    }
    try {
      return ParticipantGender.valueOf(input.toUpperCase());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
