package org.tron.consensus.dpos;

import static org.tron.consensus.base.Constant.WITNESS_STANDBY_LENGTH;

import com.google.protobuf.ByteString;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.consensus.ConsensusDelegate;
import org.tron.core.capsule.AccountCapsule;
import org.tron.protos.Protocol.Block;

@Slf4j(topic = "consensus")
@Component
public class IncentiveManager {

  @Autowired
  private ConsensusDelegate consensusDelegate;

  public void applyBlock(Block block) {
    byte[] witness = block.getBlockHeader().getRawData().getWitnessAddress().toByteArray();
    AccountCapsule account = consensusDelegate.getAccount(witness);
    account.setAllowance(account.getAllowance() + consensusDelegate.getWitnessPayPerBlock());
    consensusDelegate.saveAccount(account);
  }

  public void reward(List<ByteString> witnesses) {
    if (witnesses.size() > WITNESS_STANDBY_LENGTH) {
      witnesses = witnesses.subList(0, WITNESS_STANDBY_LENGTH);
    }
    long voteSum = 0;
    for (ByteString witness : witnesses) {
      voteSum += consensusDelegate.getWitness(witness.toByteArray()).getVoteCount();
    }
    if (voteSum <= 0) {
      return;
    }
    long totalPay = consensusDelegate.getWitnessStandbyAllowance();
    for (ByteString witness : witnesses) {
      byte[] address = witness.toByteArray();
      long pay = (long) (consensusDelegate.getWitness(address).getVoteCount() * ((double) totalPay
          / voteSum));
      AccountCapsule accountCapsule = consensusDelegate.getAccount(address);
      accountCapsule.setAllowance(accountCapsule.getAllowance() + pay);
      consensusDelegate.saveAccount(accountCapsule);
    }
  }
}
