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
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.CrossChain.UnvoteCrossContract;

@Slf4j(topic = "actuator")
public class UnvoteCrossActuator extends AbstractActuator {


  public UnvoteCrossActuator() {
    super(ContractType.UnvoteCrossContract, UnvoteCrossContract.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) object;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    try {
      UnvoteCrossContract voteCrossContract = any.unpack(UnvoteCrossContract.class);
      AccountStore accountStore = chainBaseManager.getAccountStore();
      CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();
      String chainId = voteCrossContract.getChainId().toString();
      byte[] address = voteCrossContract.getOwnerAddress().toByteArray();
      long voted = crossRevokingStore.getChainVote(chainId, ByteArray.toHexString(address));
      Commons.adjustBalance(accountStore, address, voted);
      crossRevokingStore.deleteChainVote(chainId, ByteArray.toHexString(address));
      crossRevokingStore.updateTotalChainVote(chainId, -voted);

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
    CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();
    if (!this.any.is(UnvoteCrossContract.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [VoteCrossContract], real type[" + any
              .getClass() + "]");
    }
    final UnvoteCrossContract contract;
    try {
      contract = this.any.unpack(UnvoteCrossContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    String chainId = contract.getChainId().toString();
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    String readableOwnerAddress = ByteArray.toHexString(ownerAddress);
    long voteCountBefore = crossRevokingStore.getChainVote(chainId, readableOwnerAddress);

    if (voteCountBefore == 0) {
      throw new ContractValidateException(
          "this address has not voted for this chain.");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(UnvoteCrossContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 1_000_000L;
  }

}
