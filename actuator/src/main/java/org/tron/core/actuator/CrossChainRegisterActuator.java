package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
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
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.BalanceContract.CrossChainInfo;

@Slf4j(topic = "actuator")
public class CrossChainRegisterActuator extends AbstractActuator {

  public CrossChainRegisterActuator() {
    super(ContractType.RegisterCrossChainContract, CrossChainInfo.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) object;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();
    try {
      CrossChainInfo crossChainInfo = any.unpack(CrossChainInfo.class);
      byte[] ownerAddress = crossChainInfo.getOwnerAddress().toByteArray();
      String chainId = ByteArray.toHexString(crossChainInfo.getChainId().toByteArray());
      long burn = dynamicStore.getBurnedForRegisterCross();
      Commons.adjustBalance(accountStore, ownerAddress, -burn);
      Commons.adjustBalance(accountStore, accountStore.getBlackhole().createDbKey(), burn);
      crossRevokingStore.putChainInfo(chainId, crossChainInfo.toByteArray());
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
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    if (!this.any.is(CrossChainInfo.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [RegisterCrossContract], real type [" + this.any
              .getClass() + "]");
    }
    final CrossChainInfo CrossChainInfo;
    try {
      CrossChainInfo = any.unpack(CrossChainInfo.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    String chainId = ByteArray.toHexString(CrossChainInfo.getChainId().toByteArray());
    byte[] ownerAddress = CrossChainInfo.getOwnerAddress().toByteArray();

    // check chain_id is exist
    if (crossRevokingStore.getChainInfo(chainId) != null) {
      throw new ContractValidateException("ChainId has already been registered!");
    }

    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress!");
    }

    AccountCapsule ownerAccount = accountStore.get(ownerAddress);

    if (ownerAccount == null) {
      throw new ContractValidateException("Validate RegisterCrossActuator error, no OwnerAccount.");
    }

    long balance = ownerAccount.getBalance();

    if (balance <= dynamicStore.getBurnedForRegisterCross()) {
      throw new ContractValidateException("OwnerAccount balance must be greater than BURNED_FOR_REGISTER_CROSS.");
    }

    // todo: whether check all params?

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(CrossChainInfo.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0L;
  }

}
