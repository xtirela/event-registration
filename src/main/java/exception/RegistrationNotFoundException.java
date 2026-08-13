package exception;

public class RegistrationNotFoundException extends EventRegException {
  public RegistrationNotFoundException(int eventRegId, String operation) {
    super("Registration with id " + eventRegId + " not found", operation);
  }
}
