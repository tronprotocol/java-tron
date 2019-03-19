package org.tron.stresstest.exception;

import org.tron.core.exception.TronException;

public class CancelException extends TronException {

  public CancelException() {
    super();
  }

  public CancelException(String message) {
    super(message);
  }

}
