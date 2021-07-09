package org.tron.core.services.jsonrpc;

public class JsonRpcInvalidParams extends RuntimeException {

  public JsonRpcInvalidParams(String msg) {
    super(msg);
  }
}