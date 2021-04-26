package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.CrossRevokingStore;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.CrossChain;
import org.tron.protos.contract.CrossChain.UnvoteCrossChainContract;

@Slf4j(topic = "actuator")
public class UnvoteCrossChainActuator extends AbstractActuator {


  public UnvoteCrossChainActuator() {
    super(ContractType.UnvoteCrossChainContract, UnvoteCrossChainContract.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) object;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    try {
      UnvoteCrossChainContract VoteCrossChainContract = any.unpack(UnvoteCrossChainContract.class);
      AccountStore accountStore = chainBaseManager.getAccountStore();
      CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();
      String chainId = ByteArray.toHexString(VoteCrossChainContract.getChainId().toByteArray());
      byte[] address = VoteCrossChainContract.getOwnerAddress().toByteArray();
      int round = VoteCrossChainContract.getRound();
      byte[] crossVoteInfoBytes = crossRevokingStore.getChainVote(round, chainId, ByteArray.toHexString(address));
      long voted = 0;
      if (!ByteArray.isEmpty(crossVoteInfoBytes)) {
        CrossChain.VoteCrossChainContract voteCrossInfo = CrossChain.VoteCrossChainContract.parseFrom(crossVoteInfoBytes);
        voted = voteCrossInfo.getAmount();
      }
      Commons.adjustBalance(accountStore, address, voted);
      crossRevokingStore.deleteChainVote(round, chainId, ByteArray.toHexString(address));
      crossRevokingStore.updateTotalChainVote(round, chainId, -voted);

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
    if (!this.any.is(UnvoteCrossChainContract.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [VoteCrossChainContract], real type[" + any
              .getClass() + "]");
    }
    final UnvoteCrossChainContract contract;
    try {
      contract = this.any.unpack(UnvoteCrossChainContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    String chainId = ByteArray.toHexString(contract.getChainId().toByteArray());
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    String readableOwnerAddress = ByteArray.toHexString(ownerAddress);
    int round = contract.getRound();
    byte[] voteCrossInfoBytes = crossRevokingStore.getChainVote(round, chainId, readableOwnerAddress);
    if (ByteArray.isEmpty(voteCrossInfoBytes)) {
      throw new ContractValidateException(
              "this address has not voted for this chain.");
    } else {
      try {
        CrossChain.VoteCrossChainContract voteCrossInfo = CrossChain.VoteCrossChainContract.parseFrom(voteCrossInfoBytes);
        long voteCountBefore = voteCrossInfo.getAmount();

        if (voteCountBefore == 0) {
          throw new ContractValidateException(
                  "this address has not voted for this chain.");
        }
      } catch (InvalidProtocolBufferException e) {
        logger.debug(e.getMessage(), e);
        throw new ContractValidateException(e.getMessage());
      }
    }



    List<String> paraChainList = crossRevokingStore.getParaChainList(round);
    if (paraChainList.contains(chainId)) {
      throw new ContractValidateException(
              "can not unvote from a parachain");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(UnvoteCrossChainContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 1_000_000L;
  }

}
