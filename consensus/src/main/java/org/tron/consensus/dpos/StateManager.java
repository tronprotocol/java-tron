package org.tron.consensus.dpos;

import static org.tron.core.config.args.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.args.Parameter.ChainConstant.MAX_ACTIVE_WITNESS_NUM;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.consensus.ConsensusDelegate;
import org.tron.consensus.base.Param.Miner;
import org.tron.consensus.base.State;
import org.tron.core.capsule.AccountCapsule;
import org.tron.protos.Protocol.Block;

@Slf4j(topic = "consensus")
@Component
public class StateManager {

  @Autowired
  private ConsensusDelegate consensusDelegate;

  @Setter
  private DposService dposService;

  @Setter
  private volatile Block currentBlock;

  private AtomicInteger dupBlockCount = new AtomicInteger(0);

  private AtomicLong dupBlockTime = new AtomicLong(0);

  private long blockCycle = BLOCK_PRODUCED_INTERVAL * MAX_ACTIVE_WITNESS_NUM;


  public State getState() {

    if (System.currentTimeMillis() < consensusDelegate.getLatestBlockHeaderTimestamp()) {
      return State.CLOCK_ERROR;
    }

    State status = dposService.getBlockHandle().getState();
    if (!State.OK.equals(status)) {
      return status;
    }

    if (isDupWitness()) {
      return State.DUP_WITNESS;
    }

    int participation = consensusDelegate.calculateFilledSlotsCount();
    int minParticipationRate = dposService.getMinParticipationRate();
    if (participation < minParticipationRate) {
      logger
          .warn("Participation:{} <  minParticipationRate:{}", participation, minParticipationRate);
      return State.LOW_PARTICIPATION;
    }

    return State.OK;
  }

  public State getState(ByteString scheduledWitness) {
    Miner miner = dposService.getMiners().get(scheduledWitness);
    if (miner == null) {
      return State.NOT_MY_TURN;
    }
    return State.OK;
  }

  public void receiveBlock(Block block) {
    if (block.equals(currentBlock)) {
      return;
    }

    if (dposService.isNeedSyncCheck()) {
      return;
    }

    long blockTime = block.getBlockHeader().getRawData().getTimestamp();
    if (System.currentTimeMillis() - blockTime > BLOCK_PRODUCED_INTERVAL) {
      return;
    }

    ByteString witness = block.getBlockHeader().getRawData().getWitnessAddress();
    if (!dposService.getMiners().containsKey(witness)) {
      return;
    }

    if (dupBlockCount.get() == 0) {
      dupBlockCount.set(new Random().nextInt(10));
    } else {
      dupBlockCount.set(10);
    }

    dupBlockTime.set(System.currentTimeMillis());

    logger.warn("Dup block produced: {}", block);
  }

  private boolean isDupWitness() {
    if (dupBlockCount.get() == 0) {
      return false;
    }
    if (System.currentTimeMillis() - dupBlockTime.get() > dupBlockCount.get() * blockCycle) {
      dupBlockCount.set(0);
      return false;
    }
    return true;
  }

}
