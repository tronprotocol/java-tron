package org.tron.consensus.dpos;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.consensus.ConsensusDelegate;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;

@Slf4j(topic = "consensus")
@Component
public class StatisticManager {

  @Autowired
  private ConsensusDelegate consensusDelegate;

  @Autowired
  private DposSlot dposSlot;

  public void applyBlock(BlockCapsule blockCapsule) {
    WitnessCapsule wc;
    long blockNum = blockCapsule.getNum();
    long blockTime = blockCapsule.getTimeStamp();
    byte[] blockWitness = blockCapsule.getWitnessAddress().toByteArray();
    wc = consensusDelegate.getWitness(blockWitness);
    wc.setTotalProduced(wc.getTotalProduced() + 1);
    wc.setLatestBlockNum(blockNum);
    wc.setLatestSlotNum(dposSlot.getAbSlot(blockTime));
    consensusDelegate.saveWitness(wc);

    long slot = 1;
    if (blockNum != 1) {
      slot = dposSlot.getSlot(blockTime);
    }
    for (int i = 1; i < slot; ++i) {
      byte[] witness = dposSlot.getScheduledWitness(i).toByteArray();
      wc = consensusDelegate.getWitness(witness);
      wc.setTotalMissed(wc.getTotalMissed() + 1);
      consensusDelegate.saveWitness(wc);
      logger.info("Current block: {}, witness: {} totalMissed: {}",
          blockNum, wc.createReadableString(), wc.getTotalMissed());
      consensusDelegate.applyBlock(false);
    }
    consensusDelegate.applyBlock(true);
  }
}