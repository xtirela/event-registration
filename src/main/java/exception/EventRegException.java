package exception;

public class EventRegException extends RuntimeException {

  private final String operation;

  public EventRegException() {
    super();
    this.operation = null;
  }

  public EventRegException(String message) {
    super(message);
    this.operation = null;
  }

  public EventRegException(Throwable cause) {
    super(cause);
    this.operation = null;
  }

  public EventRegException(String message, Throwable cause) {
    super(message, cause);
    this.operation = null;
  }

  public EventRegException(String message, String operation) {
    super(message);
    this.operation = operation;
  }

  public String getOperation() {
    return operation;
  }
}
