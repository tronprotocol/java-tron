package org.tron.core.services.jsonrpc;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.convertToTronAddress;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getToAddress;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getTransactionAmount;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.protobuf.ByteString;
import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.Value;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.TransactionInfoList;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.ResourceReceipt;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.TransactionInfo;

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
    public String to;

    public String cumulativeGasUsed;
    public String gasUsed;
    public String contractAddress;
    public TransactionLog[] logs;
    public String logsBloom;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String root;  // 32 bytes of post-transaction stateroot (pre Byzantium)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String status;  //  either 1 (success) or 0 (failure) (post Byzantium)

    public TransactionReceipt(Block block, TransactionInfo txInfo, Wallet wallet) {
      BlockCapsule blockCapsule = new BlockCapsule(block);
      String txid = ByteArray.toHexString(txInfo.getId().toByteArray());

      Transaction transaction = null;
      long cumulativeGas = 0;
      long cumulativeLogCount = 0;

      long sunPerEnergy = Constant.SUN_PER_ENERGY;
      long dynamicEnergyFee = wallet.getEnergyFee();
      if (dynamicEnergyFee > 0) {
        sunPerEnergy = dynamicEnergyFee;
      }

      TransactionInfoList infoList = wallet.getTransactionInfoByBlockNum(blockCapsule.getNum());
      for (int index = 0; index < infoList.getTransactionInfoCount(); index++) {
        TransactionInfo info = infoList.getTransactionInfo(index);
        ResourceReceipt resourceReceipt = info.getReceipt();

        long energyUsage = resourceReceipt.getEnergyUsage()
            + resourceReceipt.getOriginEnergyUsage()
            + resourceReceipt.getEnergyFee() / sunPerEnergy;
        cumulativeGas += energyUsage;

        if (ByteArray.toHexString(info.getId().toByteArray()).equals(txid)) {
          transactionIndex = ByteArray.toJsonHex(index);
          cumulativeGasUsed = ByteArray.toJsonHex(cumulativeGas);
          gasUsed = ByteArray.toJsonHex(energyUsage);
          status = resourceReceipt.getResultValue() == 1 ? "0x1" : "0x0";

          transaction = block.getTransactions(index);
          break;
        } else {
          cumulativeLogCount += info.getLogCount();
        }
      }

      blockHash = ByteArray.toJsonHex(blockCapsule.getBlockId().getBytes());
      blockNumber = ByteArray.toJsonHex(blockCapsule.getNum());
      transactionHash = ByteArray.toJsonHex(txInfo.getId().toByteArray());

      if (transaction != null && !transaction.getRawData().getContractList().isEmpty()) {
        Contract contract = transaction.getRawData().getContract(0);
        byte[] fromByte = TransactionCapsule.getOwner(contract);
        byte[] toByte = getToAddress(transaction);
        from = ByteArray.toJsonHexAddress(fromByte);
        to = ByteArray.toJsonHexAddress(toByte);
      } else {
        from = null;
        to = null;
      }

      contractAddress = ByteArray.toJsonHexAddress(txInfo.getContractAddress().toByteArray());

      // 统一的log
      List<TransactionLog> logList = new ArrayList<>();
      for (int index = 0; index < txInfo.getLogCount(); index++) {
        TransactionInfo.Log log = txInfo.getLogList().get(index);

        TransactionReceipt.TransactionLog transactionLog = new TransactionReceipt.TransactionLog();
        // log的index为在这个block中的index
        transactionLog.logIndex = ByteArray.toJsonHex(index + cumulativeLogCount);
        transactionLog.transactionHash = txid;
        transactionLog.transactionIndex = transactionIndex;
        transactionLog.blockHash = blockHash;
        transactionLog.blockNumber = blockNumber;
        byte[] addressByte = convertToTronAddress(log.getAddress().toByteArray());
        transactionLog.address = ByteArray.toJsonHexAddress(addressByte);
        transactionLog.data = ByteArray.toJsonHex(log.getData().toByteArray());
        String[] topics = new String[log.getTopicsCount()];
        for (int i = 0; i < log.getTopicsCount(); i++) {
          topics[i] = ByteArray.toJsonHex(log.getTopics(i).toByteArray());
        }
        transactionLog.topics = topics;

        logList.add(transactionLog);
      }
      logs = logList.toArray(new TransactionReceipt.TransactionLog[logList.size()]);
      logsBloom = null; // no value

      root = null;
    }
  }

  class TransactionResult {

    public String hash;
    public String nonce;
    public String blockHash;
    public String blockNumber;
    public String transactionIndex;

    public String from;
    public String to;
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
        from = ByteArray.toJsonHexAddress(fromByte);
        to = ByteArray.toJsonHexAddress(toByte);
        value = ByteArray.toJsonHex(getTransactionAmount(contract, hash, wallet));
      } else {
        from = null;
        to = null;
        value = null;
      }

      gas = null; // no value
      gasPrice = null; // no value
      input = null; // no value

      ByteString signature = tx.getSignature(0); // r[32] + s[32] + v[1]
      byte[] signData = signature.toByteArray();
      byte vByte = (byte) (signData[64] + 27); // according to Base64toBytes
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
