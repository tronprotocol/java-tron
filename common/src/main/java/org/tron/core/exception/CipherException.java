package org.tron.core.exception;

/**
 * Cipher exception wrapper.
 */
public class CipherException extends Exception {

  public CipherException(String message) {
    super(message);
  }

  public CipherException(Throwable cause) {
    super(cause);
  }

  public CipherException(String message, Throwable cause) {
    super(message, cause);
  }
}
