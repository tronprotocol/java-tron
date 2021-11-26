package org.tron.core.exception;

public class JsonRpcTooManyResultException extends TronException {

  public JsonRpcTooManyResultException() {
    super();
  }

  public JsonRpcTooManyResultException(String message) {
    super(message);
  }

  public JsonRpcTooManyResultException(String message, Throwable cause) {
    super(message, cause);
  }
}