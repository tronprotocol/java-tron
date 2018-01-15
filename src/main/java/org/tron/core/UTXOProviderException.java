package org.tron.core;


public class UTXOProviderException extends Exception {

  public UTXOProviderException(String message, Throwable cause) {
    super(message, cause);
  }

  public UTXOProviderException(Throwable cause) {
    super(cause);
  }
}
