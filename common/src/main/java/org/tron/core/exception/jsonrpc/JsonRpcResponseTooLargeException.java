package org.tron.core.exception.jsonrpc;

public class JsonRpcResponseTooLargeException extends RuntimeException {

  public JsonRpcResponseTooLargeException() {
    super();
  }

  public JsonRpcResponseTooLargeException(String message) {
    super(message);
  }

  public JsonRpcResponseTooLargeException(String message, Throwable cause) {
    super(message, cause);
  }

}
