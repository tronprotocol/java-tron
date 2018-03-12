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
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.RandomGenerator;
import org.tron.common.utils.Utils;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.CancelException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.witness.BlockProductionCondition;


public class WitnessService implements Service {

  private static final Logger logger = LoggerFactory.getLogger(WitnessService.class);
  private static final int MIN_PARTICIPATION_RATE = 33; // MIN_PARTICIPATION_RATE * 1%
  private static final int PRODUCE_TIME_OUT = 500; // ms
  private Application tronApp;
  @Getter
  protected WitnessCapsule localWitnessState; //  WitnessId;
  @Getter
  protected List<WitnessCapsule> witnessStates;
  private Thread generateThread;
  private Manager db;
  private volatile boolean isRunning = false;
  private byte[] privateKey;
  private boolean needSyncCheck = Args.getInstance().isNeedSyncCheck();
  private volatile boolean canceled = false;

  /**
   * Construction method.
   */
  public WitnessService(Application tronApp) {
    this.tronApp = tronApp;
    db = tronApp.getDbManager();
    generateThread = new Thread(scheduleProductionLoop);
  }

  private Runnable scheduleProductionLoop =
      () -> {
        if (localWitnessState == null) {
          logger.error("LocalWitness is null");
        }

        logger.info("LocalWitness[" + ByteArray
            .toHexString(this.getLocalWitnessState().getAddress().toByteArray()) + "]");

        while (isRunning) {
          DateTime time = DateTime.now();
          long timeToNextSecond = Manager.LOOP_INTERVAL - time.getMillisOfSecond();
          if (timeToNextSecond < 50) {
            timeToNextSecond = timeToNextSecond + Manager.LOOP_INTERVAL;
          }
          try {
            DateTime nextTime = time.plus(timeToNextSecond);
            logger.info("sleep : " + timeToNextSecond + " ms,next time:" + nextTime);
            Thread.sleep(timeToNextSecond);
            this.blockProductionLoop();

            this.updateWitnessSchedule();
          } catch (Exception ex) {
            logger.error("ProductionLoop error", ex);
          }
        }
      };

  private void blockProductionLoop() throws CancelException {
    BlockProductionCondition result;
    try {
      result = this.tryProduceBlock();
    } catch (final CancelException ex) {
      throw ex;
    } catch (final Exception ex) {
      logger.error("produce block error,", ex);
      result = BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
    }

    if (result == null) {
      logger.warn("result is null");
      return;
    }

    switch (result) {
      case PRODUCED:
        logger.info("Porduced");
        break;
      case NOT_SYNCED:
        logger.info("not sync");
        break;
      case NOT_MY_TURN:
        logger.info("It's not my turn");
        break;
      case NOT_TIME_YET:
        logger.info("not time yet");
        break;
      case NO_PRIVATE_KEY:
        logger.info("no pri key");
        break;
      case LOW_PARTICIPATION:
        logger.info("low part");
        break;
      case LAG:
        logger.info("lag");
        break;
      case CONSECUTIVE:
        logger.info("consecutive");
        break;
      case EXCEPTION_PRODUCING_BLOCK:
        logger.info("excpetion");
        break;
      default:
        break;
    }
  }


  private BlockProductionCondition tryProduceBlock()
      throws CancelException {

    this.checkCancelFlag();

    if (this.needSyncCheck) {
      if (db.getSlotTime(1).isAfterNow()) { // check sync during first loop
        needSyncCheck = false;
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

    long slot = db.getSlotAtTime(DateTime.now());
    logger.debug("slot:" + slot);

    if (slot == 0) {
      // todo capture error message
      return BlockProductionCondition.NOT_TIME_YET;
    }

    final ByteString scheduledWitness = this.db.getScheduledWitness(slot);

    if (!scheduledWitness.equals(this.getLocalWitnessState().getAddress())) {
      logger
          .info("scheduledWitness[" + ByteArray.toHexString(scheduledWitness.toByteArray()) + "]");
      return BlockProductionCondition.NOT_MY_TURN;
    }

    DateTime scheduledTime = db.getSlotTime(slot);

    //TODO:implement private and public key code, fake code first.

    if (scheduledTime.getMillis() - DateTime.now().getMillis() > PRODUCE_TIME_OUT) {
      return BlockProductionCondition.LAG;
    }

    //TODO:implement private and public key code, fake code first.
    try {
      BlockCapsule block = generateBlock(scheduledTime);
      logger.info("Block is generated successfully, Its Id is " + block.getBlockId());
      broadcastBlock(block);
      return BlockProductionCondition.PRODUCED;
    } catch (ValidateSignatureException e) {
      logger.error(e.getMessage());
      return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
    } catch (ContractValidateException e) {
      logger.error(e.getMessage());
      return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
    } catch (ContractExeException e) {
      logger.error(e.getMessage());
      return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
    }catch (Exception e) {
      logger.error(e.getMessage());
      return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
    }


  }

  private void checkCancelFlag() throws CancelException {
    if (canceled) {
      throw new CancelException();
    }
  }

  private void broadcastBlock(BlockCapsule block) {
    try {
      tronApp.getP2pNode().broadcast(new BlockMessage(block.getData()));
    } catch (Exception ex) {
      throw new RuntimeException("broadcastBlock error");
    }
  }

  private BlockCapsule generateBlock(DateTime when)
      throws ValidateSignatureException, ContractValidateException, ContractExeException {
    return db.generateBlock(localWitnessState, when.getMillis(), privateKey);
  }


  // shuffle witnesses
  private void updateWitnessSchedule() {

    long headBlockNum = db.getBlockStore().getHeadBlockNum();
    if (headBlockNum != 0 && headBlockNum % witnessStates.size() == 0) {
      String witnessStringListBefore = getWitnessStringList(witnessStates).toString();
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

  public static void main(String[] args) {
    Utils.getRandom();

    byte[] privateKey = Args.getInstance().getLocalWitness().getPrivateKey()
        .getBytes();

    final ECKey ecKey = ECKey.fromPrivate(privateKey);


  }


  // shuffle todo
  @Override
  public void init() {
    this.privateKey = ByteArray.fromHexString(Args.getInstance().getLocalWitness().getPrivateKey());
    final ECKey ecKey = ECKey.fromPrivate(this.privateKey);

    WitnessCapsule witnessCapsule = this.db.getWitnessStore()
        .get(ecKey.getAddress());
    // need handle init witness
    if (null == witnessCapsule) {
      logger.warn("witnessCapsule[" + ecKey.getAddress() + "] is not in witnessStore");
      witnessCapsule = new WitnessCapsule(ByteString.copyFrom(ecKey.getAddress()));
    }
    //
    this.db.updateWits();
    this.localWitnessState = witnessCapsule;
    this.witnessStates = this.db.getWitnesses();
  }


  @Override
  public void init(Args args) {
    //this.privateKey = args.getPrivateKey();
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
