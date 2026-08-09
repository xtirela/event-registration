package model.enums;

public enum EventGenderRequirement {
  MALE_ONLY,
  FEMALE_ONLY,
  NONE;

  public static EventGenderRequirement fromString(String input) {
    if (input == null) return null;
    try {
      return EventGenderRequirement.valueOf(input.toUpperCase());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
