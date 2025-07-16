package org.tron.core.exception.jsonrpc;

public class JsonRpcInvalidRequestException extends JsonRpcException {

  public JsonRpcInvalidRequestException() {
    super();
  }

  public JsonRpcInvalidRequestException(String message) {
    super(message);
  }

  public JsonRpcInvalidRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}