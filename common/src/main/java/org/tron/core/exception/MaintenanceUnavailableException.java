package org.tron.core.exception;

/**
 * Maintenance unavailable exception - thrown when service is in maintenance state
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