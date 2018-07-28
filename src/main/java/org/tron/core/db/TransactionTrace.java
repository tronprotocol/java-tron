package org.tron.core.db;

import org.tron.core.capsule.ReceiptCapsule;
import org.tron.core.capsule.TransactionCapsule;

public class TransactionTrace {
  private TransactionCapsule trx;

  private ReceiptCapsule receipt;

  public TransactionTrace(TransactionCapsule trx) {
    this.trx = trx;
  }

  public void init() {
    //TODO: check
  }

  public void exec() {
    //TODO:create vm and exec.
  }

  public void finalize() {
    //TODO: pay bill
  }

}
