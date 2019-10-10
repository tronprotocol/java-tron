package org.tron.core.db;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.util.StringUtils;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.RuntimeImpl;
import org.tron.common.runtime.config.VMConfig;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.common.runtime.vm.program.InternalTransaction.TrxType;
import org.tron.common.runtime.vm.program.Program.*;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.common.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.*;
import org.tron.core.config.args.Args;
import org.tron.core.exception.*;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol.SmartContract.ABI;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.contractResult;

import java.util.Objects;

import static org.tron.common.runtime.vm.program.InternalTransaction.TrxType.*;

@Slf4j(topic = "TransactionTrace")
public class TransactionTrace {

  private TransactionCapsule trx;

  private ReceiptCapsule receipt;

  private Manager dbManager;

  private Runtime runtime;

  private EnergyProcessor energyProcessor;

  private InternalTransaction.TrxType trxType;

  private long txStartTimeInMs;

  public TransactionCapsule getTrx() {
    return trx;
  }

  public enum TimeResultType {
    NORMAL,
    LONG_RUNNING,
    OUT_OF_TIME
  }

  @Getter
  @Setter
  private TimeResultType timeResultType = TimeResultType.NORMAL;

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

  private boolean needVM() {
    return this.trxType == TRX_CONTRACT_CALL_TYPE || this.trxType == TRX_CONTRACT_CREATION_TYPE;
  }

  public void init(BlockCapsule blockCap) {
    init(blockCap, false);
  }

  //pre transaction check
  public void init(BlockCapsule blockCap, boolean eventPluginLoaded) {
    txStartTimeInMs = System.currentTimeMillis();
    DepositImpl deposit = DepositImpl.createRoot(dbManager);
    runtime = new RuntimeImpl(this, blockCap, deposit, new ProgramInvokeFactoryImpl());
    runtime.setEnableEventLinstener(eventPluginLoaded);
  }

  public void checkIsConstant() throws ContractValidateException, VMIllegalException {
    if (VMConfig.allowTvmConstantinople()) {
      return;
    }

    TriggerSmartContract triggerContractFromTransaction = ContractCapsule
        .getTriggerContractFromTransaction(this.getTrx().getInstance());
    if (TrxType.TRX_CONTRACT_CALL_TYPE == this.trxType) {
      DepositImpl deposit = DepositImpl.createRoot(dbManager);
      ContractCapsule contract = deposit
          .getContract(triggerContractFromTransaction.getContractAddress().toByteArray());
      if (contract == null) {
        logger.info("contract: {} is not in contract store", Wallet
            .encode58Check(triggerContractFromTransaction.getContractAddress().toByteArray()));
        throw new ContractValidateException("contract: " + Wallet
            .encode58Check(triggerContractFromTransaction.getContractAddress().toByteArray())
            + " is not in contract store");
      }
      ABI abi = contract.getInstance().getAbi();
      if (Wallet.isConstant(abi, triggerContractFromTransaction)) {
        throw new VMIllegalException("cannot call constant method");
      }
    }
  }

  //set bill
  public void setBill(long energyUsage) {
    if (energyUsage < 0) {
      energyUsage = 0L;
    }
    receipt.setEnergyUsageTotal(energyUsage);
  }

  //set net bill
  public void setNetBill(long netUsage, long netFee) {
    receipt.setNetUsage(netUsage);
    receipt.setNetFee(netFee);
  }

  public void addNetBill(long netFee) {
    receipt.addNetFee(netFee);
  }

  public void exec()
      throws ContractExeException, ContractValidateException, VMIllegalException {
    /*  VM execute  */
    runtime.execute();
    runtime.go();

    if (TRX_PRECOMPILED_TYPE != runtime.getTrxType()) {
      if (contractResult.OUT_OF_TIME
          .equals(receipt.getResult())) {
        setTimeResultType(TimeResultType.OUT_OF_TIME);
      } else if (System.currentTimeMillis() - txStartTimeInMs
          > Args.getInstance().getLongRunningTime()) {
        setTimeResultType(TimeResultType.LONG_RUNNING);
      }
    }
  }

  public void finalization() throws ContractExeException {
    try {
      pay();
    } catch (BalanceInsufficientException e) {
      throw new ContractExeException(e.getMessage());
    }
    runtime.finalization();
  }

  /**
   * pay actually bill(include ENERGY and storage).
   */
  public void pay() throws BalanceInsufficientException {
    byte[] originAccount;
    byte[] callerAccount;
    long percent = 0;
    long originEnergyLimit = 0;
    switch (trxType) {
      case TRX_CONTRACT_CREATION_TYPE:
        callerAccount = TransactionCapsule.getOwner(trx.getInstance().getRawData().getContract(0));
        originAccount = callerAccount;
        break;
      case TRX_CONTRACT_CALL_TYPE:
        TriggerSmartContract callContract = ContractCapsule
            .getTriggerContractFromTransaction(trx.getInstance());
        ContractCapsule contractCapsule =
            dbManager.getContractStore().get(callContract.getContractAddress().toByteArray());

        callerAccount = callContract.getOwnerAddress().toByteArray();
        originAccount = contractCapsule.getOriginAddress();
        percent = Math
            .max(Constant.ONE_HUNDRED - contractCapsule.getConsumeUserResourcePercent(), 0);
        percent = Math.min(percent, Constant.ONE_HUNDRED);
        originEnergyLimit = contractCapsule.getOriginEnergyLimit();
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
        percent, originEnergyLimit,
        energyProcessor,
        dbManager.getWitnessController().getHeadSlot());
  }

  public boolean checkNeedRetry() {
    if (!needVM()) {
      return false;
    }
    return trx.getContractRet() != contractResult.OUT_OF_TIME && receipt.getResult()
        == contractResult.OUT_OF_TIME;
  }

  public void check() throws ReceiptCheckErrException {
    if (!needVM()) {
      return;
    }
    if (Objects.isNull(trx.getContractRet())) {
      throw new ReceiptCheckErrException("null resultCode");
    }
    if (!trx.getContractRet().equals(receipt.getResult())) {
      logger.info(
          "this tx id: {}, the resultCode in received block: {}, the resultCode in self: {}",
          Hex.toHexString(trx.getTransactionId().getBytes()), trx.getContractRet(),
          receipt.getResult());
      throw new ReceiptCheckErrException("Different resultCode");
    }
  }

  public ReceiptCapsule getReceipt() {
    return receipt;
  }

  public void setResult() {
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
    if (exception instanceof OutOfTimeException) {
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
    if (exception instanceof TransferException) {
      receipt.setResult(contractResult.TRANSFER_FAILED);
      return;
    }

    logger.info("uncaught exception", exception);
    receipt.setResult(contractResult.UNKNOWN);
  }

  public String getRuntimeError() {
    return runtime.getRuntimeError();
  }

  public ProgramResult getRuntimeResult() {
    return runtime.getResult();
  }

  public Runtime getRuntime() {
    return runtime;
  }
}
