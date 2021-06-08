package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.List;
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
      long registerNum = crossChainInfo.getRegisterNum();
      fee = fee + dynamicStore.getBurnedForRegisterCross();
      Commons.adjustBalance(accountStore, ownerAddress, -fee);
      Commons.adjustBalance(accountStore, accountStore.getBlackhole().createDbKey(), fee);
      crossRevokingStore.putChainInfo(registerNum, crossChainInfo.toByteArray());
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

    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    if (!dynamicStore.allowCrossChain()) {
      throw new ContractValidateException("not support cross chain!");
    }

    CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    if (!this.any.is(CrossChainInfo.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [RegisterCrossContract], real type [" + this.any
              .getClass() + "]");
    }
    final CrossChainInfo crossChainInfo;
    try {
      crossChainInfo = any.unpack(CrossChainInfo.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    String chainId = ByteArray.toHexString(crossChainInfo.getChainId().toByteArray());
    byte[] ownerAddress = crossChainInfo.getOwnerAddress().toByteArray();
    byte[] proxyAddress = crossChainInfo.getProxyAddress().toByteArray();
    long registerNum = crossChainInfo.getRegisterNum();
    if (registerNum <= 0) {
      throw new ContractValidateException("Invalid registerNum!");
    }
    // check chain_id is exist
    if (chainId.isEmpty()) {
      throw new ContractValidateException("No chainId!");
    }
    if (crossChainInfo.getChainId().toByteArray().length != ActuatorConstant.CHAIN_ID_LENGTH) {
      throw new ContractValidateException("Invalid chainId!");
    }
    if (crossRevokingStore.getChainInfo(registerNum) != null) {
      throw new ContractValidateException("registerNum has already been registered!");
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

    if (!DecodeUtil.addressValid(proxyAddress)) {
      throw new ContractValidateException("Invalid proxyAddress!");
    }

    // check sr list
    List<ByteString> srList = crossChainInfo.getSrListList();
    if (srList.isEmpty()) {
      throw new ContractValidateException("Invalid srList!");
    }

    // check other params
    long maintenanceTimeInterval = crossChainInfo.getMaintenanceTimeInterval();
    if (maintenanceTimeInterval <= 0) {
      throw new ContractValidateException("Invalid maintenanceTimeInterval!");
    }
    long blockTime = crossChainInfo.getBlockTime();
    if (blockTime <= 0) {
      throw new ContractValidateException("Invalid blockTime!");
    }
    long beginSyncHeight = crossChainInfo.getBeginSyncHeight();
    if (beginSyncHeight <= 0) {
      throw new ContractValidateException("Invalid beginSyncHeight!");
    }
    String parentBlockHash =
            ByteArray.toHexString(crossChainInfo.getParentBlockHash().toByteArray());
    if (parentBlockHash.isEmpty()) {
      throw new ContractValidateException("No parentBlockHash!");
    }
    if (crossChainInfo.getParentBlockHash().toByteArray().length !=
            ActuatorConstant.CHAIN_ID_LENGTH) {
      throw new ContractValidateException("Invalid parentBlockHash!");
    }

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
