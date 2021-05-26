package org.tron.core.services.jsonrpc;

public interface TestService {
  int getInt(int code);

  String web3_clientVersion();
  String web3_sha3(String data) throws Exception;
}