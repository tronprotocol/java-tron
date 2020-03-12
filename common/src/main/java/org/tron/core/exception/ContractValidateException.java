package org.tron.core.exception;

public class ContractValidateException extends TronException {

  public ContractValidateException() {
    super();
  }

  public ContractValidateException(String message) {
    super(message);
  }

  public ContractValidateException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
