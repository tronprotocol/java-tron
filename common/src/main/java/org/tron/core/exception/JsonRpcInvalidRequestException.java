package org.tron.core.exception;

public class JsonRpcInvalidRequestException extends TronException {

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