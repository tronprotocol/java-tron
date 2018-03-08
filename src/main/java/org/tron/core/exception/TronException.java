package org.tron.core.exception;

public class TronException extends RuntimeException {

  public TronException() {
    super();
  }

  public TronException(String message) {
    super(message);
  }

  public TronException(String message, Throwable cause) {
    super(message, cause);
  }

}
