package org.tron.core.exception.jsonrpc;

public class JsonRpcInternalException extends JsonRpcException {

  public JsonRpcInternalException() {
    super();
  }

  public JsonRpcInternalException(String message) {
    super(message);
  }

  public JsonRpcInternalException(String message, Throwable cause) {
    super(message, cause);
  }

  public JsonRpcInternalException(String message, Object data) {
    super(message, data);
  }
}