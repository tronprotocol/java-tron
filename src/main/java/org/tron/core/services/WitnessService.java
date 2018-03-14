package org.tron.core.services;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.application.Application;
import org.tron.common.application.Service;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.RandomGenerator;
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


public class WitnessService implements Service {

  private static final Logger logger = LoggerFactory.getLogger(WitnessService.class);
  private static final int MIN_PARTICIPATION_RATE = 33; // MIN_PARTICIPATION_RATE * 1%
  private static final int PRODUCE_TIME_OUT = 500; // ms
  private Application tronApp;
  @Getter
  protected Map<ByteString, WitnessCapsule> localWitnessStateMap = Maps
      .newHashMap(); //  <address,WitnessCapsule>
  @Getter
  protected List<WitnessCapsule> witnessStates;
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
          DateTime time = DateTime.now();
          long timeToNextSecond = Manager.LOOP_INTERVAL - time.getMillisOfSecond();
          if (timeToNextSecond < 50) {
            timeToNextSecond = timeToNextSecond + Manager.LOOP_INTERVAL;
          }
          try {
            DateTime nextTime = time.plus(timeToNextSecond);
            if (this.needSyncCheck) {
              Thread.sleep(500L);
            } else {
              logger.info("Sleep : " + timeToNextSecond + " ms,next time:" + nextTime);
              Thread.sleep(timeToNextSecond);
            }
            this.blockProductionLoop();
            this.updateWitnessSchedule();
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
        logger.info("Produced");
        break;
      case NOT_SYNCED:
//        logger.info("Not sync");
        break;
      case NOT_MY_TURN:
        logger.info("It's not my turn");
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

    DateTime now = DateTime.now();
    if (this.needSyncCheck) {
      logger.info(new DateTime(db.getSlotTime(1)).toString());
      logger.info(now.toString());

      long nexSlotTime = db.getSlotTime(1);
      if (nexSlotTime > now.getMillis()) { // check sync during first loop
        needSyncCheck = false;
        Thread.sleep(nexSlotTime - now.getMillis()); //Processing Time Drift later
        now = DateTime.now();
      } else {
        return BlockProductionCondition.NOT_SYNCED;
      }
    }

    final int participation = this.db.calculateParticipationRate();
    if (participation < MIN_PARTICIPATION_RATE) {
      logger.warn(
          "Participation[" + participation + "] <  MIN_PARTICIPATION_RATE[" + MIN_PARTICIPATION_RATE
              + "]");
      return BlockProductionCondition.LOW_PARTICIPATION;
    }

    long slot = db.getSlotAtTime(now.getMillis());
    logger.debug("Slot:" + slot);

    if (slot == 0) {
      return BlockProductionCondition.NOT_TIME_YET;
    }

    final ByteString scheduledWitness = this.db.getScheduledWitness(slot);

    if (!this.getLocalWitnessStateMap().containsKey(scheduledWitness)) {
      logger
          .info("ScheduledWitness[" + ByteArray.toHexString(scheduledWitness.toByteArray()) + "]");
      return BlockProductionCondition.NOT_MY_TURN;
    }

    long scheduledTime = db.getSlotTime(slot);

    if (scheduledTime - now.getMillis() > PRODUCE_TIME_OUT) {
      return BlockProductionCondition.LAG;
    }

    if (!privateKeyMap.containsKey(scheduledWitness)) {
      return BlockProductionCondition.NO_PRIVATE_KEY;
    }

    try {
      BlockCapsule block = generateBlock(scheduledTime, scheduledWitness);
      logger.info("Block is generated successfully, Its Id is " + block.getBlockId());
      broadcastBlock(block);
      return BlockProductionCondition.PRODUCED;
    } catch (TronException e) {
      e.printStackTrace();
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
   * shuffle witnesses after each cycle is completed
   */
  private void updateWitnessSchedule() {

    long headBlockNum = db.getBlockStore().getHeadBlockNum();
    if (headBlockNum != 0 && headBlockNum % witnessStates.size() == 0) {
//      String witnessStringListBefore = getWitnessStringList(witnessStates).toString();
      witnessStates = new RandomGenerator<WitnessCapsule>()
          .shuffle(witnessStates, db.getBlockStore().getHeadBlockTime());
//      logger.info("updateWitnessSchedule,before: " + witnessStringListBefore + ",after: "
//          + getWitnessStringList(witnessStates));
    }
  }

  private List<String> getWitnessStringList(List<WitnessCapsule> witnessStates) {
    return witnessStates.stream()
        .map(witnessCapsule -> ByteArray.toHexString(witnessCapsule.getAddress().toByteArray()))
        .collect(Collectors.toList());
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

    this.db.updateWits();
    this.witnessStates = this.db.getWitnesses();
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
