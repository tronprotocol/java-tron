package org.tron.core.services.jsonrpc.types;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.convertToTronAddress;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getToAddress;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.tron.api.GrpcAPI.TransactionInfoList;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.ResourceReceipt;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.TransactionInfo;

@JsonPropertyOrder(alphabetic = true)
public class TransactionReceipt {

  @JsonPropertyOrder(alphabetic = true)
  public static class TransactionLog {

    @Getter
    @Setter
    private String logIndex;
    @Getter
    @Setter
    private String blockHash;
    @Getter
    @Setter
    private String blockNumber;
    @Getter
    @Setter
    private String transactionIndex;
    @Getter
    @Setter
    private String transactionHash;
    @Getter
    @Setter
    private String address;
    @Getter
    @Setter
    private String data;
    @Getter
    @Setter
    private String[] topics;
    @Getter
    @Setter
    private boolean removed = false;

    public TransactionLog() {
    }
  }

  @Getter
  @Setter
  private String blockHash;
  @Getter
  @Setter
  private String blockNumber;
  @Getter
  @Setter
  private String transactionIndex;
  @Getter
  @Setter
  private String transactionHash;
  @Getter
  @Setter
  private String from;
  @Getter
  @Setter
  private String to;

  @Getter
  @Setter
  private String cumulativeGasUsed;
  @Getter
  @Setter
  private String effectiveGasPrice;
  @Getter
  @Setter
  private String gasUsed;
  @Getter
  @Setter
  private String contractAddress;
  @Getter
  @Setter
  private TransactionLog[] logs;
  @Getter
  @Setter
  private String logsBloom;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String root;  // 32 bytes of post-transaction stateroot (pre Byzantium)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String status;  //  either 1 (success) or 0 (failure) (post Byzantium)

  @Getter
  @Setter
  private String type = "0x0";

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
        status = resourceReceipt.getResultValue() <= 1 ? "0x1" : "0x0";

        transaction = block.getTransactions(index);
        break;
      } else {
        cumulativeLogCount += info.getLogCount();
      }
    }

    blockHash = ByteArray.toJsonHex(blockCapsule.getBlockId().getBytes());
    blockNumber = ByteArray.toJsonHex(blockCapsule.getNum());
    transactionHash = ByteArray.toJsonHex(txInfo.getId().toByteArray());
    effectiveGasPrice = ByteArray.toJsonHex(wallet.getEnergyFee(blockCapsule.getTimeStamp()));

    from = null;
    to = null;
    contractAddress = null;

    if (transaction != null && !transaction.getRawData().getContractList().isEmpty()) {
      Contract contract = transaction.getRawData().getContract(0);
      byte[] fromByte = TransactionCapsule.getOwner(contract);
      byte[] toByte = getToAddress(transaction);
      from = ByteArray.toJsonHexAddress(fromByte);
      to = ByteArray.toJsonHexAddress(toByte);

      if (contract.getType() == ContractType.CreateSmartContract) {
        contractAddress = ByteArray.toJsonHexAddress(txInfo.getContractAddress().toByteArray());
      }
    }

    // logs
    List<TransactionLog> logList = new ArrayList<>();
    for (int index = 0; index < txInfo.getLogCount(); index++) {
      TransactionInfo.Log log = txInfo.getLogList().get(index);

      TransactionReceipt.TransactionLog transactionLog = new TransactionReceipt.TransactionLog();
      // index is the index in the block
      transactionLog.logIndex = ByteArray.toJsonHex(index + cumulativeLogCount);
      transactionLog.transactionHash = transactionHash;
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
    logsBloom = ByteArray.toJsonHex(new byte[256]); // no value
    root = null;
  }
}