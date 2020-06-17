package org.tron.core.exception;

public class ItemNotFoundException extends StoreException {

  public ItemNotFoundException(String message) {
    super(message);
  }

  public ItemNotFoundException() {
    super();
  }

  public ItemNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
