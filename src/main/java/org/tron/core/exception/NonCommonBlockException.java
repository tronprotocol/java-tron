package org.tron.core.exception;

public class NonCommonBlockException extends TronException {

  public NonCommonBlockException() {
    super();
  }

  public NonCommonBlockException(String message) {
    super(message);
  }

  public NonCommonBlockException(String message, Throwable cause) {
    super(message, cause);
  }
}
