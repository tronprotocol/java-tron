package org.tron.common.logsfilter.capsule;

import java.util.Objects;

import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.trigger.TransactionLogTrigger;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.common.utils.Base58;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TransactionTrace;
import org.tron.protos.Protocol;

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

    //result
    if (Objects.nonNull(trxCasule.getContractRet())){
      transactionLogTrigger.setResult(trxCasule.getContractRet().toString());
    }

    if (Objects.nonNull(trxCasule.getInstance().getRawData())){
      // feelimit
      transactionLogTrigger.setFeeLimit(trxCasule.getInstance().getRawData().getFeeLimit());

      Protocol.Transaction.Contract contract = trxCasule.getInstance().getRawData().getContract(0);
      // contract type
      if (Objects.nonNull(contract)){
        Protocol.Transaction.Contract.ContractType contractType = contract.getType();
        if (Objects.nonNull(contractType)){
          transactionLogTrigger.setContractType(contractType.toString());
        }

        transactionLogTrigger.setCallValue(TransactionCapsule.getCallValue(contract));
      }
    }

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
        transactionLogTrigger.setContractResult(Hex.toHexString(contractResult.toByteArray()));
      }

      if (Objects.nonNull(contractAddress) && contractAddress.size() > 0){
        transactionLogTrigger.setContractAddress(Base58.encode((contractAddress.toByteArray())));
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
