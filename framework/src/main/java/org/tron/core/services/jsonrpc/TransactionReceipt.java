package org.tron.core.services.jsonrpc;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.convertToTronAddress;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getToAddress;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import org.tron.api.GrpcAPI.TransactionInfoList;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.ResourceReceipt;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.TransactionInfo;

public class TransactionReceipt {

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

  public TransactionReceipt(Protocol.Block block, TransactionInfo txInfo, Wallet wallet) {
    BlockCapsule blockCapsule = new BlockCapsule(block);
    String txid = ByteArray.toHexString(txInfo.getId().toByteArray());
    long blockNum = blockCapsule.getNum();

    Protocol.Transaction transaction = null;
    long cumulativeGas = 0;
    long cumulativeLogCount = 0;

    TransactionInfoList infoList = wallet.getTransactionInfoByBlockNum(blockNum);
    for (int index = 0; index < infoList.getTransactionInfoCount(); index++) {
      TransactionInfo info = infoList.getTransactionInfo(index);
      ResourceReceipt resourceReceipt = info.getReceipt();

      long energyUsage = resourceReceipt.getEnergyUsageTotal();
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

    // logs
    List<TransactionLog> logList = new ArrayList<>();
    for (int index = 0; index < txInfo.getLogCount(); index++) {
      TransactionInfo.Log log = txInfo.getLogList().get(index);

      TransactionReceipt.TransactionLog transactionLog = new TransactionReceipt.TransactionLog();
      // index is the index in the block
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