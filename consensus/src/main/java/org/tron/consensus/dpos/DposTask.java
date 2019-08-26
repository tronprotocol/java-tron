package org.tron.consensus.dpos;

import static org.tron.consensus.base.Constant.BLOCK_PRODUCED_INTERVAL;

import com.google.protobuf.ByteString;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.ConsensusDelegate;
import org.tron.consensus.base.State;
import org.tron.protos.Protocol.Block;
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

  private Thread produceThread;

  private volatile boolean isRunning = true;

  public void init() {

    if (!dposService.isEnable() || StringUtils.isEmpty(dposService.getMiners())) {
      return;
    }

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
        } catch (Throwable throwable) {
          logger.error("Produce block error.", throwable);
        }
      }
    };
    produceThread = new Thread(runnable, "DPosMiner");
    produceThread.start();
    logger.info("DPoS service stared.");
  }

  public void stop() {
    isRunning = false;
    if (produceThread != null) {
      produceThread.interrupt();
    }
    logger.info("DPoS service stopped.");
  }

  private State produceBlock() {

    State state = stateManager.getState();
    if (!State.OK.equals(state)) {
      return state;
    }

    synchronized (dposService.getBlockHandle().getLock()) {

      long slot = dposSlot.getSlot(System.currentTimeMillis() + 50);
      if (slot == 0) {
        return State.NOT_TIME_YET;
      }

      ByteString pWitness = dposSlot.getScheduledWitness(slot);
      state = stateManager.getState(pWitness);
      if (!State.OK.equals(state)) {
        return state;
      }

      long pTime = dposSlot.getTime(slot);
      long timeout =
          pTime + BLOCK_PRODUCED_INTERVAL / 2 * dposService.getBlockProduceTimeoutPercent() / 100;
      Block block = dposService.getBlockHandle().produce(timeout);
      if (block == null) {
        return State.PRODUCE_BLOCK_FAILED;
      }

      Block sBlock = getSignedBlock(block, pWitness, pTime);

      stateManager.setCurrentBlock(sBlock);

      dposService.getBlockHandle().complete(sBlock);

      BlockHeader.raw raw = sBlock.getBlockHeader().getRawData();
      logger.info("Produce block successfully, num:{}, time:{}, witness:{}, hash:{} parentHash:{}",
          raw.getNumber(),
          new DateTime(raw.getTimestamp()),
          ByteArray.toHexString(raw.getWitnessAddress().toByteArray()),
          DposService.getBlockHash(sBlock),
          ByteArray.toHexString(raw.getParentHash().toByteArray()));
    }

    return State.OK;
  }

  public Block getSignedBlock(Block block, ByteString witness, long time) {
    BlockHeader.raw raw = block.getBlockHeader().getRawData().toBuilder()
        .setParentHash(ByteString.copyFrom(consensusDelegate.getLatestBlockHeaderHash().getBytes()))
        .setNumber(consensusDelegate.getLatestBlockHeaderNumber() + 1)
        .setTimestamp(time)
        .setWitnessAddress(witness)
        .build();

    ECKey ecKey = ECKey.fromPrivate(dposService.getMiners().get(witness).getPrivateKey());
    ECDSASignature signature = ecKey.sign(Sha256Hash.of(raw.toByteArray()).getBytes());
    ByteString sign = ByteString.copyFrom(signature.toByteArray());

    BlockHeader blockHeader = block.getBlockHeader().toBuilder()
        .setRawData(raw)
        .setWitnessSignature(sign)
        .build();

    Block signedBlock = block.toBuilder().setBlockHeader(blockHeader).build();

    return signedBlock;
  }

}
