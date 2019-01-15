package org.tron.common.logsfilter.capsule;

import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TransferAssetContract;
import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TransferContract;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;

import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.trigger.TransactionLogTrigger;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TransactionTrace;
import org.tron.protos.Contract.TransferAssetContract;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract;

@Slf4j
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
      Any contractParameter = contract.getParameter();
      // contract type
      if (Objects.nonNull(contract)){
        Protocol.Transaction.Contract.ContractType contractType = contract.getType();
        if (Objects.nonNull(contractType)){
          transactionLogTrigger.setContractType(contractType.toString());
        }

        transactionLogTrigger.setContractCallValue(TransactionCapsule.getCallValue(contract));
      }

      if (Objects.nonNull(contractParameter)) {
        if (contract.getType() == TransferContract) {
          try {
            TransferContract contractTransfer = contractParameter.unpack(TransferContract.class);
            transactionLogTrigger.setAssetName("TRX");
            transactionLogTrigger.setFromAddress(Wallet.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
            transactionLogTrigger.setToAddress(Wallet.encode58Check(contractTransfer.getToAddress().toByteArray()));
            transactionLogTrigger.setAssetAmount(contractTransfer.getAmount());
          } catch (InvalidProtocolBufferException e) {
            logger.error("failed to load transferContract, error '{}'", e);
          }
        } else if (contract.getType() == TransferAssetContract) {
          try {
            TransferAssetContract contractTransfer = contractParameter.unpack(TransferAssetContract.class);
            transactionLogTrigger.setAssetName(ByteArray.toStr(contractTransfer.getAssetName().toByteArray()));
            transactionLogTrigger.setFromAddress(Wallet.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
            transactionLogTrigger.setToAddress(Wallet.encode58Check(contractTransfer.getToAddress().toByteArray()));
            transactionLogTrigger.setAssetAmount(contractTransfer.getAmount());
          } catch (InvalidProtocolBufferException e) {
            logger.error("failed to load transferAssetContract, error'{}'", e);
          }
        }
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
        transactionLogTrigger.setContractAddress(Wallet.encode58Check((contractAddress.toByteArray())));
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
