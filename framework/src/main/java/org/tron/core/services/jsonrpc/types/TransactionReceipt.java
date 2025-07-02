package org.tron.core.services.jsonrpc.types;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.convertToTronAddress;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getToAddress;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.TransactionInfo;

@Getter
@Setter
@JsonPropertyOrder(alphabetic = true)
public class TransactionReceipt {
  @JsonPropertyOrder(alphabetic = true)
  @Getter
  @Setter
  public static class TransactionLog {

    private String logIndex;
    private String blockHash;
    private String blockNumber;
    private String transactionIndex;
    private String transactionHash;
    private String address;
    private String data;
    private String[] topics;
    private boolean removed = false;

    public TransactionLog() {}
  }

  private String blockHash;
  private String blockNumber;
  private String transactionIndex;
  private String transactionHash;
  private String from;
  private String to;

  private String cumulativeGasUsed;
  private String effectiveGasPrice;
  private String gasUsed;
  private String contractAddress;
  private TransactionLog[] logs;
  private String logsBloom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String root; // 32 bytes of post-transaction stateroot (pre Byzantium)

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String status; //  either 1 (success) or 0 (failure) (post Byzantium)

  private String type = "0x0";

  /**
   * Constructor for creating a TransactionReceipt
   *
   * @param blockCapsule the block containing the transaction
   * @param txInfo the transaction info containing execution details
   * @param context the pre-calculated transaction context
   * @param energyFee the energy price at the block timestamp
   */
  public TransactionReceipt(
      BlockCapsule blockCapsule,
      TransactionInfo txInfo,
      TransactionContext context,
      long energyFee) {
    // Set basic fields
    this.blockHash = ByteArray.toJsonHex(blockCapsule.getBlockId().getBytes());
    this.blockNumber = ByteArray.toJsonHex(blockCapsule.getNum());
    this.transactionHash = ByteArray.toJsonHex(txInfo.getId().toByteArray());
    this.transactionIndex = ByteArray.toJsonHex(context.index);
    // Compute cumulativeGasTillTxn
    this.cumulativeGasUsed =
        ByteArray.toJsonHex(context.cumulativeGas + txInfo.getReceipt().getEnergyUsageTotal());
    this.gasUsed = ByteArray.toJsonHex(txInfo.getReceipt().getEnergyUsageTotal());
    this.status = txInfo.getReceipt().getResultValue() <= 1 ? "0x1" : "0x0";
    this.effectiveGasPrice = ByteArray.toJsonHex(energyFee);

    // Set contract fields
    this.from = null;
    this.to = null;
    this.contractAddress = null;

    TransactionCapsule txCapsule = blockCapsule.getTransactions().get(context.index);
    Protocol.Transaction transaction = txCapsule.getInstance();
    if (!transaction.getRawData().getContractList().isEmpty()) {
      Contract contract = transaction.getRawData().getContract(0);
      byte[] fromByte = TransactionCapsule.getOwner(contract);
      byte[] toByte = getToAddress(transaction);
      this.from = ByteArray.toJsonHexAddress(fromByte);
      this.to = ByteArray.toJsonHexAddress(toByte);

      if (contract.getType() == ContractType.CreateSmartContract) {
        this.contractAddress =
            ByteArray.toJsonHexAddress(txInfo.getContractAddress().toByteArray());
      }
    }

    // Set logs
    List<TransactionLog> logList = new ArrayList<>();
    for (int logIndex = 0; logIndex < txInfo.getLogCount(); logIndex++) {
      TransactionInfo.Log log = txInfo.getLogList().get(logIndex);
      TransactionLog transactionLog = new TransactionLog();
      transactionLog.setLogIndex(ByteArray.toJsonHex(logIndex + context.cumulativeLogCount));
      transactionLog.setTransactionHash(this.transactionHash);
      transactionLog.setTransactionIndex(this.transactionIndex);
      transactionLog.setBlockHash(this.blockHash);
      transactionLog.setBlockNumber(this.blockNumber);

      byte[] addressByte = convertToTronAddress(log.getAddress().toByteArray());
      transactionLog.setAddress(ByteArray.toJsonHexAddress(addressByte));
      transactionLog.setData(ByteArray.toJsonHex(log.getData().toByteArray()));

      String[] topics = new String[log.getTopicsCount()];
      for (int i = 0; i < log.getTopicsCount(); i++) {
        topics[i] = ByteArray.toJsonHex(log.getTopics(i).toByteArray());
      }
      transactionLog.setTopics(topics);

      logList.add(transactionLog);
    }
    this.logs = logList.toArray(new TransactionLog[0]);

    // Set default fields
    this.logsBloom = ByteArray.toJsonHex(new byte[256]); // no value
    this.root = null;
    this.type = "0x0";
  }

  /**
   * Context class to hold transaction creation parameters Contains index and cumulative values
   * needed for receipt creation
   */
  @Getter
  public static class TransactionContext {
    private final int index;
    private final long cumulativeGas;
    private final long cumulativeLogCount;

    /**
     * Creates a transaction context with the given parameters
     *
     * @param index the transaction index within the block
     * @param cumulativeGas the cumulative gas used up to this transaction
     * @param cumulativeLogCount the cumulative log count up to this transaction
     */
    public TransactionContext(int index, long cumulativeGas, long cumulativeLogCount) {
      this.index = index;
      this.cumulativeGas = cumulativeGas;
      this.cumulativeLogCount = cumulativeLogCount;
    }
  }
}
