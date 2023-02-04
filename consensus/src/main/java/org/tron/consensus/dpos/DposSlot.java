package org.tron.consensus.dpos;


import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.ChainConstant.SINGLE_REPEAT;

import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.consensus.ConsensusDelegate;

@Slf4j(topic = "consensus")
@Component
public class DposSlot {

  @Autowired
  private ConsensusDelegate consensusDelegate;

  @Setter
  @Getter
  private DposService dposService;

  public long getAbSlot(long time) {
    return (time - dposService.getGenesisBlockTime()) / BLOCK_PRODUCED_INTERVAL;
  }

  public long getSlot(long time) {
    long firstSlotTime = getTime(1);
    if (time < firstSlotTime) {
      return 0;
    }
    return (time - firstSlotTime) / BLOCK_PRODUCED_INTERVAL + 1;
  }

  public long getTime(long slot) {
    if (slot == 0) {
      return System.currentTimeMillis();
    }
    long interval = BLOCK_PRODUCED_INTERVAL;
    if (consensusDelegate.getLatestBlockHeaderNumber() == 0) {
      logger.info("stress-test: consensusDelegate.getLatestBlockHeaderNumber(): {}", consensusDelegate.getLatestBlockHeaderNumber());
      long l = dposService.getGenesisBlockTime() + slot * interval;
      logger.info("stress-test: dposService.getGenesisBlockTime(): {}",  dposService.getGenesisBlockTime() + slot * interval);
      return l;
    }

    if (consensusDelegate.lastHeadBlockIsMaintenance()) {
      logger.info("stress-test: consensusDelegate.lastHeadBlockIsMaintenance(): {}", consensusDelegate.lastHeadBlockIsMaintenance());
      slot += consensusDelegate.getMaintenanceSkipSlots();
      logger.info("stress-test: slot: {}", slot);
    }
    long time = consensusDelegate.getLatestBlockHeaderTimestamp();
    logger.info("stress-test: consensusDelegate.getLatestBlockHeaderTimestamp: {}", time);
    time = time - ((time - dposService.getGenesisBlockTime()) % interval);
    logger.info("stress-test: time + interval * slot: {}", time + interval * slot);
    return time + interval * slot;
  }

  public ByteString getScheduledWitness(long slot) {
    final long currentSlot = getAbSlot(consensusDelegate.getLatestBlockHeaderTimestamp()) + slot;
    if (currentSlot < 0) {
      throw new RuntimeException("current slot should be positive.");
    }
    int size = consensusDelegate.getActiveWitnesses().size();
    if (size <= 0) {
      throw new RuntimeException("active witnesses is null.");
    }
    int witnessIndex = (int) currentSlot % (size * SINGLE_REPEAT);
    witnessIndex /= SINGLE_REPEAT;
    return consensusDelegate.getActiveWitnesses().get(witnessIndex);
  }

}
