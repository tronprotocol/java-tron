package org.tron.core.db.actuator;

import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;

public abstract class AbstractTransactionActuator implements Actuator {

  protected TransactionCapsule transactionCapsule;
  protected Manager dbManager;

  AbstractTransactionActuator(TransactionCapsule transactionCapsule, Manager dbManager) {
    this.transactionCapsule = transactionCapsule;
    this.dbManager = dbManager;
  }
}
