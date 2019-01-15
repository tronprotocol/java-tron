package org.tron.common.logsfilter.capsule;

import java.util.Objects;

import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.trigger.TransactionLogTrigger;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.common.utils.TypeConversion;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TransactionTrace;

public class TransactionLogTriggerCapsule extends TriggerCapsule {

  @Getter
  @Setter
  TransactionLogTrigger transactionLogTrigger;

  public TransactionLogTriggerCapsule(TransactionCapsule trxCasule, BlockCapsule blockCapsule) {
    transactionLogTrigger = new TransactionLogTrigger();
    if (Objects.nonNull(blockCapsule)) {
      transactionLogTrigger.setBlockHash(blockCapsule.getBlockId().toString());
    }
    transactionLogTrigger.setTransactionId(trxCasule.getTransactionId().toString());
    transactionLogTrigger.setTimeStamp(trxCasule.getTimestamp());
    transactionLogTrigger.setBlockNumber(trxCasule.getBlockNum());

    TransactionTrace trxTrace = trxCasule.getTrxTrace();

    // receipt
    if (Objects.nonNull(trxTrace) && Objects.nonNull(trxTrace.getReceipt())) {
      transactionLogTrigger.setEnergyFee(trxTrace.getReceipt().getEnergyFee());
      transactionLogTrigger.setOriginEnergyUsage(trxTrace.getReceipt().getOriginEnergyUsage());
      transactionLogTrigger.setEnergyUsageTotal(trxTrace.getReceipt().getEnergyUsageTotal());
      transactionLogTrigger.setNetUsage(trxTrace.getReceipt().getNetUsage());
      transactionLogTrigger.setNetFee(trxTrace.getReceipt().getNetFee());
      transactionLogTrigger.setEnergyUsage(trxTrace.getReceipt().getEnergyUsage());
    }

    ProgramResult programResult = trxTrace.getRuntime().getResult();
    // program result
    if (Objects.nonNull(trxTrace) && Objects.nonNull(programResult)){
      ByteString contractResult = ByteString.copyFrom(programResult.getHReturn());
      ByteString contractAddress = ByteString.copyFrom(programResult.getContractAddress());

      if (Objects.nonNull(contractResult) && contractResult.size() > 0){
        transactionLogTrigger.setContractResult(TypeConversion.bytesToHexString(contractResult.toByteArray()));
      }

      if (Objects.nonNull(contractAddress) && contractAddress.size() > 0){
        transactionLogTrigger.setContractAddress(TypeConversion.bytesToHexString(contractAddress.toByteArray()));
      }

      if (Objects.nonNull(programResult.getRet())){
        transactionLogTrigger.setContractFee(programResult.getRet().getFee());
        transactionLogTrigger.setUnfreezeAmount(programResult.getRet().getUnfreezeAmount());
        transactionLogTrigger.setAssetIssueID(programResult.getRet().getAssetIssueID());
        transactionLogTrigger.setExchangeId(programResult.getRet().getExchangeId());
        transactionLogTrigger.setWithdrawAmount(programResult.getRet().getWithdrawAmount());
        transactionLogTrigger.setExchangeReceivedAmount(programResult.getRet().getExchangeReceivedAmount());
        transactionLogTrigger.setExchangeInjectAnotherAmount(programResult.getRet().getExchangeInjectAnotherAmount());
        transactionLogTrigger.setExchangeWithdrawAnotherAmount(programResult.getRet().getExchangeWithdrawAnotherAmount());
      }

      // internal transaction
      transactionLogTrigger.setInternalTransactionPojoList(programResult.getInternalTransactions());
    }
  }

  @Override
  public void processTrigger() {
    EventPluginLoader.getInstance().postTransactionTrigger(transactionLogTrigger);
  }
}
