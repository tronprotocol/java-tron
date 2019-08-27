package org.tron.consensus.dpos;

import static org.tron.consensus.base.Constant.BLOCK_FILLED_SLOTS_NUMBER;

import com.google.protobuf.ByteString;
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
    long blockNum = block.getBlockHeader().getRawData().getNumber();
    long blockTime = block.getBlockHeader().getRawData().getTimestamp();
    ByteString witness = block.getBlockHeader().getRawData().getWitnessAddress();
    WitnessCapsule witnessCapsule = consensusDelegate.getWitnessStore().getUnchecked(witness.toByteArray());
    witnessCapsule.setTotalProduced(witnessCapsule.getTotalProduced() + 1);
    witnessCapsule.setLatestBlockNum(blockNum);
    witnessCapsule.setLatestSlotNum(dposSlot.getAbSlot(blockTime));
    consensusDelegate.getWitnessStore().put(witnessCapsule.getAddress().toByteArray(), witnessCapsule);

    long slot = 1;
    if (blockNum != 1) {
      slot = dposSlot.getSlot(blockTime);
    }
    for (int i = 1; i < slot; ++i) {
      ByteString scheduledWitness = dposSlot.getScheduledWitness(i);
      if (!scheduledWitness.equals(witness)) {
        WitnessCapsule w = consensusDelegate.getWitnessStore().getUnchecked(scheduledWitness.toByteArray());
        w.setTotalMissed(w.getTotalMissed() + 1);
        consensusDelegate.getWitnessStore().put(scheduledWitness.toByteArray(), w);
        logger.info("{} miss a block. totalMissed = {}", w.createReadableString(), w.getTotalMissed());
      }
      applyBlock(false);
    }
    applyBlock(true);
  }

  public void applyBlock(boolean fillBlock) {
    int[] blockFilledSlots = consensusDelegate.getBlockFilledSlots();
    int blockFilledSlotsIndex = consensusDelegate.getBlockFilledSlotsIndex();
    blockFilledSlots[blockFilledSlotsIndex] = fillBlock ? 1 : 0;
    consensusDelegate.saveBlockFilledSlotsIndex((blockFilledSlotsIndex + 1) % BLOCK_FILLED_SLOTS_NUMBER);
    consensusDelegate.saveBlockFilledSlots(blockFilledSlots);
  }

}