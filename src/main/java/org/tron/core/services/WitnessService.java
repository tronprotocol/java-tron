package org.tron.core.services;

import static org.tron.common.utils.ByteArray.fromHexString;
import static org.tron.core.witness.BlockProductionCondition.NOT_MY_TURN;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.application.Application;
import org.tron.common.application.Service;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.backup.BackupManager;
import org.tron.common.backup.BackupManager.BackupStatusEnum;
import org.tron.common.backup.BackupServer;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.TronException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.ValidateScheduleException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.net.TronNetService;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.witness.BlockProductionCondition;
import org.tron.core.witness.WitnessController;

@Slf4j(topic = "witness")
public class WitnessService implements Service {

  private static final int MIN_PARTICIPATION_RATE = Args.getInstance()
      .getMinParticipationRate(); // MIN_PARTICIPATION_RATE * 1%
  private static final int PRODUCE_TIME_OUT = 500; // ms
  @Getter
  private static volatile boolean needSyncCheck = Args.getInstance().isNeedSyncCheck();
  @Getter
  protected Map<ByteString, WitnessCapsule> localWitnessStateMap = Maps
      .newHashMap(); //  <witnessAccountAddress,WitnessCapsule>
  private Application tronApp;
  private Thread generateThread;

  @Getter
  private volatile boolean isRunning = false;
  private Map<ByteString, byte[]> privateKeyMap = Maps
      .newHashMap();//<witnessAccountAddress,privateKey>
  private Map<byte[], byte[]> privateKeyToAddressMap = Maps
      .newHashMap();//<privateKey,witnessPermissionAccountAddress>

  private Manager manager;

  private WitnessController controller;

  private TronApplicationContext context;

  private BackupManager backupManager;

  private BackupServer backupServer;

  private TronNetService tronNetService;

  private AtomicInteger dupBlockCount = new AtomicInteger(0);
  private AtomicLong dupBlockTime = new AtomicLong(0);
  private long blockCycle =
      ChainConstant.BLOCK_PRODUCED_INTERVAL * ChainConstant.MAX_ACTIVE_WITNESS_NUM;
  private Cache<ByteString, Long> blocks = CacheBuilder.newBuilder().maximumSize(10).build();

  /**
   * Cycle thread to generate blocks
   */
  private Runnable scheduleProductionLoop =
      () -> {
        if (localWitnessStateMap == null || localWitnessStateMap.keySet().isEmpty()) {
          logger.error("LocalWitnesses is null");
          return;
        }

        while (isRunning) {
          try {
            if (this.needSyncCheck) {
              Thread.sleep(500L);
            } else {
              DateTime time = DateTime.now();
              long timeToNextSecond = ChainConstant.BLOCK_PRODUCED_INTERVAL
                  - (time.getSecondOfMinute() * 1000 + time.getMillisOfSecond())
                  % ChainConstant.BLOCK_PRODUCED_INTERVAL;
              if (timeToNextSecond < 50L) {
                timeToNextSecond = timeToNextSecond + ChainConstant.BLOCK_PRODUCED_INTERVAL;
              }
              DateTime nextTime = time.plus(timeToNextSecond);
              logger.debug(
                  "ProductionLoop sleep : " + timeToNextSecond + " ms,next time:" + nextTime);
              Thread.sleep(timeToNextSecond);
            }
            this.blockProductionLoop();
          } catch (Throwable throwable) {
            logger.error("unknown throwable happened in witness loop", throwable);
          }
        }
      };

  /**
   * Construction method.
   */
  public WitnessService(Application tronApp, TronApplicationContext context) {
    this.tronApp = tronApp;
    this.context = context;
    backupManager = context.getBean(BackupManager.class);
    backupServer = context.getBean(BackupServer.class);
    tronNetService = context.getBean(TronNetService.class);
    generateThread = new Thread(scheduleProductionLoop);
    manager = tronApp.getDbManager();
    manager.setWitnessService(this);
    controller = manager.getWitnessController();
    new Thread(() -> {
      while (needSyncCheck) {
        try {
          Thread.sleep(100);
        } catch (Exception e) {
        }
      }
      backupServer.initServer();
    }).start();
  }

  /**
   * Loop to generate blocks
   */
  private void blockProductionLoop() throws InterruptedException {
    BlockProductionCondition result = this.tryProduceBlock();

    if (result == null) {
      logger.warn("Result is null");
      return;
    }

    if (result.ordinal() <= NOT_MY_TURN.ordinal()) {
      logger.debug(result.toString());
    } else {
      logger.info(result.toString());
    }
  }

  /**
   * Generate and broadcast blocks
   */
  private BlockProductionCondition tryProduceBlock() throws InterruptedException {
    logger.info("Try to Produce Block");
    long now = DateTime.now().getMillis() + 50L;
    if (this.needSyncCheck) {
      long nexSlotTime = controller.getSlotTime(1);
      if (nexSlotTime > now) { // check sync during first loop
        needSyncCheck = false;
        Thread.sleep(nexSlotTime - now); //Processing Time Drift later
        now = DateTime.now().getMillis();
      } else {
        logger.debug("Not sync ,now:{},headBlockTime:{},headBlockNumber:{},headBlockId:{}",
            new DateTime(now),
            new DateTime(this.tronApp.getDbManager().getDynamicPropertiesStore()
                .getLatestBlockHeaderTimestamp()),
            this.tronApp.getDbManager().getDynamicPropertiesStore().getLatestBlockHeaderNumber(),
            this.tronApp.getDbManager().getDynamicPropertiesStore().getLatestBlockHeaderHash());
        return BlockProductionCondition.NOT_SYNCED;
      }
    }

    if (!backupManager.getStatus().equals(BackupStatusEnum.MASTER)) {
      return BlockProductionCondition.BACKUP_STATUS_IS_NOT_MASTER;
    }

    if (dupWitnessCheck()) {
      return BlockProductionCondition.DUP_WITNESS;
    }

    final int participation = this.controller.calculateParticipationRate();
    if (participation < MIN_PARTICIPATION_RATE) {
      logger.warn(
          "Participation[" + participation + "] <  MIN_PARTICIPATION_RATE[" + MIN_PARTICIPATION_RATE
              + "]");

      if (logger.isDebugEnabled()) {
        this.controller.dumpParticipationLog();
      }

      return BlockProductionCondition.LOW_PARTICIPATION;
    }

    if (!controller.activeWitnessesContain(this.getLocalWitnessStateMap().keySet())) {
      logger.info("Unelected. Elected Witnesses: {}",
          StringUtil.getAddressStringList(controller.getActiveWitnesses()));
      return BlockProductionCondition.UNELECTED;
    }

    try {

      BlockCapsule block;

      synchronized (tronApp.getDbManager()) {
        long slot = controller.getSlotAtTime(now);
        logger.debug("Slot:" + slot);
        if (slot == 0) {
          logger.info("Not time yet,now:{},headBlockTime:{},headBlockNumber:{},headBlockId:{}",
              new DateTime(now),
              new DateTime(
                  this.tronApp.getDbManager().getDynamicPropertiesStore()
                      .getLatestBlockHeaderTimestamp()),
              this.tronApp.getDbManager().getDynamicPropertiesStore().getLatestBlockHeaderNumber(),
              this.tronApp.getDbManager().getDynamicPropertiesStore().getLatestBlockHeaderHash());
          return BlockProductionCondition.NOT_TIME_YET;
        }

        if (now < controller.getManager().getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp()) {
          logger.warn("have a timestamp:{} less than or equal to the previous block:{}",
              new DateTime(now), new DateTime(
                  this.tronApp.getDbManager().getDynamicPropertiesStore()
                      .getLatestBlockHeaderTimestamp()));
          return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
        }

        final ByteString scheduledWitness = controller.getScheduledWitness(slot);

        if (!this.getLocalWitnessStateMap().containsKey(scheduledWitness)) {
          logger.info("It's not my turn, ScheduledWitness[{}],slot[{}],abSlot[{}],",
              ByteArray.toHexString(scheduledWitness.toByteArray()), slot,
              controller.getAbSlotAtTime(now));
          return NOT_MY_TURN;
        }

        long scheduledTime = controller.getSlotTime(slot);

        if (scheduledTime - now > PRODUCE_TIME_OUT) {
          return BlockProductionCondition.LAG;
        }

        if (!privateKeyMap.containsKey(scheduledWitness)) {
          return BlockProductionCondition.NO_PRIVATE_KEY;
        }

        controller.getManager().lastHeadBlockIsMaintenance();

        controller.setGeneratingBlock(true);

        block = generateBlock(scheduledTime, scheduledWitness,
            controller.lastHeadBlockIsMaintenance());

        if (block == null) {
          logger.warn("exception thrown when generate block");
          return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
        }

        int blockProducedTimeOut = Args.getInstance().getBlockProducedTimeOut();

        long timeout = Math
            .min(ChainConstant.BLOCK_PRODUCED_INTERVAL * blockProducedTimeOut / 100 + 500,
                ChainConstant.BLOCK_PRODUCED_INTERVAL);
        if (DateTime.now().getMillis() - now > timeout) {
          logger.warn("Task timeout ( > {}ms), startTime:{},endTime:{}", timeout, new DateTime(now),
              DateTime.now());
          tronApp.getDbManager().eraseBlock();
          return BlockProductionCondition.TIME_OUT;
        }
      }

      logger.info(
          "Produce block successfully, blockNumber:{}, abSlot[{}], blockId:{}, transactionSize:{}, blockTime:{}, parentBlockId:{}",
          block.getNum(), controller.getAbSlotAtTime(now), block.getBlockId(),
          block.getTransactions().size(),
          new DateTime(block.getTimeStamp()),
          block.getParentHash());

      broadcastBlock(block);

      return BlockProductionCondition.PRODUCED;
    } catch (TronException e) {
      logger.error(e.getMessage(), e);
      return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
    } finally {
      controller.setGeneratingBlock(false);
    }
  }

  //Verify that the private key corresponds to the witness permission
  public boolean validateWitnessPermission(ByteString scheduledWitness) {
    if (manager.getDynamicPropertiesStore().getAllowMultiSign() == 1) {
      byte[] privateKey = privateKeyMap.get(scheduledWitness);
      byte[] witnessPermissionAddress = privateKeyToAddressMap.get(privateKey);
      AccountCapsule witnessAccount = manager.getAccountStore()
          .get(scheduledWitness.toByteArray());
      if (!Arrays.equals(witnessPermissionAddress, witnessAccount.getWitnessPermissionAddress())) {
        return false;
      }
    }
    return true;
  }

  private void broadcastBlock(BlockCapsule block) {
    try {
      tronNetService.broadcast(new BlockMessage(block.getData()));
    } catch (Exception ex) {
      throw new RuntimeException("Broadcast block error");
    }
  }

  private BlockCapsule generateBlock(long when, ByteString witnessAddress,
      Boolean lastHeadBlockIsMaintenance)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      UnLinkedBlockException, ValidateScheduleException, AccountResourceInsufficientException {
    return tronApp.getDbManager().generateBlock(this.localWitnessStateMap.get(witnessAddress), when,
        this.privateKeyMap.get(witnessAddress), lastHeadBlockIsMaintenance, true);
  }

  private boolean dupWitnessCheck() {
    if (dupBlockCount.get() == 0) {
      return false;
    }

    if (System.currentTimeMillis() - dupBlockTime.get() > dupBlockCount.get() * blockCycle) {
      dupBlockCount.set(0);
      return false;
    }

    return true;
  }

  public void checkDupWitness(BlockCapsule block) {
    if (block.generatedByMyself) {
      blocks.put(block.getBlockId().getByteString(), System.currentTimeMillis());
      return;
    }

    if (blocks.getIfPresent(block.getBlockId().getByteString()) != null) {
      return;
    }

    if (needSyncCheck) {
      return;
    }

    if (System.currentTimeMillis() - block.getTimeStamp() > ChainConstant.BLOCK_PRODUCED_INTERVAL) {
      return;
    }

    if (!privateKeyMap.containsKey(block.getWitnessAddress())) {
      return;
    }

    if (backupManager.getStatus() != BackupStatusEnum.MASTER) {
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

  /**
   * Initialize the local witnesses
   */
  @Override
  public void init() {
    List<String> privateKeys = Args.getInstance().getLocalWitnesses().getPrivateKeys();
    if (privateKeys.size() == 0) {
      return;
    }

    if (privateKeys.size() == 1) {
      byte[] privateKey = ByteArray.fromHexString(privateKeys.get(0));
      byte[] wAddress = Args.getInstance().getLocalWitnesses().getWitnessAccountAddress();
      byte[] pAddress = ECKey.fromPrivate(privateKey).getAddress();
      WitnessCapsule witnessCapsule = this.tronApp.getDbManager().getWitnessStore().get(wAddress);
      if (null == witnessCapsule) {
        logger.warn("WitnessCapsule is null, witness: {}", wAddress);
        witnessCapsule = new WitnessCapsule(ByteString.copyFrom(wAddress));
      }
      this.privateKeyMap.put(witnessCapsule.getAddress(), privateKey);
      this.localWitnessStateMap.put(witnessCapsule.getAddress(), witnessCapsule);
      this.privateKeyToAddressMap.put(privateKey, pAddress);
      return;
    }

    for (String pKey : privateKeys) {
      byte[] privateKey = fromHexString(pKey);
      byte[] address = ECKey.fromPrivate(privateKey).getAddress();
      WitnessCapsule witnessCapsule = this.tronApp.getDbManager().getWitnessStore().get(address);
      if (null == witnessCapsule) {
        logger.info("WitnessCapsule is null, privateKey: {}, witness: {}", pKey,
            Hex.toHexString(address));
        witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
      }
      this.privateKeyMap.put(witnessCapsule.getAddress(), privateKey);
      this.localWitnessStateMap.put(witnessCapsule.getAddress(), witnessCapsule);
      this.privateKeyToAddressMap.put(privateKey, address);
    }
    logger.info("PrivateKeys size = {}", privateKeys.size());
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
