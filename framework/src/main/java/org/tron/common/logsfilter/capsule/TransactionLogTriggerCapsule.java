package org.tron.common.logsfilter.capsule;

import static org.tron.protos.Protocol.Transaction.Contract.ContractType.CreateSmartContract;
import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TransferAssetContract;
import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TransferContract;
import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TriggerSmartContract;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.trigger.InternalTransactionPojo;
import org.tron.common.logsfilter.trigger.LogPojo;
import org.tron.common.logsfilter.trigger.TransactionLogTrigger;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.ProgramResult;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TransactionTrace;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

@Slf4j
public class TransactionLogTriggerCapsule extends TriggerCapsule {

  @Getter
  @Setter
  private TransactionLogTrigger transactionLogTrigger;

  public TransactionLogTriggerCapsule(TransactionCapsule trxCapsule, BlockCapsule blockCapsule) {
    this(trxCapsule, blockCapsule, 0, 0, 0, null, 0);
  }

  public TransactionLogTriggerCapsule(TransactionCapsule trxCapsule, BlockCapsule blockCapsule,
      int txIndex, long preCumulativeEnergyUsed, long preCumulativeLogCount,
      TransactionInfo transactionInfo, long energyUnitPrice) {
    transactionLogTrigger = new TransactionLogTrigger();

    String blockHash = "";
    if (Objects.nonNull(blockCapsule)) {
      blockHash = blockCapsule.getBlockId().toString();
      transactionLogTrigger.setBlockHash(blockHash);
    }

    String transactionHash = trxCapsule.getTransactionId().toString();
    transactionLogTrigger.setTransactionId(transactionHash);
    transactionLogTrigger.setTimeStamp(blockCapsule.getTimeStamp());
    transactionLogTrigger.setBlockNumber(trxCapsule.getBlockNum());
    transactionLogTrigger.setData(Hex.toHexString(trxCapsule
        .getInstance().getRawData().getData().toByteArray()));

    TransactionTrace trxTrace = trxCapsule.getTrxTrace();

    //result
    if (Objects.nonNull(trxCapsule.getContractRet())) {
      transactionLogTrigger.setResult(trxCapsule.getContractRet().toString());
    }

    if (Objects.nonNull(trxCapsule.getInstance().getRawData())) {
      // fee limit
      transactionLogTrigger.setFeeLimit(trxCapsule.getInstance().getRawData().getFeeLimit());

      Protocol.Transaction.Contract contract = trxCapsule.getInstance().getRawData().getContract(0);
      Any contractParameter = null;
      // contract type
      if (Objects.nonNull(contract)) {
        Protocol.Transaction.Contract.ContractType contractType = contract.getType();
        if (Objects.nonNull(contractType)) {
          transactionLogTrigger.setContractType(contractType.toString());
        }

        contractParameter = contract.getParameter();

        transactionLogTrigger.setContractCallValue(TransactionCapsule.getCallValue(contract));
      }

      if (Objects.nonNull(contractParameter) && Objects.nonNull(contract)) {
        try {
          if (contract.getType() == TransferContract) {
            TransferContract contractTransfer = contractParameter.unpack(TransferContract.class);

            if (Objects.nonNull(contractTransfer)) {
              transactionLogTrigger.setAssetName("trx");

              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil
                    .encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }

              if (Objects.nonNull(contractTransfer.getToAddress())) {
                transactionLogTrigger.setToAddress(
                    StringUtil.encode58Check(contractTransfer.getToAddress().toByteArray()));
              }

              transactionLogTrigger.setAssetAmount(contractTransfer.getAmount());
            }

          } else if (contract.getType() == TransferAssetContract) {
            TransferAssetContract contractTransfer = contractParameter
                .unpack(TransferAssetContract.class);

            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getAssetName())) {
                transactionLogTrigger.setAssetName(contractTransfer.getAssetName().toStringUtf8());
              }

              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                    StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }

              if (Objects.nonNull(contractTransfer.getToAddress())) {
                transactionLogTrigger.setToAddress(StringUtil
                    .encode58Check(contractTransfer.getToAddress().toByteArray()));
              }
              transactionLogTrigger.setAssetAmount(contractTransfer.getAmount());
            }
          } else if (contract.getType() == TriggerSmartContract) {
            TriggerSmartContract triggerSmartContract = contractParameter
                .unpack(TriggerSmartContract.class);

            if (Objects.nonNull(triggerSmartContract.getOwnerAddress())) {
              transactionLogTrigger.setFromAddress(
                  StringUtil.encode58Check(triggerSmartContract.getOwnerAddress().toByteArray()));
            }

            if (Objects.nonNull(triggerSmartContract.getContractAddress())) {
              transactionLogTrigger.setToAddress(StringUtil
                  .encode58Check(triggerSmartContract.getContractAddress().toByteArray()));
            }
          } else if (contract.getType() == CreateSmartContract) {
            CreateSmartContract createSmartContract = contractParameter
                .unpack(CreateSmartContract.class);

            if (Objects.nonNull(createSmartContract.getOwnerAddress())) {
              transactionLogTrigger.setFromAddress(
                  StringUtil.encode58Check(createSmartContract.getOwnerAddress().toByteArray()));
            }
          }
        } catch (Exception e) {
          logger.error("failed to load transferAssetContract, error '{}'", e.getMessage());
        }
      }
    }

    long energyUsageTotal = 0;
    // receipt
    if (Objects.nonNull(trxTrace) && Objects.nonNull(trxTrace.getReceipt())) {
      energyUsageTotal = trxTrace.getReceipt().getEnergyUsageTotal();

      transactionLogTrigger.setEnergyFee(trxTrace.getReceipt().getEnergyFee());
      transactionLogTrigger.setOriginEnergyUsage(trxTrace.getReceipt().getOriginEnergyUsage());
      transactionLogTrigger.setEnergyUsageTotal(energyUsageTotal);
      transactionLogTrigger.setNetUsage(trxTrace.getReceipt().getNetUsage());
      transactionLogTrigger.setNetFee(trxTrace.getReceipt().getNetFee());
      transactionLogTrigger.setEnergyUsage(trxTrace.getReceipt().getEnergyUsage());
    }

    // program result
    if (Objects.nonNull(trxTrace) && Objects.nonNull(trxTrace.getRuntime()) && Objects
        .nonNull(trxTrace.getRuntime().getResult())) {
      ProgramResult programResult = trxTrace.getRuntime().getResult();
      ByteString contractResult = ByteString.copyFrom(programResult.getHReturn());
      ByteString contractAddress = ByteString.copyFrom(programResult.getContractAddress());

      if (Objects.nonNull(contractResult) && contractResult.size() > 0) {
        transactionLogTrigger.setContractResult(Hex.toHexString(contractResult.toByteArray()));
      }

      if (Objects.nonNull(contractAddress) && contractAddress.size() > 0) {
        transactionLogTrigger
            .setContractAddress(StringUtil.encode58Check((contractAddress.toByteArray())));
      }

      // internal transaction
      transactionLogTrigger.setInternalTransactionList(
          getInternalTransactionList(programResult.getInternalTransactions()));
    }

    // process transactionInfo list, only enabled when ethCompatible is true
    if (Objects.nonNull(transactionInfo)) {
      transactionLogTrigger.setTransactionIndex(txIndex);
      transactionLogTrigger.setCumulativeEnergyUsed(preCumulativeEnergyUsed + energyUsageTotal);
      transactionLogTrigger.setPreCumulativeLogCount(preCumulativeLogCount);
      transactionLogTrigger.setEnergyUnitPrice(energyUnitPrice);

      List<LogPojo> logPojoList = new ArrayList<>();
      for (int index = 0; index < transactionInfo.getLogCount(); index++) {
        TransactionInfo.Log log = transactionInfo.getLogList().get(index);
        LogPojo logPojo = new LogPojo();

        logPojo.setAddress((log.getAddress() != null)
            ? Hex.toHexString(log.getAddress().toByteArray()) : "");
        logPojo.setBlockHash(blockHash);
        logPojo.setBlockNumber(trxCapsule.getBlockNum());
        logPojo.setData(Hex.toHexString(log.getData().toByteArray()));
        logPojo.setLogIndex(preCumulativeLogCount + index);

        List<String> topics = new ArrayList<>();
        for (int i = 0; i < log.getTopicsCount(); i++) {
          topics.add(Hex.toHexString(log.getTopics(i).toByteArray()));
        }
        logPojo.setTopicList(topics);

        logPojo.setTransactionHash(transactionHash);
        logPojo.setTransactionIndex(txIndex);

        logPojoList.add(logPojo);
      }

      transactionLogTrigger.setLogList(logPojoList);
    }
  }

  public void setLatestSolidifiedBlockNumber(long latestSolidifiedBlockNumber) {
    transactionLogTrigger.setLatestSolidifiedBlockNumber(latestSolidifiedBlockNumber);
  }

  private List<InternalTransactionPojo> getInternalTransactionList(
      List<InternalTransaction> internalTransactionList) {
    List<InternalTransactionPojo> pojoList = new ArrayList<>();

    internalTransactionList.forEach(internalTransaction -> {
      InternalTransactionPojo item = new InternalTransactionPojo();

      item.setHash(Hex.toHexString(internalTransaction.getHash()));
      item.setCallValue(internalTransaction.getValue());
      item.setTokenInfo(internalTransaction.getTokenInfo());
      item.setCaller_address(Hex.toHexString(internalTransaction.getSender()));
      item.setTransferTo_address(Hex.toHexString(internalTransaction.getTransferToAddress()));
      item.setData(Hex.toHexString(internalTransaction.getData()));
      item.setRejected(internalTransaction.isRejected());
      item.setNote(internalTransaction.getNote());
      item.setExtra(internalTransaction.getExtra());

      pojoList.add(item);
    });

    return pojoList;
  }

  @Override
  public void processTrigger() {
    EventPluginLoader.getInstance().postTransactionTrigger(transactionLogTrigger);
  }
}
