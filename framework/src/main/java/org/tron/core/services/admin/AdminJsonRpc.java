package org.tron.core.services.admin;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;

public interface AdminJsonRpc {

  @JsonRpcMethod("admin_example")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  String adminExample(@JsonRpcParam("param1") String param1, @JsonRpcParam("param2") String param2)
      throws JsonRpcInvalidParamsException;
}
