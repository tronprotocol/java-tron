package org.tron.consensus.dpos;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

import com.google.protobuf.ByteString;
import java.util.concurrent.ExecutorService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.ConsensusDelegate;
import org.tron.consensus.base.Param.Miner;
import org.tron.consensus.base.State;
import org.tron.core.capsule.BlockCapsule;
import org.tron.protos.Protocol.BlockHeader;

@Slf4j(topic = "consensus")
@Component
public class DposTask {

  @Autowired
  private ConsensusDelegate consensusDelegate;

  @Autowired
  private DposSlot dposSlot;

  @Autowired
  private StateManager stateManager;

  @Setter
  private DposService dposService;

  private ExecutorService produceExecutor;

  private final String name = "DPosMiner";

  private volatile boolean isRunning = true;

  public void init() {

    if (!dposService.isEnable() || ObjectUtils.isEmpty(dposService.getMiners())) {
      return;
    }
    produceExecutor = ExecutorServiceManager.newSingleThreadExecutor(name);
    Runnable runnable = () -> {
      while (isRunning) {
        try {
          if (dposService.isNeedSyncCheck()) {
            Thread.sleep(1000);
            dposService.setNeedSyncCheck(dposSlot.getTime(1) < System.currentTimeMillis());
          } else {
            long time =
                BLOCK_PRODUCED_INTERVAL - System.currentTimeMillis() % BLOCK_PRODUCED_INTERVAL;
            Thread.sleep(time);
            State state = produceBlock();
            if (!State.OK.equals(state)) {
              logger.info("Produce block failed: {}", state);
            }
          }
        } catch (InterruptedException e) {
          logger.warn("Produce block task interrupted.");
          Thread.currentThread().interrupt();
        } catch (Throwable throwable) {
          logger.error("Produce block error.", throwable);
        }
      }
    };
    produceExecutor.submit(runnable);
    logger.info("DPoS task started.");
  }

  public void stop() {
    isRunning = false;
    ExecutorServiceManager.shutdownAndAwaitTermination(produceExecutor, name);
  }

  private State produceBlock() {

    State state = stateManager.getState();
    if (!State.OK.equals(state)) {
      return state;
    }

    dposService.getBlockHandle().setBlockWaitLock(true);
    try {
      synchronized (dposService.getBlockHandle().getLock()) {

        state = stateManager.getState();
        if (!State.OK.equals(state)) {
          return state;
        }

        long slot = dposSlot.getSlot(System.currentTimeMillis() + 50);
        if (slot == 0) {
          return State.NOT_TIME_YET;
        }

        ByteString pWitness = dposSlot.getScheduledWitness(slot);

        Miner miner = dposService.getMiners().get(pWitness);
        if (miner == null) {
          return State.NOT_MY_TURN;
        }

        long pTime = dposSlot.getTime(slot);
        long timeout =
                pTime + BLOCK_PRODUCED_INTERVAL / 2 * dposService.getBlockProduceTimeoutPercent() / 100;
        BlockCapsule blockCapsule = dposService.getBlockHandle().produce(miner, pTime, timeout);
        if (blockCapsule == null) {
          return State.PRODUCE_BLOCK_FAILED;
        }

        BlockHeader.raw raw = blockCapsule.getInstance().getBlockHeader().getRawData();
        logger.info("Produce block successfully, num: {}, time: {}, witness: {}, ID:{}, parentID:{}",
                raw.getNumber(),
                new DateTime(raw.getTimestamp()),
                ByteArray.toHexString(raw.getWitnessAddress().toByteArray()),
                new Sha256Hash(raw.getNumber(), Sha256Hash.of(CommonParameter
                        .getInstance().isECKeyCryptoEngine(), raw.toByteArray())),
                ByteArray.toHexString(raw.getParentHash().toByteArray()));
      }
    } finally {
      dposService.getBlockHandle().setBlockWaitLock(false);
    }

    return State.OK;
  }

}
