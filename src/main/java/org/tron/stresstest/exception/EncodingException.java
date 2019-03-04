package org.tron.stresstest.exception;

import org.tron.core.exception.TronException;

public class EncodingException extends TronException {
  public EncodingException() {
    super();
  }

  public EncodingException(String msg) {
    super(msg);
  }
}
