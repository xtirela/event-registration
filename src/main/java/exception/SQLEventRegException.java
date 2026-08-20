package exception;

public class SQLEventRegException extends EventRegException {
  public SQLEventRegException(String message, String operation) {
    super(message, operation);
  }

  public SQLEventRegException(String message, String operation, Throwable cause) {
    super(message, operation, cause);
  }
}
