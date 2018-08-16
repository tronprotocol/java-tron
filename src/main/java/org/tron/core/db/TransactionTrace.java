package org.tron.core.db;

import static org.tron.common.runtime.vm.program.InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.TrxType.TRX_PRECOMPILED_TYPE;

import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.ReceiptCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.OutOfSlotTimeException;
import org.tron.core.exception.TransactionTraceException;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

public class TransactionTrace {

  private TransactionCapsule trx;

  private ReceiptCapsule receipt;

  private Manager dbManager;

  private CpuProcessor cpuProcessor;

  private StorageMarket storageMarket;

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

    //TODO: set bill owner
    receipt = new ReceiptCapsule(Sha256Hash.ZERO_HASH);
    this.dbManager = dbManager;
    this.receipt = new ReceiptCapsule(Sha256Hash.ZERO_HASH);

    this.cpuProcessor = new CpuProcessor(this.dbManager);
    this.storageMarket = new StorageMarket(this.dbManager);
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
  public void setBill(long cpuUseage, long storageUseage) {
    receipt.setCpuUsage(cpuUseage);
    receipt.setStorageDelta(storageUseage);
  }

  //set net bill
  public void setNetBill(long netUsage, long netFee) {
    receipt.setNetUsage(netUsage);
    receipt.setNetFee(netFee);
  }

  public void exec(Runtime runtime)
      throws ContractExeException, ContractValidateException, OutOfSlotTimeException {
    /**  VM execute  **/
    runtime.init();
    runtime.execute();
    runtime.go();
    runtime.finalization();
  }

  /**
   * pay actually bill(include CPU and storage).
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
    receipt.payCpuBill(
        dbManager,
        origin,
        caller,
        percent,
        cpuProcessor,
        dbManager.getWitnessController().getHeadSlot());

    receipt.payStorageBill(dbManager, origin, caller, percent, storageMarket);
  }

  public ReceiptCapsule getReceipt() {
    return receipt;
  }
}
