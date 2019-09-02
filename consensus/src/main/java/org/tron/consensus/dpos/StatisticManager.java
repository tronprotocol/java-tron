package org.tron.consensus.dpos;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.consensus.ConsensusDelegate;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.protos.Protocol.Block;

@Slf4j(topic = "consensus")
@Component
public class StatisticManager {

  @Autowired
  private ConsensusDelegate consensusDelegate;

  @Autowired
  private DposSlot dposSlot;

  public void applyBlock(Block block) {
    WitnessCapsule wc;
    long blockNum = block.getBlockHeader().getRawData().getNumber();
    long blockTime = block.getBlockHeader().getRawData().getTimestamp();
    byte[] blockWitness = block.getBlockHeader().getRawData().getWitnessAddress().toByteArray();
    wc = consensusDelegate.getWitnessStore().getUnchecked(blockWitness);
    wc.setTotalProduced(wc.getTotalProduced() + 1);
    wc.setLatestBlockNum(blockNum);
    wc.setLatestSlotNum(dposSlot.getAbSlot(blockTime));
    consensusDelegate.getWitnessStore().put(blockWitness, wc);

    long slot = 1;
    if (blockNum != 1) {
      slot = dposSlot.getSlot(blockTime);
    }
    for (int i = 1; i < slot; ++i) {
      byte[] witness = dposSlot.getScheduledWitness(i).toByteArray();
      wc = consensusDelegate.getWitnessStore().getUnchecked(witness);
      wc.setTotalMissed(wc.getTotalMissed() + 1);
      consensusDelegate.getWitnessStore().put(witness, wc);
      logger.info("Current block: {}, witness: {} totalMissed: {}",
          blockNum, wc.createReadableString(), wc.getTotalMissed());
      consensusDelegate.applyBlock(false);
    }
    consensusDelegate.applyBlock(true);
  }
}