package org.tron.core.db.actuator;

import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;

public class TransactionTransferActuator extends AbstractTransactionActuator {


  TransactionTransferActuator(TransactionCapsule transactionCapsule,
      Manager dbManager) {
    super(transactionCapsule, dbManager);
  }

  @Override
  public boolean execute() {
    //TODO
    return true;
  }
}
