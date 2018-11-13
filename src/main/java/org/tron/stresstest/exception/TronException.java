package org.tron.stresstest.exception;

public class TronException extends Exception {

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
