package org.tron.core.services.jsonrpc;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import org.springframework.stereotype.Component;

@Component
public interface TestService {

  @JsonRpcMethod("getInt")
  int getInt(int code);

  @JsonRpcMethod("net_version")
  int getNetVersion();

  @JsonRpcMethod("net_listening")
  boolean isListening();

  @JsonRpcMethod("eth_protocolVersion")
  int getProtocolVersion();
}