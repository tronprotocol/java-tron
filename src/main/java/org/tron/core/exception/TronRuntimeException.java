package org.tron.core.exception;

public class TronRuntimeException extends RuntimeException {

  public TronRuntimeException() {
    super();
  }

  public TronRuntimeException(String message) {
    super(message);
  }

  public TronRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  public TronRuntimeException(Throwable cause) {
    super(cause);
  }

  protected TronRuntimeException(String message, Throwable cause,
                             boolean enableSuppression,
                             boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }


}
