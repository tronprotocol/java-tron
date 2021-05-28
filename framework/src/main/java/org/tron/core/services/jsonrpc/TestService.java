package org.tron.core.services.jsonrpc;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import org.springframework.stereotype.Component;
import org.tron.core.exception.TronException;

@Component
public interface TestService {

  @JsonRpcMethod("getInt")
  int getInt(int code);

  @JsonRpcMethod("web3_clientVersion")
  String web3ClientVersion();

  @JsonRpcMethod("web3_sha3")
  String web3Sha3(String data) throws Exception;

  @JsonRpcMethod("eth_getBlockTransactionCountByHash")
  String ethGetBlockTransactionCountByHash(String blockHash) throws Exception;

  @JsonRpcMethod("eth_getBlockTransactionCountByNumber")
  String ethGetBlockTransactionCountByNumber(String bnOrId) throws Exception;

  @JsonRpcMethod("net_version")
  int getNetVersion();

  @JsonRpcMethod("net_listening")
  boolean isListening();

  @JsonRpcMethod("eth_protocolVersion")
  int getProtocolVersion();

  @JsonRpcMethod("eth_syncing")
  Object isSyncing();

  @JsonRpcMethod("eth_coinbase")
  String getCoinbase() throws Exception;

  @JsonRpcMethod("eth_gasPrice")
  String gasPrice();

  @JsonRpcMethod("eth_estimateGas")
  String estimateGas();

}
