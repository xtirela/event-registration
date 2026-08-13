package model.enums;

public enum EventRegRequestStatus {
  ACCEPTED,
  PENDING,
  DENIED,
  CANCELLED,
  NOT_FOUND,
  DEPRECATED,
  WAITING;

  public static EventRegRequestStatus fromString(String input) {
    if (input == null) return null;
    try {
      return EventRegRequestStatus.valueOf(input.toUpperCase());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
