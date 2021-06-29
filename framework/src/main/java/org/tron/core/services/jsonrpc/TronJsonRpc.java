package org.tron.core.services.jsonrpc;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.Value;
import org.springframework.stereotype.Component;
import org.tron.core.exception.ItemNotFoundException;

@Component
public interface TronJsonRpc {

  @Value
  @AllArgsConstructor
  @ToString
  class SyncingResult {

    private final String startingBlock;
    private final String currentBlock;
    private final String highestBlock;
  }

  class BlockResult {

    public String number;
    public String hash;
    public String parentHash;
    public String nonce;
    public String sha3Uncles;
    public String logsBloom;
    public String transactionsRoot;
    public String stateRoot;
    public String receiptsRoot;
    public String miner;
    public String difficulty;
    public String totalDifficulty;
    public String extraData;
    public String size;
    public String gasLimit;
    public String gasUsed;
    public String timestamp;
    public Object[] transactions; //TransactionResult or byte32
    public String[] uncles;

    @Override
    public String toString() {
      return "BlockResult{"
          + "number='" + number + '\''
          + ", hash='" + hash + '\''
          + ", parentHash='" + parentHash + '\''
          + ", nonce='" + nonce + '\''
          + ", sha3Uncles='" + sha3Uncles + '\''
          + ", logsBloom='" + logsBloom + '\''
          + ", transactionsRoot='" + transactionsRoot + '\''
          + ", stateRoot='" + stateRoot + '\''
          + ", receiptsRoot='" + receiptsRoot + '\''
          + ", miner='" + miner + '\''
          + ", difficulty='" + difficulty + '\''
          + ", totalDifficulty='" + totalDifficulty + '\''
          + ", extraData='" + extraData + '\''
          + ", size='" + size + '\''
          + ", gas='" + gasLimit + '\''
          + ", gasUsed='" + gasUsed + '\''
          + ", timestamp='" + timestamp + '\''
          + ", transactions=" + Arrays.toString(transactions)
          + ", uncles=" + Arrays.toString(uncles)
          + '}';
    }
  }

  class CallArguments {
    public String from;
    public String to;
    public String gas; //not used
    public String gasPrice; //not used
    public String value; //not used
    public String data;

    @Override
    public String toString() {
      return String.format("{\"from\":\"%s\", \"to\":\"%s\", \"gas\":\"0\", \"gasPrice\":\"0\", "
          + "\"value\":\"0\", \"data\":\"%s\"}", from, to, data);
    }
  }

  class CompilationResult {
    public String code;
    public CompilationInfo info;

    @Override
    public String toString() {
      return "CompilationResult{"
          + "code='" + code + '\''
          + ", info=" + info
          + '}';
    }
  }

  class CompilationInfo {
    public String source;
    public String language;
    public String languageVersion;
    public String compilerVersion;
    // public CallTransaction.Function[] abiDefinition;
    public String userDoc;
    public String developerDoc;

    @Override
    public String toString() {
      return "CompilationInfo{"
          + "source='" + source + '\''
          + ", language='" + language + '\''
          + ", languageVersion='" + languageVersion + '\''
          + ", compilerVersion='" + compilerVersion + '\''
          // + ", abiDefinition=" + abiDefinition + '\''
          + ", userDoc='" + userDoc + '\''
          + ", developerDoc='" + developerDoc + '\''
          + '}';
    }
  }

  @JsonRpcMethod("web3_clientVersion")
  String web3ClientVersion();

  @JsonRpcMethod("web3_sha3")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcApiException.class, code = -32602, data = "{}"),
  })
  String web3Sha3(String data) throws Exception;

  @JsonRpcMethod("eth_getBlockTransactionCountByHash")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcApiException.class, code = -32602, data = "{}"),
  })
  String ethGetBlockTransactionCountByHash(String blockHash) throws Exception;

  @JsonRpcMethod("eth_getBlockTransactionCountByNumber")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcApiException.class, code = -32602, data = "{}"),
  })
  String ethGetBlockTransactionCountByNumber(String bnOrId) throws Exception;

  @JsonRpcMethod("eth_getBlockByHash")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcApiException.class, code = -32602, data = "{}"),
  })
  BlockResult ethGetBlockByHash(String blockHash, Boolean fullTransactionObjects) throws Exception;

  @JsonRpcMethod("eth_getBlockByNumber")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcApiException.class, code = -32602, data = "{}"),
  })
  BlockResult ethGetBlockByNumber(String bnOrId, Boolean fullTransactionObjects) throws Exception;

  @JsonRpcMethod("net_version")
  String getNetVersion();

  @JsonRpcMethod("eth_chainId")
  String ethChainId();

  @JsonRpcMethod("net_listening")
  boolean isListening();

  @JsonRpcMethod("eth_protocolVersion")
  String getProtocolVersion();

  @JsonRpcMethod("eth_blockNumber")
  String getLatestBlockNum();

  @JsonRpcMethod("eth_getBalance")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcApiException.class, code = -32602, data = "{}"),
  })
  String getTrxBalance(String address, String blockNumOrTag) throws ItemNotFoundException;

  @JsonRpcMethod("eth_getStorageAt")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcApiException.class, code = -32602, data = "{}"),
  })
  String getStorageAt(String address, String storageIdx, String blockNumOrTag);

  @JsonRpcMethod("eth_getTransactionCount")
  String getSendTransactionCountOfAddress(String address, String blockNumOrTag);

  @JsonRpcMethod("eth_getCode")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcApiException.class, code = -32602, data = "{}"),
  })
  String getABIofSmartContract(String contractAddress, String bnOrId);

  @JsonRpcMethod("eth_coinbase")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcApiException.class, code = -32602, data = "{}"),
  })
  String getCoinbase() throws Exception;

  @JsonRpcMethod("eth_gasPrice")
  String gasPrice();

  @JsonRpcMethod("eth_estimateGas")
  String estimateGas(CallArguments args);

  @JsonRpcMethod("eth_getCompilers")
  @JsonRpcErrors({
      @JsonRpcError(exception = UnsupportedOperationException.class, code = -32601, data = "{}"),
  })
  String[] getCompilers();

  @JsonRpcMethod("eth_compileSolidity")
  @JsonRpcErrors({
      @JsonRpcError(exception = UnsupportedOperationException.class, code = -32601, data = "{}"),
  })
  CompilationResult ethCompileSolidity(String contract) throws Exception;

  @JsonRpcMethod("eth_compileLLL")
  @JsonRpcErrors({
      @JsonRpcError(exception = UnsupportedOperationException.class, code = -32601, data = "{}"),
  })
  CompilationResult ethCompileLLL(String contract);

  @JsonRpcMethod("eth_compileSerpent")
  @JsonRpcErrors({
      @JsonRpcError(exception = UnsupportedOperationException.class, code = -32601, data = "{}"),
  })
  CompilationResult ethCompileSerpent(String contract);

  @JsonRpcMethod("eth_getTransactionByHash")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcApiException.class, code = -32602, data = "{}"),
  })
  TransactionResult getTransactionByHash(String txid);

  @JsonRpcMethod("eth_getTransactionByBlockHashAndIndex")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcApiException.class, code = -32602, data = "{}"),
  })
  TransactionResult getTransactionByBlockHashAndIndex(String blockHash, String index);

  @JsonRpcMethod("eth_getTransactionByBlockNumberAndIndex")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcApiException.class, code = -32602, data = "{}"),
  })
  TransactionResult getTransactionByBlockNumberAndIndex(String blockNumOrTag, String index);

  @JsonRpcMethod("eth_getTransactionReceipt")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcApiException.class, code = -32602, data = "{}"),
  })
  TransactionReceipt getTransactionReceipt(String txid);

  @JsonRpcMethod("eth_call")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcApiException.class, code = -32602, data = "{}"),
  })
  String getCall(CallArguments transactionCall, String blockNumOrTag);

  @JsonRpcMethod("net_peerCount")
  String getPeerCount();

  @JsonRpcMethod("eth_syncing")
  Object getSyncingStatus();

  @JsonRpcMethod("eth_getUncleByBlockHashAndIndex")
  BlockResult getUncleByBlockHashAndIndex(String blockHash, int index);

  @JsonRpcMethod("eth_getUncleByBlockNumberAndIndex")
  BlockResult getUncleByBlockNumberAndIndex(String blockNumOrTag, int index);

  @JsonRpcMethod("eth_getUncleCountByBlockHash")
  String getUncleCountByBlockHash(String blockHash);

  @JsonRpcMethod("eth_getUncleCountByBlockNumber")
  String getUncleCountByBlockNumber(String blockNumOrTag);

  @JsonRpcMethod("eth_getWork")
  List<Object> ethGetWork();

  @JsonRpcMethod("eth_hashrate")
  String getHashRate();

  @JsonRpcMethod("eth_mining")
  boolean isMining();

  @JsonRpcMethod("eth_accounts")
  String[] getAccounts();

  // not supported
  @JsonRpcMethod("eth_submitWork")
  @JsonRpcErrors({
      @JsonRpcError(exception = UnsupportedOperationException.class, code = -32601, data = "{}"),
  })
  boolean ethSubmitWork(String nonce, String header, String digest);

  @JsonRpcMethod("eth_sendRawTransaction")
  @JsonRpcErrors({
      @JsonRpcError(exception = UnsupportedOperationException.class, code = -32601, data = "{}"),
  })
  String ethSendRawTransaction(String rawData);

  @JsonRpcMethod("eth_sendTransaction")
  @JsonRpcErrors({
      @JsonRpcError(exception = UnsupportedOperationException.class, code = -32601, data = "{}"),
  })
  String ethSendTransaction(CallArguments transactionArgs);

  @JsonRpcMethod("eth_sign")
  @JsonRpcErrors({
      @JsonRpcError(exception = UnsupportedOperationException.class, code = -32601, data = "{}"),
  })
  String ethSign(String addr, String data);

  @JsonRpcMethod("eth_signTransaction")
  @JsonRpcErrors({
      @JsonRpcError(exception = UnsupportedOperationException.class, code = -32601, data = "{}"),
  })
  String ethSignTransaction(CallArguments transactionArgs);

  @JsonRpcMethod("parity_nextNonce")
  @JsonRpcErrors({
      @JsonRpcError(exception = UnsupportedOperationException.class, code = -32601, data = "{}"),
  })
  String parityNextNonce(String address);
}
