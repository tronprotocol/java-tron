package org.tron.core.services.jsonrpc;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import java.math.BigInteger;
import java.util.Arrays;
import org.springframework.stereotype.Component;
import org.tron.core.exception.ItemNotFoundException;

@Component
public interface TestService {

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
    public Object[] transactions; //TransactionResultDTO or byte32
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

  @JsonRpcMethod("web3_clientVersion")
  String web3ClientVersion();

  @JsonRpcMethod("web3_sha3")
  String web3Sha3(String data) throws Exception;

  @JsonRpcMethod("eth_getBlockTransactionCountByHash")
  String ethGetBlockTransactionCountByHash(String blockHash) throws Exception;

  @JsonRpcMethod("eth_getBlockTransactionCountByNumber")
  String ethGetBlockTransactionCountByNumber(String bnOrId) throws Exception;

  @JsonRpcMethod("eth_getBlockByHash")
  BlockResult ethGetBlockByHash(String blockHash, Boolean fullTransactionObjects) throws Exception;

  @JsonRpcMethod("eth_getBlockByNumber")
  BlockResult ethGetBlockByNumber(String bnOrId, Boolean fullTransactionObjects) throws Exception;

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
  long getSendTransactionCountOfAddress(String address, String blockNumOrTag);

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

  @JsonRpcMethod("eth_getTransactionByHash")
  TransactionResultDTO getTransactionByHash(String txid);

  @JsonRpcMethod("eth_getTransactionByBlockHashAndIndex")
  TransactionResultDTO getTransactionByBlockHashAndIndex(String blockHash, int index);

  @JsonRpcMethod("eth_getTransactionByBlockNumberAndIndex")
  TransactionResultDTO getTransactionByBlockNumberAndIndex(int blockNum, int index);

  @JsonRpcMethod("eth_gettransactionreceipt")
  TransactionReceipt getTransactionReceipt(String txid);

  @JsonRpcMethod("eth_call")
  String getCall(TransactionCall transactionCall, String blockNumOrTag);

  @JsonRpcMethod("net_peerCount")
  int getPeerCount();

  @JsonRpcMethod("eth_syncing")
  Object getSyncingStatus();

  @JsonRpcMethod("eth_getUncleByBlockHashAndIndex")
  BlockResult getUncleByBlockHashAndIndex(String blockHash, int index);

  @JsonRpcMethod("eth_getUncleByBlockNumberAndIndex")
  BlockResult getUncleByBlockNumberAndIndex(String blockNumOrTag, int index);

  @JsonRpcMethod("eth_getUncleCountByBlockHash")
  int getUncleCountByBlockHash(String blockHash);

  @JsonRpcMethod("eth_getUncleCountByBlockNumber")
  int getUncleCountByBlockNumber(String blockNumOrTag);

  @JsonRpcMethod("eth_hashrate")
  int getHashRate();

  @JsonRpcMethod("eth_mining")
  boolean isMining();

}
