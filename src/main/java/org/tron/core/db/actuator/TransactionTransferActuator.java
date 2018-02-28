package org.tron.core.db.actuator;

import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TronDatabase;

public class TransactionTransferActuator extends AbstractTransactionActuator {


  TransactionTransferActuator(TransactionCapsule transactionCapsule,
      TronDatabase tronDatabase) {
    super(transactionCapsule, tronDatabase);
  }

  @Override
  public boolean execute() {
    //TODO
    return true;
  }
}
