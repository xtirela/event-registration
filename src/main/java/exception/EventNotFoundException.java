package exception;

public class EventNotFoundException extends EventRegException {
  public EventNotFoundException(int eventId, String operation) {
    super("Event with id " + eventId + " not found", operation);
  }

  public EventNotFoundException(int eventId, int eventRegistrationId, String operation) {
    super(
        "Event with id " + eventId + " does not exist for registration " + eventRegistrationId,
        operation);
  }
}
