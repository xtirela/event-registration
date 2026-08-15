package exception;

public class DuplicateException extends EventRegException {
  public DuplicateException(String message, String operation) {
    super(message, operation);
  }
}
