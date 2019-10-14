package org.tron.core.exception;

public class StoreException extends Exception {

  public StoreException() {
    super();
  }

  public StoreException(String message) {
    super(message);
  }

  public StoreException(String message, Throwable cause) {
    super(message, cause);
  }

  public StoreException(Throwable cause) {
    super(cause);
  }
}
