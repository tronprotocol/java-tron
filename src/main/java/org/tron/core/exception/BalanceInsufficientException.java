package org.tron.core.exception;

public class BalanceInsufficientException extends TronException {

  public BalanceInsufficientException() {
    super();
  }

  public BalanceInsufficientException(String message) {
    super(message);
  }
}
