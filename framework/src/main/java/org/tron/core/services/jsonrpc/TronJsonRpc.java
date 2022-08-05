package org.tron.core.services.jsonrpc;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.stereotype.Component;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteArray;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.JsonRpcInternalException;
import org.tron.core.exception.JsonRpcInvalidParamsException;
import org.tron.core.exception.JsonRpcInvalidRequestException;
import org.tron.core.exception.JsonRpcMethodNotFoundException;
import org.tron.core.exception.JsonRpcTooManyResultException;
import org.tron.core.services.jsonrpc.types.BlockResult;
import org.tron.core.services.jsonrpc.types.BuildArguments;
import org.tron.core.services.jsonrpc.types.CallArguments;
import org.tron.core.services.jsonrpc.types.TransactionReceipt;
import org.tron.core.services.jsonrpc.types.TransactionResult;

@Component
public interface TronJsonRpc {

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
  String getNetVersion() throws JsonRpcInternalException;

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
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32000, data = "{}"),
  })
  String getCoinbase() throws JsonRpcInternalException;

  @JsonRpcMethod("eth_gasPrice")
  String gasPrice();

  @JsonRpcMethod("eth_estimateGas")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidRequestException.class, code = -32600, data = "{}"),
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32000, data = "{}"),
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
      @JsonRpcError(exception = JsonRpcInvalidRequestException.class, code = -32600, data = "{}"),
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32000, data = "{}"),
  })
  String getCall(CallArguments transactionCall, String blockNumOrTag)
      throws JsonRpcInvalidParamsException, JsonRpcInvalidRequestException,
      JsonRpcInternalException;

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
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcInternalException.class, code = -32000, data = "{}"),
  })
  TransactionJson buildTransaction(BuildArguments args)
      throws JsonRpcInvalidParamsException, JsonRpcInvalidRequestException,
      JsonRpcInternalException, JsonRpcMethodNotFoundException;

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

  // filter
  @JsonRpcMethod("eth_newFilter")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
  })
  String newFilter(FilterRequest fr) throws JsonRpcInvalidParamsException,
      JsonRpcMethodNotFoundException;

  @JsonRpcMethod("eth_newBlockFilter")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
  })
  String newBlockFilter() throws JsonRpcMethodNotFoundException;

  @JsonRpcMethod("eth_uninstallFilter")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = ItemNotFoundException.class, code = -32000, data = "{}"),
  })
  boolean uninstallFilter(String filterId) throws JsonRpcInvalidParamsException,
      JsonRpcMethodNotFoundException, ItemNotFoundException;

  @JsonRpcMethod("eth_getFilterChanges")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = ItemNotFoundException.class, code = -32000, data = "{}"),
  })
  Object[] getFilterChanges(String filterId)
      throws JsonRpcInvalidParamsException, IOException, ExecutionException, InterruptedException,
      JsonRpcMethodNotFoundException, ItemNotFoundException;

  @JsonRpcMethod("eth_getLogs")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
      @JsonRpcError(exception = JsonRpcTooManyResultException.class, code = -32005, data = "{}"),
      @JsonRpcError(exception = BadItemException.class, code = -32000, data = "{}"),
      @JsonRpcError(exception = ExecutionException.class, code = -32000, data = "{}"),
      @JsonRpcError(exception = InterruptedException.class, code = -32000, data = "{}"),
      @JsonRpcError(exception = ItemNotFoundException.class, code = -32000, data = "{}"),
  })
  LogFilterElement[] getLogs(FilterRequest fr) throws JsonRpcInvalidParamsException,
      ExecutionException, InterruptedException, BadItemException, ItemNotFoundException,
      JsonRpcMethodNotFoundException, JsonRpcTooManyResultException;

  @JsonRpcMethod("eth_getFilterLogs")
  @JsonRpcErrors({
      @JsonRpcError(exception = JsonRpcInvalidParamsException.class, code = -32602, data = "{}"),
      @JsonRpcError(exception = JsonRpcMethodNotFoundException.class, code = -32601, data = "{}"),
      @JsonRpcError(exception = JsonRpcTooManyResultException.class, code = -32005, data = "{}"),
      @JsonRpcError(exception = BadItemException.class, code = -32000, data = "{}"),
      @JsonRpcError(exception = ExecutionException.class, code = -32000, data = "{}"),
      @JsonRpcError(exception = InterruptedException.class, code = -32000, data = "{}"),
      @JsonRpcError(exception = ItemNotFoundException.class, code = -32000, data = "{}"),
  })
  LogFilterElement[] getFilterLogs(String filterId) throws JsonRpcInvalidParamsException,
      ExecutionException, InterruptedException, BadItemException, ItemNotFoundException,
      JsonRpcMethodNotFoundException, JsonRpcTooManyResultException;

  @AllArgsConstructor
  @ToString
  class SyncingResult {

    @Getter
    private final String startingBlock;
    @Getter
    private final String currentBlock;
    @Getter
    private final String highestBlock;
  }

  @ToString
  class CompilationResult {

    @Getter
    private String code;
    @Getter
    private CompilationInfo info;
  }

  @ToString
  class CompilationInfo {

    @Getter
    private String source;
    @Getter
    private String language;
    @Getter
    private String languageVersion;
    @Getter
    private String compilerVersion;
    @Getter
    private String userDoc;
    @Getter
    private String developerDoc;

  }

  class TransactionJson {

    @Getter
    @Setter
    private JSONObject transaction;
  }

  /**
   * FILTER OBJECT
   * <li> address [optional]
   * - a contract address or a list of addresses from which logs should originate.
   * <li> fromBlock [optional, default is "latest"]
   * - an integer block number, or the string "latest", "earliest" or "pending"
   * <li> toBlock [optional, default is "latest"]
   * - an integer block number, or the string "latest", "earliest" or "pending"
   * <li> topics[optional] - Array of 32 Bytes DATA topics. Topics are order-dependent.
   * <br>
   * <br> A note on specifying topic filters: Topics are order-dependent.
   * A transaction with a log with topics [A, B] will be matched by the following topic filters:
   *
   * <li> [] - anything"
   * <li> [A] - A in first position (and anything after)
   * <li> [null, B] - anything in first position AND B in second position (and anything after)
   * <li> [A, B] - A in first position AND B in second position (and anything after)"
   * <li> [[A, B], [A, B]] - (A OR B) in first position AND (A OR B) in second position (and
   * anything after)
   *
   * <br> Filter IDs will be valid for up to fifteen minutes, and can polled by any connection using
   * the same v3 project ID.
   */
  @NoArgsConstructor
  @AllArgsConstructor
  class FilterRequest {

    @Getter
    @Setter
    private String fromBlock;
    @Getter
    @Setter
    private String toBlock;
    @Getter
    @Setter
    private Object address;
    @Getter
    @Setter
    private Object[] topics;
    @Getter
    @Setter
    private String blockHash;  // EIP-234: makes fromBlock = toBlock = blockHash

  }

  @JsonPropertyOrder(alphabetic = true)
  class LogFilterElement {

    @Getter
    private final String logIndex;
    @Getter
    private final String transactionIndex;
    @Getter
    private final String transactionHash;
    @Getter
    private final String blockHash;
    @Getter
    private final String blockNumber;
    @Getter
    private final String address;
    @Getter
    private final String data;
    @Getter
    private final String[] topics;
    @Getter
    private final boolean removed;

    public LogFilterElement(String blockHash, Long blockNum, String txId, Integer txIndex,
        String contractAddress, List<DataWord> topicList, String logData, int logIdx,
        boolean removed) {
      logIndex = ByteArray.toJsonHex(logIdx);
      this.blockNumber = blockNum == null ? null : ByteArray.toJsonHex(blockNum);
      this.blockHash = blockHash == null ? null : ByteArray.toJsonHex(blockHash);
      transactionIndex = txIndex == null ? null : ByteArray.toJsonHex(txIndex);
      transactionHash = ByteArray.toJsonHex(txId);
      address = ByteArray.toJsonHex(contractAddress);
      data = logData == null ? "0x" : ByteArray.toJsonHex(logData);
      topics = new String[topicList.size()];
      for (int i = 0; i < topics.length; i++) {
        topics[i] = ByteArray.toJsonHex(topicList.get(i).getData());
      }
      this.removed = removed;
    }
  }
}
