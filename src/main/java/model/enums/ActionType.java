package model.enums;

public enum ActionType {
  CREATE_PARTICIPANT,
  CREATE_EVENT,
  REGISTER_PARTICIPANT,
  CANCEL_REGISTRATION;

  public static ActionType fromString(String input) {
    if (input == null) return null;
    try {
      return ActionType.valueOf(input.toUpperCase());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
