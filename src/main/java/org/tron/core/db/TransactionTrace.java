package org.tron.core.db;

import java.util.List;
import org.tron.common.runtime.Runtime;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.ReceiptCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol.Transaction.Contract;

public class TransactionTrace {

  private TransactionCapsule trx;

  private ReceiptCapsule receipt;

  private Manager dbManager;

  AccountCapsule owner;

  public TransactionCapsule getTrx() {
    return trx;
  }

  public TransactionTrace(TransactionCapsule trx, Manager dbManager) {
    this.trx = trx;
    this.dbManager = dbManager;
    this.owner = dbManager.getAccountStore()
        .get(TransactionCapsule.getOwner(trx.getInstance().getRawData().getContract(0)));
    this.receipt = new ReceiptCapsule(Sha256Hash.ZERO_HASH);

  }

  //pre transaction check
  public void init() throws ContractValidateException {
    List<Contract> contractList = trx.getInstance().getRawData().getContractList();
    long castMaxTrx = 0;
    for (Contract contract : contractList) {
      castMaxTrx = Math.addExact(castMaxTrx, TransactionCapsule.getCpuLimitInTrx(contract));
      castMaxTrx = Math.addExact(castMaxTrx, TransactionCapsule.getStorageLimitInTrx(contract));
      castMaxTrx = Math.addExact(castMaxTrx, TransactionCapsule.getCallValue(contract));
    }
    if (owner.getBalance() < castMaxTrx) {
      throw new ContractValidateException(
          "init trace error, balance is not sufficient.");
    }
//    receipt.payCpuBill();
    checkStorage();
  }

  //set bill
  public void setBill(long cpuUseage, long storageDelta) {
    receipt.setCpuUsage(cpuUseage);
    receipt.setStorageDelta(storageDelta);
  }

  private void checkStorage() {
    //TODO if not enough buy some storage auto
    receipt.buyStorage(0);
  }

  public void exec(Runtime runtime) throws ContractExeException, ContractValidateException {
    /**  VM execute  **/
    runtime.init();
    runtime.execute();
    runtime.go();
  }

  public void finalize() {
    //TODO: if SR package this this trx, use their receipt
    ReceiptCapsule witReceipt = new ReceiptCapsule(trx.getInstance().getRet(0).getReceipt(),
        trx.getTransactionId());
    if (0 == witReceipt.getCpuUsage() && 0 == witReceipt.getStorageDelta()) {
      return;
    }
    //TODO calculatedly pay cpu pay storage
    receipt.payCpuBill();
    receipt.payStorageBill();
    //TODO: pay bill
  }

  public void checkBill() {
    //TODO: check SR's bill and ours.
  }

}
