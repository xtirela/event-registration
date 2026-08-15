package exception;

public class EventCapacityExceededException extends EventRegException {
  public EventCapacityExceededException(int eventId, String operation) {
    super("Event capacity exceeded for event with id " + eventId, operation);
  }
}
