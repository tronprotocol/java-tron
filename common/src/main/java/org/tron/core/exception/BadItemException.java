package org.tron.core.exception;

public class BadItemException extends StoreException {

  public BadItemException() {
    super();
  }

  public BadItemException(String message) {
    super(message);
  }

  public BadItemException(String message, Throwable cause) {
    super(message, cause);
  }
}
