package org.tron.core.exception;

import java.security.InvalidParameterException;

public class InvalidHttpParameterException extends InvalidParameterException {

  public InvalidHttpParameterException(String message) {
    super(message);
  }

  public InvalidHttpParameterException(String message, Throwable cause) {
    super(message);
    initCause(cause);
  }
}
