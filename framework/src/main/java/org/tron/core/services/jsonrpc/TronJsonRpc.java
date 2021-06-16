package org.tron.core.services.jsonrpc;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.encode58Check;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getToAddress;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getTransactionAmount;

import com.google.protobuf.ByteString;
import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;

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

    //需要采用public修饰符，否则输入参数不能被识别
    /**
     * 用户地址，16进制
     */
    public String from;
    /**
     * 合约地址，16进制
     */
    public String to;
    public String gas; //not used
    public String gasPrice; //not used
    public String value; //not used
    /**
     * 函数的签名 || 输入参数列表
     */
    public String data;

    @Override
    public String toString() {
      return String.format("{\"from\":\"%s\", \"to\":\"%s\", \"gas\":\"0\", \"gasPrice\":\"0\", "
          + "\"value\":\"0\", \"data\":\"%s\"}", from, to, data);
    }
  }

  class TransactionReceipt {

    public static class TransactionLog {

      public String logIndex;
      public String blockHash;
      public String blockNumber;
      public String transactionIndex;
      public String transactionHash;
      public String address;
      public String addressBase58;
      public String data;
      public String[] topics;

      public TransactionLog() {

      }
    }

    public String blockHash;
    public String blockNumber;
    public String transactionIndex;
    public String transactionHash;
    public String from;
    public String fromBase58;
    public String to;
    public String toBase58;

    public String cumulativeGasUsed;
    public String gasUsed;
    public String contractAddress;
    public String contractAddressBase58;
    public TransactionLog[] logs;
    public String logsBloom;

    public TransactionReceipt() {

    }
  }

  class TransactionResult {

    public String hash;
    public String nonce;
    public String blockHash;
    public String blockNumber;
    public String transactionIndex;

    public String from;
    public String fromBase58;
    public String to;
    public String toBase58;
    public String gas;
    public String gasPrice;
    public String value;
    public String input;

    public String v;
    public String r;
    public String s;

    public TransactionResult(Block b, int index, Transaction tx, Wallet wallet) {
      BlockCapsule blockCapsule = new BlockCapsule(b);

      byte[] txid = new TransactionCapsule(tx).getTransactionId().getBytes();
      hash = ByteArray.toJsonHex(txid);
      nonce = null; // no value
      blockHash = ByteArray.toJsonHex(blockCapsule.getBlockId().getBytes());
      blockNumber = ByteArray.toJsonHex(blockCapsule.getNum());
      transactionIndex = ByteArray.toJsonHex(index);

      if (!tx.getRawData().getContractList().isEmpty()) {
        Contract contract = tx.getRawData().getContract(0);
        byte[] fromByte = TransactionCapsule.getOwner(contract);
        byte[] toByte = getToAddress(tx);
        from = ByteArray.toJsonHex(fromByte);
        to = ByteArray.toJsonHex(toByte);
        fromBase58 = encode58Check(fromByte);
        toBase58 = encode58Check(toByte);
        value = ByteArray.toJsonHex(getTransactionAmount(contract, hash, wallet));
      } else {
        from = "";
        to = "";
        fromBase58 = "";
        toBase58 = "";
        value = "";
      }

      gas = "";
      gasPrice = "";
      input = "";

      ByteString signature = tx.getSignature(0); // r[32] + s[32] + 符号位v[1]
      byte[] signData = signature.toByteArray();
      byte vByte = (byte) (signData[64] + 27); //参考函数 Base64toBytes
      byte[] rByte = Arrays.copyOfRange(signData, 0, 32);
      byte[] sByte = Arrays.copyOfRange(signData, 32, 64);
      v = ByteArray.toJsonHex(vByte);
      r = ByteArray.toJsonHex(rByte);
      s = ByteArray.toJsonHex(sByte);
    }

    @Override
    public String toString() {
      return "TransactionResult{"
          + "hash='" + hash + '\''
          + ", nonce='" + nonce + '\''
          + ", blockHash='" + blockHash + '\''
          + ", blockNumber='" + blockNumber + '\''
          + ", transactionIndex='" + transactionIndex + '\''
          + ", from='" + from + '\''
          + ", to='" + to + '\''
          + ", gas='" + gas + '\''
          + ", gasPrice='" + gasPrice + '\''
          + ", value='" + value + '\''
          + ", input='" + input + '\''
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
  @JsonRpcErrors({
      @JsonRpcError(exception=JsonRpcApiException.class, code=-32602, data="no data"),
  })
  BlockResult ethGetBlockByHash(String blockHash, Boolean fullTransactionObjects) throws Exception;

  @JsonRpcMethod("eth_getBlockByNumber")
  BlockResult ethGetBlockByNumber(String bnOrId, Boolean fullTransactionObjects) throws Exception;

  @JsonRpcMethod("net_version")
  String getNetVersion();

  @JsonRpcMethod("net_listening")
  boolean isListening();

  @JsonRpcMethod("eth_protocolVersion")
  String getProtocolVersion();

  @JsonRpcMethod("eth_blockNumber")
  String getLatestBlockNum();

  @JsonRpcMethod("eth_getBalance")
  String getTrxBalance(String address, String blockNumOrTag) throws ItemNotFoundException;

  @JsonRpcMethod("eth_getStorageAt")
  String getTrc20Balance(String address, String contractAddress, String blockNumOrTag);

  @JsonRpcMethod("eth_getTransactionCount")
  String getSendTransactionCountOfAddress(String address, String blockNumOrTag);

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
  TransactionResult getTransactionByHash(String txid);

  @JsonRpcMethod("eth_getTransactionByBlockHashAndIndex")
  TransactionResult getTransactionByBlockHashAndIndex(String blockHash, int index);

  @JsonRpcMethod("eth_getTransactionByBlockNumberAndIndex")
  TransactionResult getTransactionByBlockNumberAndIndex(int blockNum, int index);

  @JsonRpcMethod("eth_gettransactionreceipt")
  TransactionReceipt getTransactionReceipt(String txid);

  @JsonRpcMethod("eth_call")
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

  @JsonRpcMethod("eth_hashrate")
  String getHashRate();

  @JsonRpcMethod("eth_mining")
  boolean isMining();

}
