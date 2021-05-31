package org.tron.core.services.jsonrpc;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import java.math.BigInteger;
import org.springframework.stereotype.Component;
import org.tron.core.exception.TronException;
import org.tron.core.exception.ItemNotFoundException;

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

  @JsonRpcMethod("eth_blockNumber")
  int getLatestBlockNum();

  @JsonRpcMethod("eth_getBalance")
  long getTrxBalance(String address, String blockNumOrTag) throws ItemNotFoundException;

  @JsonRpcMethod("eth_getStorageAt")
  BigInteger getTrc20Balance(String address, String contractAddress, String blockNumOrTag);

  @JsonRpcMethod("eth_getTransactionCount")
  int getSendTransactionCountOfAddress(String address, String blockNumOrTag);

  @JsonRpcMethod("eth_getCode")
  String getABIofSmartContract(String contractAddress);

  @JsonRpcMethod("eth_syncing")
  Object isSyncing();

  @JsonRpcMethod("eth_coinbase")
  String getCoinbase();

  @JsonRpcMethod("eth_gasPrice")
  String gasPrice();

  @JsonRpcMethod("eth_estimateGas")
  String estimateGas();

  @JsonRpcMethod("eth_getCompilers")
  String[] getCompilers();

  @JsonRpcMethod("eth_compileSolidity")
  String compileSolidity(String source);

  @JsonRpcMethod("eth_gettransactionreceipt")
  String getTransactionReceipt(String source);
}
