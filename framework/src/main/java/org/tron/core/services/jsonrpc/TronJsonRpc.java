package org.tron.core.services.jsonrpc;

import com.alibaba.fastjson.JSONObject;
import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.Value;
import org.springframework.stereotype.Component;
import org.tron.core.exception.JsonRpcInternalException;
import org.tron.core.exception.JsonRpcInvalidParamsException;
import org.tron.core.exception.JsonRpcInvalidRequestException;
import org.tron.core.exception.JsonRpcMethodNotFoundException;

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

  class TransactionJson {

    public JSONObject transaction;
  }

  @JsonRpcMethod("web3_clientVersion")
  String web3ClientVersion();

  @JsonRpcMethod("web3_sha3")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  String web3Sha3(String data) throws JsonRpcInvalidParamsException;

  @JsonRpcMethod("eth_getBlockTransactionCountByHash")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  String ethGetBlockTransactionCountByHash(String blockHash) throws JsonRpcInvalidParamsException;

  @JsonRpcMethod("eth_getBlockTransactionCountByNumber")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  String ethGetBlockTransactionCountByNumber(String bnOrId) throws JsonRpcInvalidParamsException;

  @JsonRpcMethod("eth_getBlockByHash")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  BlockResult ethGetBlockByHash(String blockHash, Boolean fullTransactionObjects)
      throws JsonRpcInvalidParamsException;

  @JsonRpcMethod("eth_getBlockByNumber")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  BlockResult ethGetBlockByNumber(String bnOrId, Boolean fullTransactionObjects)
      throws JsonRpcInvalidParamsException;

  @JsonRpcMethod("net_version")
  String getNetVersion();

  @JsonRpcMethod("eth_chainId")
  String ethChainId() throws JsonRpcInternalException;

  @JsonRpcMethod("net_listening")
  boolean isListening();

  @JsonRpcMethod("eth_protocolVersion")
  String getProtocolVersion();

  @JsonRpcMethod("eth_blockNumber")
  String getLatestBlockNum();

  @JsonRpcMethod("eth_getBalance")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  String getTrxBalance(String address, String blockNumOrTag) throws JsonRpcInvalidParamsException;

  @JsonRpcMethod("eth_getStorageAt")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  String getStorageAt(String address, String storageIdx, String blockNumOrTag)
      throws JsonRpcInvalidParamsException;

  @JsonRpcMethod("eth_getCode")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  String getABIOfSmartContract(String contractAddress, String bnOrId)
      throws JsonRpcInvalidParamsException;

  @JsonRpcMethod("eth_coinbase")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  String getCoinbase() throws JsonRpcInternalException;

  @JsonRpcMethod("eth_gasPrice")
  String gasPrice();

  @JsonRpcMethod("eth_estimateGas")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidRequestException.class, code = -32600, data = "{}"),
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32603, data = "{}"),
  })
  String estimateGas(CallArguments args) throws JsonRpcInvalidRequestException,
      JsonRpcInvalidParamsException, JsonRpcInternalException;

  @JsonRpcMethod("eth_getTransactionByHash")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  TransactionResult getTransactionByHash(String txId) throws JsonRpcInvalidParamsException;

  @JsonRpcMethod("eth_getTransactionByBlockHashAndIndex")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  TransactionResult getTransactionByBlockHashAndIndex(String blockHash, String index)
      throws JsonRpcInvalidParamsException;

  @JsonRpcMethod("eth_getTransactionByBlockNumberAndIndex")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  TransactionResult getTransactionByBlockNumberAndIndex(String blockNumOrTag, String index)
      throws JsonRpcInvalidParamsException;

  @JsonRpcMethod("eth_getTransactionReceipt")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  TransactionReceipt getTransactionReceipt(String txid) throws JsonRpcInvalidParamsException;

  @JsonRpcMethod("eth_call")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  String getCall(CallArguments transactionCall, String blockNumOrTag)
      throws JsonRpcInvalidParamsException;

  @JsonRpcMethod("net_peerCount")
  String getPeerCount();

  @JsonRpcMethod("eth_syncing")
  Object getSyncingStatus();

  @JsonRpcMethod("eth_getUncleByBlockHashAndIndex")
  BlockResult getUncleByBlockHashAndIndex(String blockHash, String index);

  @JsonRpcMethod("eth_getUncleByBlockNumberAndIndex")
  BlockResult getUncleByBlockNumberAndIndex(String blockNumOrTag, String index);

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

  @JsonRpcMethod("buildTransaction")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidRequestException.class, code = -32600, data = "{}"),
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32603, data = "{}"),
  })
  TransactionJson buildTransaction(BuildArguments args)
      throws JsonRpcInvalidParamsException, JsonRpcInvalidRequestException,
      JsonRpcInternalException;

  // not supported
  @JsonRpcMethod("eth_submitWork")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
  })
  boolean ethSubmitWork(String nonce, String header, String digest)
      throws JsonRpcMethodNotFoundException;

  @JsonRpcMethod("eth_sendRawTransaction")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
  })
  String ethSendRawTransaction(String rawData) throws JsonRpcMethodNotFoundException;

  @JsonRpcMethod("eth_sendTransaction")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
  })
  String ethSendTransaction(CallArguments transactionArgs) throws JsonRpcMethodNotFoundException;

  @JsonRpcMethod("eth_sign")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
  })
  String ethSign(String addr, String data) throws JsonRpcMethodNotFoundException;

  @JsonRpcMethod("eth_signTransaction")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
  })
  String ethSignTransaction(CallArguments transactionArgs) throws JsonRpcMethodNotFoundException;

  @JsonRpcMethod("parity_nextNonce")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
  })
  String parityNextNonce(String address) throws JsonRpcMethodNotFoundException;

  @JsonRpcMethod("eth_getTransactionCount")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
  })
  String getSendTransactionCountOfAddress(String address, String blockNumOrTag)
      throws JsonRpcMethodNotFoundException;

  @JsonRpcMethod("eth_getCompilers")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
  })
  String[] getCompilers() throws JsonRpcMethodNotFoundException;

  @JsonRpcMethod("eth_compileSolidity")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
  })
  CompilationResult ethCompileSolidity(String contract) throws JsonRpcMethodNotFoundException;

  @JsonRpcMethod("eth_compileLLL")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
  })
  CompilationResult ethCompileLLL(String contract) throws JsonRpcMethodNotFoundException;

  @JsonRpcMethod("eth_compileSerpent")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
  })
  CompilationResult ethCompileSerpent(String contract) throws JsonRpcMethodNotFoundException;

  @JsonRpcMethod("eth_submitHashrate")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
  })
  CompilationResult ethSubmitHashrate(String hashrate, String id)
      throws JsonRpcMethodNotFoundException;
}
