package org.tron.core.exception;

public class JsonRpcInternalException extends TronException {

  public JsonRpcInternalException() {
    super();
  }

  public JsonRpcInternalException(String message) {
    super(message);
  }

  public JsonRpcInternalException(String message, Throwable cause) {
    super(message, cause);
  }
}