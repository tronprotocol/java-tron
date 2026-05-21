package org.tron.core.exception.jsonrpc;

public class JsonRpcTooManyResultException extends JsonRpcException {

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