package org.tron.core.services.jsonrpc.types;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.convertToTronAddress;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getToAddress;

import java.util.ArrayList;
import java.util.List;
import org.tron.api.GrpcAPI.TransactionInfoList;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.JsonRpcInternalException;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.ResourceReceipt;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.TransactionInfo;

/**
 * Factory class for creating TransactionReceipt instances
 */
public class TransactionReceiptFactory {

  /**
   * Creates a single TransactionReceipt from block and transaction info
   * This method finds the transaction context and creates a receipt with all necessary fields
   * @param blockCapsule the block containing the transaction, used for set
   * blockHash/blockNumber/address/from/to fields
   * @param txInfo the transaction info containing execution details
   * @param transactionInfoList the complete transaction info list in the block,
   * used for cumulative calculations
   * @param energyFee the energy fee at the block timestamp
   * @return TransactionReceipt object, or null if the transaction is not found in the block
   */
  public static TransactionReceipt createFromBlockAndTxInfo(
      BlockCapsule blockCapsule,
      TransactionInfo txInfo,
      TransactionInfoList transactionInfoList,
      long energyFee) {
    String txId = ByteArray.toHexString(txInfo.getId().toByteArray());
    TransactionContext context = findTransactionContext(transactionInfoList, txId);

    // txId not in transactionInfoList
    if (context == null) {
      return null;
    }

    return createReceipt(blockCapsule, txInfo, context, energyFee);
  }

  /**
   * Creates all TransactionReceipts from a block
   * This method processes all transactions in the block
   * and creates receipts with cumulative gas calculations
   * @param blockCapsule the block containing transactions
   * @param transactionInfoList the transaction info list for the block
   * @param energyFee the energy fee for the block timestamp
   * @return List of TransactionReceipt objects for all transactions in the block
   */
  public static List<TransactionReceipt> createFromBlock(
      BlockCapsule blockCapsule, 
      TransactionInfoList transactionInfoList,
      long energyFee) throws JsonRpcInternalException {

    if (blockCapsule == null || transactionInfoList == null) {
      return null;
    }

    // Validate transaction list size consistency
    int transactionSizeInBlock = blockCapsule.getTransactions().size();
    if (transactionInfoList.getTransactionInfoCount() != transactionSizeInBlock) {
      throw new JsonRpcInternalException(
          String.format("TransactionList size mismatch: "
                  + "block has %d transactions, but transactionInfoList has %d",
              transactionSizeInBlock, transactionInfoList.getTransactionInfoCount()));
    }
    
    List<TransactionReceipt> receipts = new ArrayList<>();
    long cumulativeGas = 0;
    long cumulativeLogCount = 0;

    for (int index = 0; index < transactionInfoList.getTransactionInfoCount(); index++) {
      TransactionInfo info = transactionInfoList.getTransactionInfo(index);
      ResourceReceipt resourceReceipt = info.getReceipt();
      
      long energyUsage = resourceReceipt.getEnergyUsageTotal();
      cumulativeGas += energyUsage;

      TransactionContext context = new TransactionContext(
          index, cumulativeGas, energyUsage, cumulativeLogCount);
      
      TransactionReceipt receipt = createReceipt(blockCapsule, info, context, energyFee);
      receipts.add(receipt);
      
      cumulativeLogCount += info.getLogCount();
    }

    return receipts;
  }

  /**
   * Finds transaction context for a specific transaction ID within the block
   * Calculates cumulative gas and log count up to the target transaction
   * @param infoList the transaction info list for the block
   * @param txId the transaction ID to search for
   * @return TransactionContext containing index and cumulative values, or null if not found
   */
  private static TransactionContext findTransactionContext(TransactionInfoList infoList,
      String txId) {
    long cumulativeGas = 0;
    long cumulativeLogCount = 0;

    for (int index = 0; index < infoList.getTransactionInfoCount(); index++) {
      TransactionInfo info = infoList.getTransactionInfo(index);
      ResourceReceipt resourceReceipt = info.getReceipt();
      
      long energyUsage = resourceReceipt.getEnergyUsageTotal();
      cumulativeGas += energyUsage;

      if (ByteArray.toHexString(info.getId().toByteArray()).equals(txId)) {
        return new TransactionContext(index, cumulativeGas, energyUsage, cumulativeLogCount);
      } else {
        cumulativeLogCount += info.getLogCount();
      }
    }
    return null;
  }

  /**
   * Creates a TransactionReceipt with the given context
   * Orchestrates the creation process by calling all field-setting methods
   * @param blockCapsule the block containing the transaction
   * @param txInfo the transaction info
   * @param context the transaction context with index and cumulative values
   * @param energyFee the energy fee for the block
   * @return fully populated TransactionReceipt object
   */
  private static TransactionReceipt createReceipt(
      BlockCapsule blockCapsule,
      TransactionInfo txInfo,
      TransactionContext context,
      long energyFee) {
    
    TransactionReceipt receipt = new TransactionReceipt();
    
    setBasicFields(receipt, blockCapsule, txInfo, context, energyFee);
    setContractFields(receipt, blockCapsule, txInfo, context);
    setLogs(receipt, txInfo, context);
    setDefaultFields(receipt);
    
    return receipt;
  }

  /**
   * Sets basic fields of the receipt including block info, transaction hash, and gas usage
   * @param receipt the receipt object to populate
   * @param blockCapsule the block containing the transaction
   * @param txInfo the transaction info
   * @param context the transaction context with index and cumulative values
   * @param energyFee the energy fee for the block
   */
  private static void setBasicFields(
      TransactionReceipt receipt,
      BlockCapsule blockCapsule,
      TransactionInfo txInfo,
      TransactionContext context,
      long energyFee) {
    
    receipt.setBlockHash(ByteArray.toJsonHex(blockCapsule.getBlockId().getBytes()));
    receipt.setBlockNumber(ByteArray.toJsonHex(blockCapsule.getNum()));
    receipt.setTransactionHash(ByteArray.toJsonHex(txInfo.getId().toByteArray()));
    receipt.setTransactionIndex(ByteArray.toJsonHex(context.index));
    receipt.setCumulativeGasUsed(ByteArray.toJsonHex(context.cumulativeGas));
    receipt.setGasUsed(ByteArray.toJsonHex(context.energyUsage));
    receipt.status = txInfo.getReceipt().getResultValue() <= 1 ? "0x1" : "0x0";
    receipt.setEffectiveGasPrice(ByteArray.toJsonHex(energyFee));
  }

  /**
   * Sets contract-related fields including from/to addresses
   * and contract address for smart contract creation
   * @param receipt the receipt object to populate
   * @param blockCapsule the block containing the transaction
   * @param txInfo the transaction info
   * @param context the transaction context with index
   */
  private static void setContractFields(
      TransactionReceipt receipt,
      BlockCapsule blockCapsule,
      TransactionInfo txInfo,
      TransactionContext context) {
    
    receipt.setFrom(null);
    receipt.setTo(null);
    receipt.setContractAddress(null);

    TransactionCapsule txCapsule = blockCapsule.getTransactions().get(context.index);
    
    Protocol.Transaction transaction = txCapsule.getInstance();
    if (!transaction.getRawData().getContractList().isEmpty()) {
      Contract contract = transaction.getRawData().getContract(0);
      byte[] fromByte = TransactionCapsule.getOwner(contract);
      byte[] toByte = getToAddress(transaction);
      receipt.setFrom(ByteArray.toJsonHexAddress(fromByte));
      receipt.setTo(ByteArray.toJsonHexAddress(toByte));

      if (contract.getType() == ContractType.CreateSmartContract) {
        receipt.setContractAddress(
            ByteArray.toJsonHexAddress(txInfo.getContractAddress().toByteArray()));
      }
    }
  }

  /**
   * Sets transaction logs by creating TransactionLog objects for each log entry
   * @param receipt the receipt object to populate
   * @param txInfo the transaction info containing log data
   * @param context the transaction context with cumulative log count
   */
  private static void setLogs(
      TransactionReceipt receipt,
      TransactionInfo txInfo,
      TransactionContext context) {
    
    List<TransactionReceipt.TransactionLog> logList = new ArrayList<>();
    for (int logIndex = 0; logIndex < txInfo.getLogCount(); logIndex++) {
      TransactionInfo.Log log = txInfo.getLogList().get(logIndex);
      TransactionReceipt.TransactionLog transactionLog
          = createTransactionLog(log, receipt, logIndex, context);
      logList.add(transactionLog);
    }
    receipt.setLogs(logList.toArray(new TransactionReceipt.TransactionLog[0]));
  }

  /**
   * Creates a single transaction log with all required fields
   * @param log the log data from the transaction
   * @param receipt the parent receipt for reference
   * @param logIndex the index of this log within the transaction
   * @param context the transaction context with cumulative log count
   * @return populated TransactionLog object
   */
  private static TransactionReceipt.TransactionLog createTransactionLog(
      TransactionInfo.Log log,
      TransactionReceipt receipt,
      int logIndex,
      TransactionContext context) {
    
    TransactionReceipt.TransactionLog transactionLog = new TransactionReceipt.TransactionLog();
    transactionLog.setLogIndex(ByteArray.toJsonHex(logIndex + context.cumulativeLogCount));
    transactionLog.setTransactionHash(receipt.getTransactionHash());
    transactionLog.setTransactionIndex(receipt.getTransactionIndex());
    transactionLog.setBlockHash(receipt.getBlockHash());
    transactionLog.setBlockNumber(receipt.getBlockNumber());
    
    byte[] addressByte = convertToTronAddress(log.getAddress().toByteArray());
    transactionLog.setAddress(ByteArray.toJsonHexAddress(addressByte));
    transactionLog.setData(ByteArray.toJsonHex(log.getData().toByteArray()));

    String[] topics = new String[log.getTopicsCount()];
    for (int i = 0; i < log.getTopicsCount(); i++) {
      topics[i] = ByteArray.toJsonHex(log.getTopics(i).toByteArray());
    }
    transactionLog.setTopics(topics);
    
    return transactionLog;
  }

  /**
   * Sets default fields that are not specific to the transaction
   * @param receipt the receipt object to populate
   */
  private static void setDefaultFields(TransactionReceipt receipt) {
    receipt.setLogsBloom(ByteArray.toJsonHex(new byte[256])); // no value
    receipt.root = null;
    receipt.setType("0x0");
  }

  /**
   * Context class to hold transaction creation parameters
   * Contains index and cumulative values needed for receipt creation
   */
  private static class TransactionContext {
    final int index;
    final long cumulativeGas;
    final long energyUsage;
    final long cumulativeLogCount;

    /**
     * Creates a transaction context with the given parameters
     * @param index the transaction index within the block
     * @param cumulativeGas the cumulative gas used up to this transaction
     * @param energyUsage the energy usage for this specific transaction
     * @param cumulativeLogCount the cumulative log count up to this transaction
     */
    TransactionContext(int index, long cumulativeGas, long energyUsage, long cumulativeLogCount) {
      this.index = index;
      this.cumulativeGas = cumulativeGas;
      this.energyUsage = energyUsage;
      this.cumulativeLogCount = cumulativeLogCount;
    }
  }
} 