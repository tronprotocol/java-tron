package org.tron.core.exception;

/**
 * Maintenance clearing exception - thrown when system is in maintenance clearing state
 * Please try again later
 */
public class MaintenanceUnavailableException extends TronException {

  public MaintenanceUnavailableException() {
    super();
  }

  public MaintenanceUnavailableException(String message) {
    super(message);
  }

  public MaintenanceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}