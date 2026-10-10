package org.tron.core.exception.jsonrpc;

public class JsonRpcExecutionRevertedException extends JsonRpcException {

  public JsonRpcExecutionRevertedException(String message) {
    super(message);
  }

  public JsonRpcExecutionRevertedException(String message, Object data) {
    super(message, data);
  }
}
