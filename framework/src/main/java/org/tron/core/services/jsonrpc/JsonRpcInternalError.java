package org.tron.core.services.jsonrpc;

public class JsonRpcInternalError extends RuntimeException {

  public JsonRpcInternalError() {
    super();
  }

  public JsonRpcInternalError(String message) {
    super(message);
  }

  public JsonRpcInternalError(String message, Throwable cause) {
    super(message, cause);
  }
}