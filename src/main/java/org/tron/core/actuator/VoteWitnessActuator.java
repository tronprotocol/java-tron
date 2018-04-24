package org.tron.core.actuator;

import com.google.common.math.LongMath;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Iterator;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.AccountStore;
import org.tron.core.db.DynamicPropertiesStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.VoteWitnessContract;
import org.tron.protos.Contract.VoteWitnessContract.Vote;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class VoteWitnessActuator extends AbstractActuator {

  VoteWitnessActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }


  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    try {
      VoteWitnessContract voteContract = contract.unpack(VoteWitnessContract.class);
      countVoteAccount(voteContract);
      ret.setStatus(fee, code.SUCESS);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    try {
      if (!contract.is(VoteWitnessContract.class)) {
        throw new ContractValidateException(
            "contract type error,expected type [VoteWitnessContract],real type[" + contract
                .getClass() + "]");
      }

      VoteWitnessContract contract = this.contract.unpack(VoteWitnessContract.class);
      if (!Wallet.addressValid(contract.getOwnerAddress().toByteArray())) {
        throw new ContractValidateException("Invalidate address");
      }
      ByteString ownerAddress = contract.getOwnerAddress();
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

      AccountStore accountStore = dbManager.getAccountStore();
      byte[] ownerAddressBytes = ownerAddress.toByteArray();

      Iterator<Vote> iterator = contract.getVotesList().iterator();
      while (iterator.hasNext()) {
        Vote vote = iterator.next();
        byte[] bytes = vote.getVoteAddress().toByteArray();
        String readableWitnessAddress = StringUtil.createReadableString(vote.getVoteAddress());
        if (!dbManager.getAccountStore().has(bytes)) {
          throw new ContractValidateException(
              "Account[" + readableWitnessAddress + "] not exists");
        }
        if (!dbManager.getWitnessStore().has(bytes)) {
          throw new ContractValidateException(
              "Witness[" + readableWitnessAddress + "] not exists");
        }
      }

      if (!dbManager.getAccountStore().has(contract.getOwnerAddress().toByteArray())) {
        throw new ContractValidateException(
            "Account[" + readableOwnerAddress + "] not exists");
      }

      if (contract.getVotesCount() > DynamicPropertiesStore.MAX_VOTE_NUMBER) {
        throw new ContractValidateException(
            "VoteNumber more than maxVoteNumber[30]");
      }

      long share = dbManager.getAccountStore().get(contract.getOwnerAddress().toByteArray())
          .getShare();

      Long sum = 0L;
      for (Vote vote : contract.getVotesList()) {
        sum = LongMath.checkedAdd(sum, vote.getVoteCount());
      }

      sum = LongMath.checkedMultiply(sum, 1000000L); //trx -> drop. The vote count is based on TRX
      if (sum > share) {
        throw new ContractValidateException(
            "The total number of votes[" + sum + "] is greater than the share[" + share + "]");
      }

    } catch (Exception ex) {
      ex.printStackTrace();
      throw new ContractValidateException(ex.getMessage());
    }

    return true;
  }

  private void countVoteAccount(VoteWitnessContract voteContract) {

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(voteContract.getOwnerAddress().toByteArray());

    accountCapsule.setInstance(accountCapsule.getInstance().toBuilder().clearVotes().build());

    voteContract.getVotesList().forEach(vote -> {
      //  String toStringUtf8 = vote.getVoteAddress().toStringUtf8();

      logger.debug("countVoteAccount,address[{}]",
          ByteArray.toHexString(vote.getVoteAddress().toByteArray()));

      accountCapsule.addVotes(vote.getVoteAddress(),
          vote.getVoteCount());
    });

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(VoteWitnessContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
