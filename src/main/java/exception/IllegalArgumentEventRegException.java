package exception;

public class IllegalArgumentEventRegException extends EventRegException {

  public IllegalArgumentEventRegException(String message, String operation) {
    super(message, operation);
  }
}
