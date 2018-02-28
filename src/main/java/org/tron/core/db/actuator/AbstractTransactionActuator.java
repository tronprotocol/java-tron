package org.tron.core.db.actuator;

import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TronDatabase;

public abstract class AbstractTransactionActuator implements Actuator {

  protected TransactionCapsule transactionCapsule;
  protected TronDatabase tronDatabase;

  AbstractTransactionActuator(TransactionCapsule transactionCapsule, TronDatabase tronDatabase) {
    this.transactionCapsule = transactionCapsule;
  }
}
