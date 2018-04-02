package org.tron.core.services;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.tron.common.application.Application;
import org.tron.common.application.Service;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.TronException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.witness.BlockProductionCondition;

@Slf4j
public class WitnessService implements Service {

  private static final int MIN_PARTICIPATION_RATE = 0; // MIN_PARTICIPATION_RATE * 1%
  private static final int PRODUCE_TIME_OUT = 500; // ms
  private Application tronApp;
  @Getter
  protected Map<ByteString, WitnessCapsule> localWitnessStateMap = Maps
      .newHashMap(); //  <address,WitnessCapsule>
  private Thread generateThread;
  private Manager db;
  private volatile boolean isRunning = false;
  private Map<ByteString, byte[]> privateKeyMap = Maps.newHashMap();
  private boolean needSyncCheck = Args.getInstance().isNeedSyncCheck();

  /**
   * Construction method.
   */
  public WitnessService(Application tronApp) {
    this.tronApp = tronApp;
    db = tronApp.getDbManager();
    generateThread = new Thread(scheduleProductionLoop);
  }

  /**
   * Cycle thread to generate blocks
   */
  private Runnable scheduleProductionLoop =
      () -> {
        if (localWitnessStateMap == null || localWitnessStateMap.keySet().size() == 0) {
          logger.error("LocalWitnesses is null");
          return;
        }

        while (isRunning) {
          try {
            if (this.needSyncCheck) {
              Thread.sleep(500L);
            } else {
              DateTime time = DateTime.now();
              long timeToNextSecond = Manager.LOOP_INTERVAL
                  - (time.getSecondOfMinute() * 1000 + time.getMillisOfSecond())
                  % Manager.LOOP_INTERVAL;
              if (timeToNextSecond < 50L) {
                timeToNextSecond = timeToNextSecond + Manager.LOOP_INTERVAL;
              }
              DateTime nextTime = time.plus(timeToNextSecond);
              logger.debug(
                  "ProductionLoop sleep : " + timeToNextSecond + " ms,next time:" + nextTime);
              Thread.sleep(timeToNextSecond);
            }
            this.blockProductionLoop();
          } catch (InterruptedException ex) {
            logger.info("ProductionLoop interrupted");
          } catch (Exception ex) {
            logger.error("Unknown exception happened", ex);
            throw ex;
          }
        }
      };

  /**
   * Loop to generate blocks
   */
  private void blockProductionLoop() throws InterruptedException {
    BlockProductionCondition result = this.tryProduceBlock();

    if (result == null) {
      logger.warn("Result is null");
      return;
    }

    switch (result) {
      case PRODUCED:
        logger.debug("Produced");
        break;
      case NOT_SYNCED:
//        logger.info("Not sync");
        break;
      case NOT_MY_TURN:
        logger.debug("It's not my turn");
        break;
      case NOT_TIME_YET:
        logger.info("Not time yet");
        break;
      case NO_PRIVATE_KEY:
        logger.info("No pri key");
        break;
      case LOW_PARTICIPATION:
        logger.info("Low part");
        break;
      case LAG:
        logger.info("Lag");
        break;
      case CONSECUTIVE:
        logger.info("Consecutive");
        break;
      case EXCEPTION_PRODUCING_BLOCK:
        logger.info("Exception");
        break;
      default:
        break;
    }
  }

  /**
   * Generate and broadcast blocks
   */
  private BlockProductionCondition tryProduceBlock() throws InterruptedException {

    long now = DateTime.now().getMillis() + 50L;
    if (this.needSyncCheck) {
      long nexSlotTime = db.getSlotTime(1);
      if (nexSlotTime > now) { // check sync during first loop
        needSyncCheck = false;
        Thread.sleep(nexSlotTime - now); //Processing Time Drift later
        now = DateTime.now().getMillis();
      } else {
        logger.debug("Not sync ,now:{},headBlockTime:{},headBlockNumber:{},headBlockId:{}",
            new DateTime(now), new DateTime(db.getHead().getTimeStamp()), db.getHead().getNum(),
            db.getHead().getBlockId());
        return BlockProductionCondition.NOT_SYNCED;
      }
    }
    if (db.isSyncMode()) {
      return BlockProductionCondition.NOT_SYNCED;
    }
    final int participation = this.db.calculateParticipationRate();
    if (participation < MIN_PARTICIPATION_RATE) {
      logger.warn(
          "Participation[" + participation + "] <  MIN_PARTICIPATION_RATE[" + MIN_PARTICIPATION_RATE
              + "]");
      return BlockProductionCondition.LOW_PARTICIPATION;
    }

    long slot = db.getSlotAtTime(now);
    logger.debug("Slot:" + slot);

    if (slot == 0) {
      logger.info("Not time yet,now:{},headBlockTime:{},headBlockNumber:{},headBlockId:{}",
          new DateTime(now), new DateTime(db.getHead().getTimeStamp()), db.getHead().getNum(),
          db.getHead().getBlockId());
      return BlockProductionCondition.NOT_TIME_YET;
    }

    final ByteString scheduledWitness = db.getScheduledWitness(slot);

    if (!this.getLocalWitnessStateMap().containsKey(scheduledWitness)) {
      logger.info("It's not my turn,ScheduledWitness[{}],slot[{}],abSlot[{}],",
          ByteArray.toHexString(scheduledWitness.toByteArray()), slot, db.getAbSlotAtTime(now));
      logger.debug("headBlockNumber:{},headBlockId:{},headBlockTime:{}",
          db.getHead().getNum(), db.getHead().getBlockId(),
          new DateTime(db.getHead().getTimeStamp()));
      return BlockProductionCondition.NOT_MY_TURN;
    }

    long scheduledTime = db.getSlotTime(slot);

    if (scheduledTime - now > PRODUCE_TIME_OUT) {
      return BlockProductionCondition.LAG;
    }

    if (!privateKeyMap.containsKey(scheduledWitness)) {
      return BlockProductionCondition.NO_PRIVATE_KEY;
    }

    try {
      BlockCapsule block = generateBlock(scheduledTime, scheduledWitness);
      logger.info(
          "Produce block successfully, blockNumber:{},abSlot[{}],blockId:{}, blockTime:{}, parentBlockId:{}",
          block.getNum(), db.getAbSlotAtTime(now), block.getBlockId(),
          new DateTime(block.getTimeStamp()),
          db.getHead().getBlockId());
      broadcastBlock(block);
      return BlockProductionCondition.PRODUCED;
    } catch (TronException e) {
      logger.debug(e.getMessage(), e);
      return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
    }

  }

  private void broadcastBlock(BlockCapsule block) {
    try {
      tronApp.getP2pNode().broadcast(new BlockMessage(block.getData()));
    } catch (Exception ex) {
      throw new RuntimeException("BroadcastBlock error");
    }
  }

  private BlockCapsule generateBlock(long when, ByteString witnessAddress)
      throws ValidateSignatureException, ContractValidateException, ContractExeException, UnLinkedBlockException {
    return db.generateBlock(this.localWitnessStateMap.get(witnessAddress), when,
        this.privateKeyMap.get(witnessAddress));
  }


  /**
   * Initialize the local witnesses
   */
  @Override
  public void init() {
    Args.getInstance().getLocalWitnesses().getPrivateKeys().forEach(key -> {
      byte[] privateKey = ByteArray.fromHexString(key);
      final ECKey ecKey = ECKey.fromPrivate(privateKey);
      byte[] address = ecKey.getAddress();
      WitnessCapsule witnessCapsule = this.db.getWitnessStore()
          .get(address);
      // need handle init witness
      if (null == witnessCapsule) {
        logger.warn("WitnessCapsule[" + address + "] is not in witnessStore");
        witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
      }

      this.privateKeyMap.put(witnessCapsule.getAddress(), privateKey);
      this.localWitnessStateMap.put(witnessCapsule.getAddress(), witnessCapsule);
    });

  }


  @Override
  public void init(Args args) {
    //this.privateKey = args.getPrivateKeys();
    init();
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
