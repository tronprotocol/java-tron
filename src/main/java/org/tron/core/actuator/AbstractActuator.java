package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.storage.Deposit;
import org.tron.core.Constant;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.protos.Protocol.Transaction.Result.code;
@Slf4j(topic = "actuator")
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

  public boolean deductDeferredFee(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcDeferredFee();
    try {
      byte[] ownerAddress = getOwnerAddress().toByteArray();
      dbManager.adjustBalance(ownerAddress, -fee);
      dbManager.adjustBalance(dbManager.getAccountStore().getBlackhole().createDbKey(), fee);
    } catch (BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
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
