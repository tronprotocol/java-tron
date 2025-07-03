package org.tron.core.exception.jsonrpc;

public class JsonRpcMethodNotFoundException extends JsonRpcException {

  public JsonRpcMethodNotFoundException() {
    super();
  }

  public JsonRpcMethodNotFoundException(String msg) {
    super(msg);
  }

  public JsonRpcMethodNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}