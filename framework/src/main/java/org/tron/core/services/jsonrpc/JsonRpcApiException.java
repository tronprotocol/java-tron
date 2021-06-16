package org.tron.core.services.jsonrpc;

public class JsonRpcApiException extends RuntimeException {

  public JsonRpcApiException(String msg) {
    super(msg);
  }
}