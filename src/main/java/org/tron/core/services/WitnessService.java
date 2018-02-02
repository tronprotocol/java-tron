package org.tron.core.services;

import com.google.protobuf.ByteString;
import java.util.List;
import lombok.Getter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.application.Application;
import org.tron.common.application.Service;
import org.tron.core.capsule.WitnessCapsule;
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
  private static final int LOOP_INTERVAL = 1000;


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
    logger.info("scheduled_witness:" + scheduledWitness + ",slot:" + slot);

    //    if( !witnesses.contains( scheduled_witness ) == _witnesses.end() ){
    if (!scheduledWitness.equals(getLocalWitnessState().getAddress())) {

      logger.info("not tune");
      return "";
    }
    // todo add other verification
    DateTime scheduledTime = getSlotTime(slot);
    // todo verify
    Protocal.Block block = generateBlock();
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

  private Protocal.Block generateBlock() {
    return tronApp.getDbManager().generateBlock();
  }

  protected long getCurrentSlot() {
    return 0; // todo
  }

  private DateTime getSlotTime(long slotNum) {
    if (slotNum == 0) {
      return DateTime.now();
    }
    long interval = blockInterval();

    // todo add other verification and need revised later
    //            long head_block_abs_slot = head_block_time().sec_since_epoch() / interval;
    //            DateTime head_slot_time = head_block_abs_slot * interval;

    DateTime headSlotTime = headBlockTime();
    headSlotTime.plus(interval * slotNum);
    return headSlotTime;
  }

  private DateTime headBlockTime() {
    return DateTime.now(); // todo
  }

  private long getSlotAtTime(DateTime when) {
    DateTime firstSlotTime = getFirstSlotTime();
    //    if (when.isAfter(first_slot_time)) return 0;
    return (when.getMillis() - firstSlotTime.getMillis()) / blockInterval() + 1;
  }

  protected DateTime getFirstSlotTime() {
    return DateTime.now(); // todo
  }

  private long blockInterval() {
    return LOOP_INTERVAL; // millisecond todo getFromDb
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