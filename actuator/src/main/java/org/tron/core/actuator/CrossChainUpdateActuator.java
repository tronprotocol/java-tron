package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.CommonDataBase;
import org.tron.core.db.CrossRevokingStore;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol;
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
      long registerNum = crossChainInfo.getRegisterNum();
      if (chainBaseManager.chainIsSelectedByRegisterNum(crossChainInfo.getRegisterNum())
              && getTx().getBlockNum() != -1) {
        crossRevokingStore.saveCrossChainUpdate(registerNum, getTx().getBlockNum());
      }

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

    if (crossChainInfo.getChainId().toByteArray().length != ActuatorConstant.CHAIN_ID_LENGTH) {
      throw new ContractValidateException("Invalid chainId!");
    }

    String chainId = ByteArray.toHexString(crossChainInfo.getChainId().toByteArray());
    long registerNum = crossChainInfo.getRegisterNum();
    byte[] ownerAddress = crossChainInfo.getOwnerAddress().toByteArray();
    byte[] proxyAddress = crossChainInfo.getProxyAddress().toByteArray();

    byte[] crossChainInfoBytes = crossRevokingStore.getChainInfo(registerNum);
    BalanceContract.CrossChainInfo crossChainInfoOld = null;

    if (registerNum <= 0) {
      throw new ContractValidateException("Invalid registerNum!");
    }

    if (crossChainInfoBytes == null) {
      throw new ContractValidateException("registerNum has not been registered!");
    }

    try {
      crossChainInfoOld = BalanceContract.CrossChainInfo.parseFrom(crossChainInfoBytes);
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException("the format of crossChainInfo stored in db is not right!");
    }

    long beginSyncHeightOld = crossChainInfoOld.getBeginSyncHeight();
    long latestHeaderBlockNum =
            chainBaseManager.getCommonDataBase().getLatestHeaderBlockNum(chainId);
    if ((chainBaseManager.chainIsSelectedByRegisterNum(crossChainInfo.getRegisterNum())
            && latestHeaderBlockNum >= beginSyncHeightOld)
            || chainBaseManager.chainIsSelectedTwice(crossChainInfo.getChainId())) {
      throw new ContractValidateException("ChainId or registerNum has been selected!");
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

    if (!DecodeUtil.addressValid(proxyAddress)) {
      throw new ContractValidateException("Invalid proxyAddress!");
    }

    HashSet<String> paraChainsHistory = (HashSet<String>) crossRevokingStore.getParaChainsHistory();
    if (paraChainsHistory != null && paraChainsHistory.contains(chainId)
            && !Arrays.equals(crossChainInfoOld.getProxyAddress().toByteArray(), proxyAddress)) {
      throw new ContractValidateException("elected parallel chains can no longer modify the proxy address!");
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
    return 1_000_000L; // 1TRX
  }

}
