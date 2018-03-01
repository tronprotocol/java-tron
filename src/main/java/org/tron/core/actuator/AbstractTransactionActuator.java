package org.tron.core.actuator;

import com.google.protobuf.Any;
import org.tron.core.db.Manager;

public abstract class AbstractTransactionActuator implements Actuator {

  protected Any contract;
  protected Manager dbManager;

  AbstractTransactionActuator(Any contract, Manager dbManager) {
    this.contract = contract;
    this.dbManager = dbManager;
  }
}
