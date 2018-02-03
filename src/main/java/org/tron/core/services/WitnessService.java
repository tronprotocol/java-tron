package org.tron.core.services;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.application.Application;
import org.tron.common.application.Service;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.net.message.BlockMessage;
import org.tron.protos.Protocal;

public class WitnessService implements Service {

  private static final Logger logger = LoggerFactory.getLogger(WitnessService.class);
  Application tronApp;
  @Getter
  protected WitnessCapsule localWitnessState; //  WitnessId;
  @Getter
  protected List<WitnessCapsule> witnessStates;
  private Thread generateThread;
  private Manager db;
  private volatile boolean isRunning = false;
  public static final int LOOP_INTERVAL = 1000; // millisecond

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
    String result = null;
    String capture = null;
    try {
      result = tryProduceBlock(capture);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    // todo
    switch (result) {
      default:
    }
  }

  private String tryProduceBlock(String capture) {

    long slot = getSlotAtTime(DateTime.now());
    if (slot == 0) {
      // todo capture error message
      return "";
    }

    ByteString scheduledWitness = db.getScheduledWitness(slot);

    if (!scheduledWitness.equals(getLocalWitnessState().getAddress())) {
      logger.info("not tune");
      return "";
    }

    DateTime scheduledTime = getSlotTime(slot);

    Protocal.Block block = generateBlock(scheduledTime);
    logger.info("generate block successfully");
    broadcastBlock(block);
    return "";
  }

  private void broadcastBlock(Protocal.Block block) {
    try {
      tronApp.getP2pNode().broadcast(new BlockMessage(block));
    } catch (Exception ex) {
      throw new RuntimeException("broadcastBlock error");
    }
    logger.info("broadcast block successfully");
  }

  private Protocal.Block generateBlock(DateTime when) {
    return tronApp.getDbManager().generateBlock(localWitnessState, when.getMillis());
  }

  private DateTime getSlotTime(long slotNum) {
    if (slotNum == 0) {
      return DateTime.now();
    }
    long interval = blockInterval();
    BlockStore blockStore = tronApp.getDbManager().getBlockStore();
    if (blockStore.getCurrentHeadBlockNum() == 0) {
      DateTime genesis_time = blockStore.getGenesisTime();
      return genesis_time.plus(slotNum * interval);
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

  private static long RANDOM_GENERATOR_NUMBER = 2685821657736338717L;

  // shuffle witnesses
  public void updateWitnessSchedule() {
    if (db.getBlockStore().getCurrentHeadBlockNum() % witnessStates.size() == 0) {
      logger.info("updateWitnessSchedule,before: " + getWitnessStringList(witnessStates));
      witnessStates = updateWitnessSchedule(witnessStates, db.getBlockStore().getHeadBlockTime());
      logger.info("updateWitnessSchedule,after: " + getWitnessStringList(witnessStates));
    }
  }

  private List<String> getWitnessStringList(List<WitnessCapsule> witnessStates) {
    return witnessStates.stream()
        .map(witnessCapsule -> witnessCapsule.getAddress().toStringUtf8())
        .collect(Collectors.toList());
  }

  private static List<WitnessCapsule> updateWitnessSchedule(List<WitnessCapsule> witnessStates,
      DateTime time) {
    long headBlockTimeHi = time.getMillis() << 32;

    for (int i = 0; i < witnessStates.size(); i++) {
      long v = headBlockTimeHi + i * RANDOM_GENERATOR_NUMBER;
      v = v ^ (v >> 12);
      v = v ^ (v << 25);
      v = v ^ (v >> 27);
      v = v * RANDOM_GENERATOR_NUMBER;

      int index = (int) (i + v % (witnessStates.size() - i));
      if (index < 0 || index >= witnessStates.size()) {
        continue;
      }
      WitnessCapsule tmp = witnessStates.get(index);
      witnessStates.set(index, witnessStates.get(i));
      witnessStates.set(i, tmp);
    }
    return witnessStates;
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