package org.tron.core.exception;

public class JsonRpcInvalidRequest extends RuntimeException {

  public JsonRpcInvalidRequest() {
    super();
  }

  public JsonRpcInvalidRequest(String message) {
    super(message);
  }

  public JsonRpcInvalidRequest(String message, Throwable cause) {
    super(message, cause);
  }
}