package org.tron.core.services;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.application.Application;
import org.tron.common.application.Service;
import org.tron.common.utils.RandomGenerator;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.witness.BlockProductionCondition;
import org.tron.program.Args;


public class WitnessService implements Service {

  private static final Logger logger = LoggerFactory.getLogger(WitnessService.class);
  private Application tronApp;
  @Getter
  protected WitnessCapsule localWitnessState; //  WitnessId;
  @Getter
  protected List<WitnessCapsule> witnessStates;
  private Thread generateThread;
  private Manager db;
  private volatile boolean isRunning = false;
  public static final int LOOP_INTERVAL = 1000; // millisecond
  private String privateKey;

  /**
   * Construction method.
   */
  public WitnessService(Application tronApp) {
    this.tronApp = tronApp;
    db = tronApp.getDbManager();
    generateThread = new Thread(scheduleProductionLoop);
    init();
  }

  private Runnable scheduleProductionLoop =
      () -> {
        while (isRunning) {
          DateTime time = DateTime.now();
          int timeToNextSecond = LOOP_INTERVAL - time.getMillisOfSecond();
          if (timeToNextSecond < 50) {
            timeToNextSecond = timeToNextSecond + LOOP_INTERVAL;
          }
          try {
            DateTime nextTime = time.plus(timeToNextSecond);
            logger.info("sleep : " + timeToNextSecond + " ms,next time:" + nextTime);
            Thread.sleep(timeToNextSecond);
            blockProductionLoop();

            updateWitnessSchedule();
          } catch (Exception ex) {
            logger.error("ProductionLoop error", ex);
          }
        }
      };

  private void blockProductionLoop() {
    BlockProductionCondition result = null;
    String capture = "";
    try {
      result = tryProduceBlock(capture);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    if (result == null) {
      logger.warn("result is null");
      return;
    }

    switch (result) {
      case PRODUCED:
        logger.info("");
        break;
      case NOT_SYNCED:
        logger.info("");
        break;
      case NOT_MY_TURN:
        logger.info("");
        break;
      case NOT_TIME_YET:
        logger.info("");
        break;
      case NO_PRIVATE_KEY:
        logger.info("");
        break;
      case LOW_PARTICIPATION:
        logger.info("");
        break;
      case LAG:
        logger.info("");
        break;
      case CONSECUTIVE:
        logger.info("");
        break;
      case EXCEPTION_PRODUCING_BLOCK:
        logger.info("");
        break;
      default:
        break;
    }
  }

  private BlockProductionCondition tryProduceBlock(String capture) {

    long slot = getSlotAtTime(DateTime.now());
    if (slot == 0) {
      // todo capture error message
      return BlockProductionCondition.NOT_TIME_YET;
    }

    ByteString scheduledWitness = db.getScheduledWitness(slot);

    if (!scheduledWitness.equals(getLocalWitnessState().getAddress())) {
      return BlockProductionCondition.NOT_MY_TURN;
    }

    DateTime scheduledTime = getSlotTime(slot);

    BlockCapsule block = generateBlock(scheduledTime);
    broadcastBlock(block);
    return BlockProductionCondition.PRODUCED;
  }

  private void broadcastBlock(BlockCapsule block) {
    try {
      tronApp.getP2pNode().broadcast(new BlockMessage(block.getData()));
    } catch (Exception ex) {
      throw new RuntimeException("broadcastBlock error");
    }
    logger.info("broadcast block successfully");
  }

  private BlockCapsule generateBlock(DateTime when) {
    return tronApp.getDbManager().generateBlock(localWitnessState, when.getMillis(), privateKey);
  }

  private DateTime getSlotTime(long slotNum) {
    if (slotNum == 0) {
      return DateTime.now();
    }
    long interval = blockInterval();
    BlockStore blockStore = tronApp.getDbManager().getBlockStore();
    if (blockStore.getHeadBlockNum() == 0) {
      DateTime genesisTime = blockStore.getGenesisTime();
      return genesisTime.plus(slotNum * interval);
    }

    DateTime headSlotTime = blockStore.getHeadBlockTime();

    return headSlotTime.plus(interval * slotNum);
  }

  private long getSlotAtTime(DateTime when) {
    DateTime firstSlotTime = getSlotTime(1);
    if (when.isBefore(firstSlotTime)) {
      return 0;
    }
    return (when.getMillis() - firstSlotTime.getMillis()) / blockInterval() + 1;
  }


  private long blockInterval() {
    return LOOP_INTERVAL; // millisecond todo getFromDb
  }


  // shuffle witnesses
  private void updateWitnessSchedule() {
    if (db.getBlockStore().getHeadBlockNum() % witnessStates.size() == 0) {
      String witnessStringListBefore = getWitnessStringList(witnessStates).toString();
      witnessStates = new RandomGenerator<WitnessCapsule>()
          .shuffle(witnessStates, db.getBlockStore().getHeadBlockTime());
      logger.info("updateWitnessSchedule,before: " + witnessStringListBefore + ",after: "
          + getWitnessStringList(witnessStates));
    }
  }

  private List<String> getWitnessStringList(List<WitnessCapsule> witnessStates) {
    return witnessStates.stream()
        .map(witnessCapsule -> witnessCapsule.getAddress().toStringUtf8())
        .collect(Collectors.toList());
  }


  private float witnessParticipationRate() {
    return 0f;
  }

  // shuffle todo
  @Override
  public void init() {
    localWitnessState = new WitnessCapsule(ByteString.copyFromUtf8("0x11"));
    this.witnessStates = db.getWitnesses();
  }

  @Override
  public void init(Args args) {
    this.privateKey = args.getPrivateKey();
    localWitnessState = new WitnessCapsule(ByteString.copyFromUtf8("0x11"));
    this.witnessStates = db.getWitnesses();
  }

  @Override
  public void start() {
    isRunning = true;
    generateThread.start();
  }

  @Override
  public void stop() {
    isRunning = false;
    generateThread.interrupt();
  }
}