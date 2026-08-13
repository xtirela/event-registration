package exception;

public class IllegalArgumentEventRegException extends java.lang.IllegalArgumentException {

  private final String operation;

  public IllegalArgumentEventRegException(String message, String operation) {
    super(message);
    this.operation = operation;
  }

  public String getOperation() {
    return operation;
  }
}
