package org.tron.core.exception;

public class OutOfSlotTimeException extends Exception {

  public OutOfSlotTimeException() {
    super();
  }

  public OutOfSlotTimeException(String message) {
    super(message);
  }

  public OutOfSlotTimeException(String message, Throwable cause) {
    super(message, cause);
  }

}
