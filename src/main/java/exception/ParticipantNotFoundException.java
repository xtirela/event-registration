package exception;

public class ParticipantNotFoundException extends EventRegException {
  public ParticipantNotFoundException(int participantID, String operation) {
    super("Participant with id " + participantID + " not found", operation);
  }
}
