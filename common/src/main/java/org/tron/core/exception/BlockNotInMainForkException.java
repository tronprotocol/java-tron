package org.tron.core.exception;

public class BlockNotInMainForkException extends TronException {
  public BlockNotInMainForkException() {
    super();
  }

  public BlockNotInMainForkException(String message) {
    super(message);
  }

  public BlockNotInMainForkException(String message, Throwable cause) {
    super(message, cause);
  }
}