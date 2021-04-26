package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.HashSet;
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
import org.tron.protos.contract.BalanceContract.CrossChainInfo;

@Slf4j(topic = "actuator")
public class CrossChainUpdateActuator extends AbstractActuator {

  public CrossChainUpdateActuator() {
    super(ContractType.UpdateCrossChainContract, CrossChainInfo.class);
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
      CrossChainInfo crossChainInfo = any.unpack(CrossChainInfo.class);
      byte[] ownerAddress = crossChainInfo.getOwnerAddress().toByteArray();
      String chainId = ByteArray.toHexString(crossChainInfo.getChainId().toByteArray());
      Commons.adjustBalance(accountStore, ownerAddress, -fee);
      Commons.adjustBalance(accountStore, accountStore.getBlackhole().createDbKey(), fee);
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
    AccountStore accountStore = chainBaseManager.getAccountStore();
    if (!this.any.is(CrossChainInfo.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [ParaChainInfo], real type [" + this.any
              .getClass() + "]");
    }
    final CrossChainInfo crossChainInfo;
    try {
      crossChainInfo = any.unpack(CrossChainInfo.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    byte[] chainId = crossChainInfo.getChainId().toByteArray();
    byte[] ownerAddress = crossChainInfo.getOwnerAddress().toByteArray();
    byte[] proxyAddress = crossChainInfo.getProxyAddress().toByteArray();

    byte[] crossChainInfoBytes = crossRevokingStore.getChainInfo(ByteArray.toHexString(chainId));
    BalanceContract.CrossChainInfo crossChainInfoOld = null;

    if (crossChainInfoBytes == null) {
      throw new ContractValidateException("ChainId has not been registered!");
    }

    if (chainBaseManager.chainIsSelected(crossChainInfo.getChainId())) {
      throw new ContractValidateException("ChainId has been selected!");
    }

    try {
      crossChainInfoOld = BalanceContract.CrossChainInfo.parseFrom(crossChainInfoBytes);
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException("the format of crossChainInfo stored in db is not right!");
    }

    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress!");
    }

    if (!Arrays.equals(crossChainInfoOld.getOwnerAddress().toByteArray(), ownerAddress)) {
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

    HashSet<String> paraChainsHistory = (HashSet<String>) crossRevokingStore.getParaChainsHistory();
    if (paraChainsHistory != null && paraChainsHistory.contains(ByteArray.toHexString(chainId))
            && !Arrays.equals(crossChainInfoOld.getProxyAddress().toByteArray(), proxyAddress)) {
      throw new ContractValidateException("elected parallel chains can no longer modify the proxy address!");
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
    return 1_000_000L; // 1TRX
  }

}
