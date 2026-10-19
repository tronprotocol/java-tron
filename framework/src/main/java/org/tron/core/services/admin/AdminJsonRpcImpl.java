package org.tron.core.services.admin;

import org.springframework.stereotype.Component;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;

@Component
public class AdminJsonRpcImpl implements AdminJsonRpc {
  @Override
  public String adminExample(String param1, String param2) throws JsonRpcInvalidParamsException {
    if ("".equals(param1) || "".equals(param2)) {
      throw new JsonRpcInvalidParamsException("param1 or param2 should not be empty");
    }
    return param1 + ":" + param2;
  }
}
