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
import org.tron.protos.contract.CrossChain.VoteCrossChainContract;

@Slf4j(topic = "actuator")
public class VoteCrossChainActuator extends AbstractActuator {


  public VoteCrossChainActuator() {
    super(ContractType.VoteCrossChainContract, VoteCrossChainContract.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) object;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    try {
      VoteCrossChainContract voteCrossContract = any.unpack(VoteCrossChainContract.class);
      AccountStore accountStore = chainBaseManager.getAccountStore();
      CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();
      long registerNum = voteCrossContract.getRegisterNum();
      long amount = voteCrossContract.getAmount();
      byte[] address = voteCrossContract.getOwnerAddress().toByteArray();
      int round = voteCrossContract.getRound();

      byte[] voteCrossInfoBytes =
              crossRevokingStore.getChainVote(round, registerNum, ByteArray.toHexString(address));
      if (!ByteArray.isEmpty(voteCrossInfoBytes)) {
        VoteCrossChainContract voteCrossInfo = VoteCrossChainContract.parseFrom(voteCrossInfoBytes);
        VoteCrossChainContract.Builder builder = voteCrossContract.toBuilder();
        builder.setAmount(voteCrossInfo.getAmount() + amount);
        voteCrossContract = builder.build();
      }

      Commons.adjustBalance(accountStore, address, -amount);
      crossRevokingStore.putChainVote(round, registerNum,
              ByteArray.toHexString(address), voteCrossContract.toByteArray());
      crossRevokingStore.updateTotalChainVote(round, registerNum, amount);

      Commons.adjustBalance(accountStore, address, -fee);
      Commons.adjustBalance(accountStore, accountStore.getBlackhole().createDbKey(), fee);
      ret.setStatus(fee, code.SUCESS);
    } catch (InvalidProtocolBufferException | BalanceInsufficientException e) {
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

    AccountStore accountStore = chainBaseManager.getAccountStore();
    if (!this.any.is(VoteCrossChainContract.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [VoteCrossContract], real type[" + any
              .getClass() + "]");
    }
    final VoteCrossChainContract contract;
    try {
      contract = this.any.unpack(VoteCrossChainContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    long registerNum = contract.getRegisterNum();
    long amount = contract.getAmount();
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    int round = contract.getRound();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }
    if (amount <= 0L) {
      throw new ContractValidateException("the amount of votes must be greater than 0");
    }
    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    if (accountCapsule.getBalance() - amount < 0) {
      throw new ContractValidateException(
              "Validate VoteCrossContract error, balance is not sufficient.");
    }

    if (registerNum <= 0) {
      throw new ContractValidateException("Invalid registerNum");
    }

    if (round <= 0) {
      throw new ContractValidateException("Invalid round");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(VoteCrossChainContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 1_000_000L;
  }

}
