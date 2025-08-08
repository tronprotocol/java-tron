package org.tron.core.exception;

/**
 * Maintenance clearing exception - thrown when system is in maintenance clearing state
 * Please try again later
 */
public class MaintenanceClearingException extends TronException {

  public MaintenanceClearingException() {
    super();
  }

  public MaintenanceClearingException(String message) {
    super(message);
  }

  public MaintenanceClearingException(String message, Throwable cause) {
    super(message, cause);
  }
}