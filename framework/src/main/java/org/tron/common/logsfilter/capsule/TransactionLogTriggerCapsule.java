package org.tron.common.logsfilter.capsule;

import static org.tron.protos.Protocol.Transaction.Contract.ContractType.CreateSmartContract;
import static org.tron.protos.contract.Common.ResourceCode.ENERGY;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.BalanceContract.CancelAllUnfreezeV2Contract;
import org.tron.protos.contract.BalanceContract.DelegateResourceContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.BalanceContract.UnDelegateResourceContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.WithdrawExpireUnfreezeContract;
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

  public TransactionLogTriggerCapsule(TransactionCapsule trxCapsule,
      BlockCapsule blockCapsule, TransactionInfo transactionInfo) {
    this(trxCapsule, blockCapsule, 0, 0, 0, transactionInfo, 0, true);
  }

  public TransactionLogTriggerCapsule(TransactionCapsule trxCapsule, BlockCapsule blockCapsule,
      int txIndex, long preCumulativeEnergyUsed, long preCumulativeLogCount,
      TransactionInfo transactionInfo, long energyUnitPrice) {
    this(trxCapsule, blockCapsule, txIndex, preCumulativeEnergyUsed, preCumulativeLogCount,
        transactionInfo, energyUnitPrice, false);
  }

  public TransactionLogTriggerCapsule(TransactionCapsule trxCapsule, BlockCapsule blockCapsule,
      int txIndex, long preCumulativeEnergyUsed, long preCumulativeLogCount,
      TransactionInfo transactionInfo, long energyUnitPrice, boolean isNewEventService) {
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

    Transaction.raw rawData = trxCapsule.getInstance().getRawData();
    ContractType contractType = null;

    if (Objects.nonNull(rawData)) {
      // fee limit
      transactionLogTrigger.setFeeLimit(rawData.getFeeLimit());

      Protocol.Transaction.Contract contract = rawData.getContract(0);
      Any contractParameter = null;

      // contract type
      if (Objects.nonNull(contract)) {
        contractType = contract.getType();
        if (Objects.nonNull(contractType)) {
          transactionLogTrigger.setContractType(contractType.toString());
        }

        contractParameter = contract.getParameter();

        transactionLogTrigger.setContractCallValue(TransactionCapsule.getCallValue(contract));
      }

      if (Objects.nonNull(contractParameter) && Objects.nonNull(contract)) {
        try {
          switch (contractType) {
            case TransferContract:
              TransferContract transferContract = contractParameter.unpack(TransferContract.class);

              if (Objects.nonNull(transferContract)) {
                transactionLogTrigger.setAssetName("trx");

                if (Objects.nonNull(transferContract.getOwnerAddress())) {
                  transactionLogTrigger.setFromAddress(StringUtil
                      .encode58Check(transferContract.getOwnerAddress().toByteArray()));
                }

                if (Objects.nonNull(transferContract.getToAddress())) {
                  transactionLogTrigger.setToAddress(
                      StringUtil.encode58Check(transferContract.getToAddress().toByteArray()));
                }

                transactionLogTrigger.setAssetAmount(transferContract.getAmount());
              }
              break;
            case TransferAssetContract:
              TransferAssetContract transferAssetContract = contractParameter
                  .unpack(TransferAssetContract.class);

              if (Objects.nonNull(transferAssetContract)) {
                if (Objects.nonNull(transferAssetContract.getAssetName())) {
                  transactionLogTrigger
                      .setAssetName(transferAssetContract.getAssetName().toStringUtf8());
                }

                if (Objects.nonNull(transferAssetContract.getOwnerAddress())) {
                  transactionLogTrigger.setFromAddress(
                      StringUtil
                          .encode58Check(transferAssetContract.getOwnerAddress().toByteArray()));
                }

                if (Objects.nonNull(transferAssetContract.getToAddress())) {
                  transactionLogTrigger.setToAddress(StringUtil
                      .encode58Check(transferAssetContract.getToAddress().toByteArray()));
                }
                transactionLogTrigger.setAssetAmount(transferAssetContract.getAmount());
              }
              break;
            case TriggerSmartContract:
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
              break;
            case CreateSmartContract:
              CreateSmartContract createSmartContract = contractParameter
                  .unpack(CreateSmartContract.class);

              if (Objects.nonNull(createSmartContract.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                    StringUtil.encode58Check(createSmartContract.getOwnerAddress().toByteArray()));
              }
              break;
            case UnfreezeBalanceContract:
              UnfreezeBalanceContract unfreezeBalanceContract = contractParameter
                  .unpack(UnfreezeBalanceContract.class);

              transactionLogTrigger.setFromAddress(StringUtil
                  .encode58Check(unfreezeBalanceContract.getOwnerAddress().toByteArray()));
              if (!ByteString.EMPTY.equals(unfreezeBalanceContract.getReceiverAddress())) {
                transactionLogTrigger.setToAddress(StringUtil
                    .encode58Check(unfreezeBalanceContract.getReceiverAddress().toByteArray()));
              }
              transactionLogTrigger.setAssetName("trx");
              if (Objects.nonNull(transactionInfo)) {
                transactionLogTrigger.setAssetAmount(
                    transactionInfo.getUnfreezeAmount());
              }
              break;
            case FreezeBalanceV2Contract:
              FreezeBalanceV2Contract freezeBalanceV2Contract = contractParameter
                  .unpack(FreezeBalanceV2Contract.class);

              transactionLogTrigger.setFromAddress(StringUtil
                  .encode58Check(freezeBalanceV2Contract.getOwnerAddress().toByteArray()));
              transactionLogTrigger.setAssetName("trx");
              transactionLogTrigger.setAssetAmount(freezeBalanceV2Contract.getFrozenBalance());
              break;
            case UnfreezeBalanceV2Contract:
              UnfreezeBalanceV2Contract unfreezeBalanceV2Contract = contractParameter
                  .unpack(UnfreezeBalanceV2Contract.class);

              transactionLogTrigger.setFromAddress(StringUtil
                  .encode58Check(unfreezeBalanceV2Contract.getOwnerAddress().toByteArray()));
              transactionLogTrigger.setAssetName("trx");
              transactionLogTrigger.setAssetAmount(
                  unfreezeBalanceV2Contract.getUnfreezeBalance());
              break;
            case WithdrawExpireUnfreezeContract:
              WithdrawExpireUnfreezeContract withdrawExpireUnfreezeContract = contractParameter
                  .unpack(WithdrawExpireUnfreezeContract.class);

              transactionLogTrigger.setFromAddress(StringUtil.encode58Check(
                  withdrawExpireUnfreezeContract.getOwnerAddress().toByteArray()));
              transactionLogTrigger.setAssetName("trx");
              if (Objects.nonNull(transactionInfo)) {
                transactionLogTrigger.setAssetAmount(transactionInfo.getWithdrawExpireAmount());
              }
              break;
            case DelegateResourceContract:
              DelegateResourceContract delegateResourceContract = contractParameter
                  .unpack(DelegateResourceContract.class);

              transactionLogTrigger.setFromAddress(StringUtil
                  .encode58Check(delegateResourceContract.getOwnerAddress().toByteArray()));
              transactionLogTrigger.setToAddress(StringUtil
                      .encode58Check(delegateResourceContract.getReceiverAddress().toByteArray()));
              transactionLogTrigger.setAssetName("trx");
              transactionLogTrigger.setAssetAmount(
                      delegateResourceContract.getBalance());
              break;
            case UnDelegateResourceContract:
              UnDelegateResourceContract unDelegateResourceContract = contractParameter
                  .unpack(UnDelegateResourceContract.class);

              transactionLogTrigger.setFromAddress(StringUtil
                  .encode58Check(unDelegateResourceContract.getOwnerAddress().toByteArray()));
              transactionLogTrigger.setToAddress(StringUtil.encode58Check(
                  unDelegateResourceContract.getReceiverAddress().toByteArray()));

              transactionLogTrigger.setAssetName("trx");
              transactionLogTrigger.setAssetAmount(
                    unDelegateResourceContract.getBalance());
              break;
            case CancelAllUnfreezeV2Contract:
              CancelAllUnfreezeV2Contract cancelAllUnfreezeV2Contract = contractParameter
                  .unpack(CancelAllUnfreezeV2Contract.class);

              transactionLogTrigger.setFromAddress(StringUtil
                  .encode58Check(cancelAllUnfreezeV2Contract.getOwnerAddress().toByteArray()));
              transactionLogTrigger.setAssetName("trx");
              if (Objects.nonNull(transactionInfo)) {
                transactionLogTrigger.setExtMap(transactionInfo.getCancelUnfreezeV2AmountMap());
              }
              break;
            default:
              break;
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
        if (Objects.nonNull(transactionInfo)
            && EventPluginLoader.getInstance().isTransactionLogTriggerEthCompatible()
            && contractType != null && contractType != CreateSmartContract) {
          transactionLogTrigger.setContractAddress(null);
        } else {
          transactionLogTrigger
              .setContractAddress(StringUtil.encode58Check((contractAddress.toByteArray())));
        }
      }

      // internal transaction
      transactionLogTrigger.setInternalTransactionList(
          getInternalTransactionList(programResult.getInternalTransactions()));
    }

    if (Objects.isNull(trxTrace) && Objects.nonNull(transactionInfo) && isNewEventService) {
      Protocol.ResourceReceipt receipt = transactionInfo.getReceipt();
      energyUsageTotal = receipt.getEnergyUsageTotal();
      transactionLogTrigger.setEnergyFee(receipt.getEnergyFee());
      transactionLogTrigger.setOriginEnergyUsage(receipt.getOriginEnergyUsage());
      transactionLogTrigger.setEnergyUsageTotal(energyUsageTotal);
      transactionLogTrigger.setNetUsage(receipt.getNetUsage());
      transactionLogTrigger.setNetFee(receipt.getNetFee());
      transactionLogTrigger.setEnergyUsage(receipt.getEnergyUsage());

      if (transactionInfo.getContractResultCount() > 0) {
        ByteString contractResult = transactionInfo.getContractResult(0);
        if (Objects.nonNull(contractResult) && contractResult.size() > 0) {
          transactionLogTrigger.setContractResult(Hex.toHexString(contractResult.toByteArray()));
        }
      }

      ByteString contractAddress = transactionInfo.getContractAddress();
      if (Objects.nonNull(contractAddress) && contractAddress.size() > 0) {
        if (EventPluginLoader.getInstance().isTransactionLogTriggerEthCompatible()
            && contractType != null && contractType != CreateSmartContract) {
          transactionLogTrigger.setContractAddress(null);
        } else {
          transactionLogTrigger
              .setContractAddress(StringUtil.encode58Check((contractAddress.toByteArray())));
        }
      }
    }

    // process transactionInfo list, only enabled when ethCompatible is true
    if (Objects.nonNull(transactionInfo)
        && EventPluginLoader.getInstance().isTransactionLogTriggerEthCompatible()) {
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
