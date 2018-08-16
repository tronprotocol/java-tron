package org.tron.core.db;

import org.tron.common.runtime.Runtime;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.ReceiptCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;

public class TransactionTrace {
  private TransactionCapsule trx;

  private ReceiptCapsule receipt;

  public TransactionCapsule getTrx() {
    return trx;
  }

  public TransactionTrace(TransactionCapsule trx) {
    this.trx = trx;
    //TODO: set bill owner
    receipt = new ReceiptCapsule(Sha256Hash.ZERO_HASH);
  }

  //pre transaction check
  public void init() {
    //TODO: check
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
    runtime.execute();
    runtime.go();
  }

  public void finalize() {
    //TODO: if SR package this this trx, use their receipt
    //ReceiptCapsule witReceipt = trx.getInstance().getRet(0).getReceipt()
    receipt.payCpuBill();
    receipt.payStorageBill();
    //TODO: pay bill
  }

  public void checkBill() {
    //TODO: check SR's bill and ours.
  }

}
