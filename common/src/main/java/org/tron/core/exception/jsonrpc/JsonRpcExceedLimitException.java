package org.tron.core.exception.jsonrpc;

public class JsonRpcExceedLimitException extends JsonRpcException {

  public JsonRpcExceedLimitException() {
    super();
  }

  public JsonRpcExceedLimitException(String message) {
    super(message);
  }

  public JsonRpcExceedLimitException(String message, Throwable cause) {
    super(message, cause);
  }
}