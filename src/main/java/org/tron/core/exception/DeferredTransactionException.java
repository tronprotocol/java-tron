package org.tron.core.exception;

public class DeferredTransactionException extends TronException {

  public DeferredTransactionException() {
    super();
  }

  public DeferredTransactionException(String message) {
    super(message);
  }

  public DeferredTransactionException(String message, Throwable cause) {
    super(message, cause);
  }

}
