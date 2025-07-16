package org.tron.core.exception.jsonrpc;

import lombok.Getter;
import org.tron.core.exception.TronException;

@Getter
public class JsonRpcException extends TronException {
  private Object data = null;

  public JsonRpcException() {
    super();
    report();
  }

  public JsonRpcException(String message, Object data) {
    super(message);
    this.data = data;
    report();
  }

  public JsonRpcException(String message) {
    super(message);
    report();
  }

  public JsonRpcException(String message, Throwable cause) {
    super(message, cause);
    report();
  }


}
