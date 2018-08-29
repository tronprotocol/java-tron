package org.tron.core.db;

import static org.tron.common.runtime.vm.program.InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.TrxType.TRX_PRECOMPILED_TYPE;

import java.util.Objects;
import org.springframework.util.StringUtils;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.common.runtime.vm.program.Program.BadJumpDestinationException;
import org.tron.common.runtime.vm.program.Program.IllegalOperationException;
import org.tron.common.runtime.vm.program.Program.JVMStackOverFlowException;
import org.tron.common.runtime.vm.program.Program.OutOfEnergyException;
import org.tron.common.runtime.vm.program.Program.OutOfMemoryException;
import org.tron.common.runtime.vm.program.Program.OutOfResourceException;
import org.tron.common.runtime.vm.program.Program.PrecompiledContractException;
import org.tron.common.runtime.vm.program.Program.StackTooLargeException;
import org.tron.common.runtime.vm.program.Program.StackTooSmallException;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.ReceiptCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.TransactionTraceException;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.contractResult;

public class TransactionTrace {

  private TransactionCapsule trx;

  private ReceiptCapsule receipt;

  private Manager dbManager;

  private EnergyProcessor energyProcessor;

  private InternalTransaction.TrxType trxType;

  public TransactionCapsule getTrx() {
    return trx;
  }

  public TransactionTrace(TransactionCapsule trx, Manager dbManager) {
    this.trx = trx;
    Transaction.Contract.ContractType contractType = this.trx.getInstance().getRawData()
        .getContract(0).getType();
    switch (contractType.getNumber()) {
      case ContractType.TriggerSmartContract_VALUE:
        trxType = TRX_CONTRACT_CALL_TYPE;
        break;
      case ContractType.CreateSmartContract_VALUE:
        trxType = TRX_CONTRACT_CREATION_TYPE;
        break;
      default:
        trxType = TRX_PRECOMPILED_TYPE;
    }

    this.dbManager = dbManager;
    this.receipt = new ReceiptCapsule(Sha256Hash.ZERO_HASH);

    this.energyProcessor = new EnergyProcessor(this.dbManager);
  }

  public boolean needVM() {
    return this.trxType == TRX_CONTRACT_CALL_TYPE || this.trxType == TRX_CONTRACT_CREATION_TYPE;
  }

  //pre transaction check
  public void init() throws TransactionTraceException {

    // switch (trxType) {
    //   case TRX_PRECOMPILED_TYPE:
    //     break;
    //   case TRX_CONTRACT_CREATION_TYPE:
    //   case TRX_CONTRACT_CALL_TYPE:
    //     // checkForSmartContract();
    //     break;
    //   default:
    //     break;
    // }

  }

  //set bill
  public void setBill(long energyUsage) {
    receipt.setEnergyUsageTotal(energyUsage);
  }

  //set net bill
  public void setNetBill(long netUsage, long netFee) {
    receipt.setNetUsage(netUsage);
    receipt.setNetFee(netFee);
  }

  public void exec(Runtime runtime)
      throws ContractExeException, ContractValidateException {
    /**  VM execute  **/
    runtime.execute();
    runtime.go();
  }

  public void finalization(Runtime runtime) {
    pay();
    runtime.finalization();
  }

  /**
   * pay actually bill(include ENERGY and storage).
   */
  public void pay() {
    byte[] originAccount;
    byte[] callerAccount;
    long percent = 0;
    switch (trxType) {
      case TRX_CONTRACT_CREATION_TYPE:
        callerAccount = TransactionCapsule.getOwner(trx.getInstance().getRawData().getContract(0));
        originAccount = callerAccount;
        break;
      case TRX_CONTRACT_CALL_TYPE:
        TriggerSmartContract callContract = ContractCapsule
            .getTriggerContractFromTransaction(trx.getInstance());
        callerAccount = callContract.getOwnerAddress().toByteArray();

        ContractCapsule contract =
            dbManager.getContractStore().get(callContract.getContractAddress().toByteArray());
        originAccount = contract.getInstance().getOriginAddress().toByteArray();
        percent = Math.max(100 - contract.getConsumeUserResourcePercent(), 0);
        percent = Math.min(percent, 100);
        break;
      default:
        return;
    }

    // originAccount Percent = 30%
    AccountCapsule origin = dbManager.getAccountStore().get(originAccount);
    AccountCapsule caller = dbManager.getAccountStore().get(callerAccount);
    receipt.payEnergyBill(
        dbManager,
        origin,
        caller,
        percent,
        energyProcessor,
        dbManager.getWitnessController().getHeadSlot());
  }

  public void check() throws ReceiptCheckErrException {
    if (!needVM()) {
      return;
    }
    if (Objects.isNull(trx.getContractRet())) {
      throw new ReceiptCheckErrException("null resultCode");
    }
    if (!trx.getContractRet().equals(receipt.getResult())) {
      throw new ReceiptCheckErrException("Different resultCode");
    }
  }

  public ReceiptCapsule getReceipt() {
    return receipt;
  }

  public void setResult(Runtime runtime) {
    if (!needVM()) {
      return;
    }
    RuntimeException exception = runtime.getResult().getException();
    if (Objects.isNull(exception) && StringUtils
        .isEmpty(runtime.getRuntimeError()) && !runtime.getResult().isRevert()) {
      receipt.setResult(contractResult.SUCCESS);
      return;
    }
    if (runtime.getResult().isRevert()) {
      receipt.setResult(contractResult.REVERT);
      return;
    }
    if (exception instanceof IllegalOperationException) {
      receipt.setResult(contractResult.ILLEGAL_OPERATION);
      return;
    }
    if (exception instanceof OutOfEnergyException) {
      receipt.setResult(contractResult.OUT_OF_ENERGY);
      return;
    }
    if (exception instanceof BadJumpDestinationException) {
      receipt.setResult(contractResult.BAD_JUMP_DESTINATION);
      return;
    }
    if (exception instanceof OutOfResourceException) {
      receipt.setResult(contractResult.OUT_OF_TIME);
      return;
    }
    if (exception instanceof OutOfMemoryException) {
      receipt.setResult(contractResult.OUT_OF_MEMORY);
      return;
    }
    if (exception instanceof PrecompiledContractException) {
      receipt.setResult(contractResult.PRECOMPILED_CONTRACT);
      return;
    }
    if (exception instanceof StackTooSmallException) {
      receipt.setResult(contractResult.STACK_TOO_SMALL);
      return;
    }
    if (exception instanceof StackTooLargeException) {
      receipt.setResult(contractResult.STACK_TOO_LARGE);
      return;
    }
    if (exception instanceof JVMStackOverFlowException) {
      receipt.setResult(contractResult.JVM_STACK_OVER_FLOW);
      return;
    }
    receipt.setResult(contractResult.UNKNOWN);
    return;
  }
}
