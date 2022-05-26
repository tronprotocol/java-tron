package org.tron.core.exception;

public class TronException extends Exception {

  public TronException() {
    super();
    report();
  }

  public TronException(String message) {
    super(message);
    report();
  }

  public TronException(String message, Throwable cause) {
    super(message, cause);
    report();
  }

  protected void report(){

  }

}
