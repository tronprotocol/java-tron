package org.tron.core.exception;

public class VMMemoryOverflowException extends TronException {

  public VMMemoryOverflowException() {
    super("VM memory overflow");
  }

  public VMMemoryOverflowException(String message) {
    super(message);
  }

}
