package org.tron.core.services.admin;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;

/**
 * Shared Admin API. Explicit error mappings are client-visible: their messages and data, including
 * exception-derived values, must be safe to disclose. Unmapped invocation errors are sanitized.
 */
public interface AdminJsonRpc {

  String BATCH_NOT_SUPPORTED_MESSAGE = "Batch requests are not supported";

  @JsonRpcMethod("admin_example")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  String adminExample(@JsonRpcParam("param1") String param1, @JsonRpcParam("param2") String param2)
      throws JsonRpcInvalidParamsException;
}
