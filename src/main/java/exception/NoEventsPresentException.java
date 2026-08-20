package exception;

public class NoEventsPresentException extends EventRegException {
  public NoEventsPresentException(String message, String operation) {
    super(message, operation);
  }
}
