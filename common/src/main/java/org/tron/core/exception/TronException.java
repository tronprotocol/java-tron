package org.tron.core.exception;

import lombok.Getter;

@Getter
public class TronException extends Exception {
  private Object data = null;

  public TronException() {
    super();
    report();
  }

  public TronException(String message, Object data) {
    super(message);
    this.data = data;
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
