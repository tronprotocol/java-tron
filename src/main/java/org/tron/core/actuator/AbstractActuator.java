package org.tron.core.actuator;

import com.google.protobuf.Any;
import org.tron.common.storage.Deposit;
import org.tron.core.db.Manager;

public abstract class AbstractActuator implements Actuator {

  protected Any contract;
  protected Manager dbManager;
  protected DeferredStage deferredStage;

  public Deposit getDeposit() {
    return deposit;
  }

  public void setDeposit(Deposit deposit) {
    this.deposit = deposit;
  }

  public long calcDeferredFee() {
    return dbManager.getDynamicPropertiesStore().getDeferredTransactionFee() *
        (deferredStage.delaySeconds / ActuatorConstant.SECONDS_EACH_DAY + 1);
  }

  protected Deposit deposit;

  AbstractActuator(Any contract, Manager dbManager) {
    this.contract = contract;
    this.dbManager = dbManager;
  }

  AbstractActuator(Any contract, Manager dbManager, DeferredStage actuatorType) {
    this.contract = contract;
    this.dbManager = dbManager;
    this.deferredStage = actuatorType;
  }
}
