package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.CrossRevokingStore;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.BalanceContract.UpdateCrossContract;

@Slf4j(topic = "actuator")
public class CrossUpdateActuator extends AbstractActuator {

  public CrossUpdateActuator() {
    super(ContractType.UpdateCrossContract, UpdateCrossContract.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) object;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();
    try {
      UpdateCrossContract updateCrossContract = any.unpack(UpdateCrossContract.class);
      byte[] ownerAddress = updateCrossContract.getOwnerAddress().toByteArray();
      String chainId = updateCrossContract.getCrossChainInfo().getChainId().toString();
      Commons.adjustBalance(accountStore, ownerAddress, -fee);
      Commons.adjustBalance(accountStore, accountStore.getBlackhole().createDbKey(), fee);
      crossRevokingStore.putChainInfo(chainId, updateCrossContract.getCrossChainInfo().toByteArray());
      ret.setStatus(fee, code.SUCESS);
    } catch (BalanceInsufficientException | ArithmeticException | InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.any == null) {
      throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
    }
    if (chainBaseManager == null) {
      throw new ContractValidateException(ActuatorConstant.STORE_NOT_EXIST);
    }
    CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    if (!this.any.is(UpdateCrossContract.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [UpdateCrossContract], real type [" + this.any
              .getClass() + "]");
    }
    final UpdateCrossContract updateCrossContract;
    try {
      updateCrossContract = any.unpack(UpdateCrossContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    byte[] chainId = updateCrossContract.getCrossChainInfo().getChainId().toByteArray();
    byte[] ownerAddress = updateCrossContract.getOwnerAddress().toByteArray();

    byte[] crossChainInfoBytes = crossRevokingStore.getChainInfo(ByteArray.toStr(chainId));
    BalanceContract.CrossChainInfo crossChainInfo = null;

    if (crossChainInfoBytes == null) {
      throw new ContractValidateException("ChainId has not been registered!");
    }

    try {
      crossChainInfo = BalanceContract.CrossChainInfo.parseFrom(crossChainInfoBytes);
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException("the format of crossChainInfo stored in db is not right!");
    }

    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress!");
    }

    if (!Arrays.equals(crossChainInfo.toByteArray(), ownerAddress)) {
      throw new ContractValidateException("ownerAddress must be the same with the register address!");
    }

    AccountCapsule ownerAccount = accountStore.get(ownerAddress);

    if (ownerAccount == null) {
      throw new ContractValidateException("Validate UpdateCrossActuator error, no OwnerAccount.");
    }

    long balance = ownerAccount.getBalance();

    if (balance <= calcFee()) {
      throw new ContractValidateException("Validate UpdateCrossContract error, balance is not sufficient.");
    }

    // todo: whether check all params?

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(UpdateCrossContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 1_000_000L; // 1TRX
  }

}
