package org.tron.core.exception;

public class JsonRpcInvalidParams extends RuntimeException {

  public JsonRpcInvalidParams(String msg) {
    super(msg);
  }
}