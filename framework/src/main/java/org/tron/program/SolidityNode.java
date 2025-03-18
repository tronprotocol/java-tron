package org.tron.program;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.util.ObjectUtils;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.client.DatabaseGrpcClient;
import org.tron.common.exit.ExitManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.Metrics;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.TronError;
import org.tron.protos.Protocol.Block;

@Slf4j(topic = "app")
public class SolidityNode {

  private Manager dbManager;

  private ChainBaseManager chainBaseManager;

  private DatabaseGrpcClient databaseGrpcClient;

  private AtomicLong ID = new AtomicLong();

  private AtomicLong remoteBlockNum = new AtomicLong();

  private LinkedBlockingDeque<Block> blockQueue = new LinkedBlockingDeque<>(100);

  private int exceptionSleepTime = 1000;

  private volatile boolean flag = true;

  public SolidityNode(Manager dbManager) {
    this.dbManager = dbManager;
    this.chainBaseManager = dbManager.getChainBaseManager();
    resolveCompatibilityIssueIfUsingFullNodeDatabase();
    ID.set(chainBaseManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum());
    databaseGrpcClient = new DatabaseGrpcClient(CommonParameter.getInstance().getTrustNodeAddr());
    remoteBlockNum.set(getLastSolidityBlockNum());
  }

  /**
   * Start the SolidityNode.
   */
  public static void main(String[] args) {
    ExitManager.initExceptionHandler();
    logger.info("Solidity node is running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    CommonParameter parameter = CommonParameter.getInstance();

    logger.info("index switch is {}",
        BooleanUtils.toStringOnOff(BooleanUtils
            .toBoolean(parameter.getStorage().getIndexSwitch())));

    if (ObjectUtils.isEmpty(parameter.getTrustNodeAddr())) {
      logger.error("Trust node is not set.");
      return;
    }
    parameter.setSolidityNode(true);

    if (parameter.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }
    // init metrics first
    Metrics.init();

    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.setAllowCircularReferences(false);
    TronApplicationContext context =
        new TronApplicationContext(beanFactory);
    context.register(DefaultConfig.class);
    context.refresh();
    Application appT = ApplicationFactory.create(context);
    context.registerShutdownHook();
    appT.startup();
    SolidityNode node = new SolidityNode(appT.getDbManager());
    node.start();
    appT.blockUntilShutdown();
  }

  private void start() {
    try {
      new Thread(this::getBlock).start();
      new Thread(this::processBlock).start();
      logger.info("Success to start solid node, ID: {}, remoteBlockNum: {}.", ID.get(),
          remoteBlockNum);
    } catch (Exception e) {
      logger.error("Failed to start solid node, address: {}.",
          CommonParameter.getInstance().getTrustNodeAddr());
      throw new TronError(e, TronError.ErrCode.SOLID_NODE_INIT);
    }
  }

  private void getBlock() {
    long blockNum = ID.incrementAndGet();
    while (flag) {
      try {
        if (blockNum > remoteBlockNum.get()) {
          sleep(BLOCK_PRODUCED_INTERVAL);
          remoteBlockNum.set(getLastSolidityBlockNum());
          continue;
        }
        Block block = getBlockByNum(blockNum);
        blockQueue.put(block);
        blockNum = ID.incrementAndGet();
      } catch (Exception e) {
        logger.error("Failed to get block {}, reason: {}.", blockNum, e.getMessage());
        sleep(exceptionSleepTime);
      }
    }
  }

  private void processBlock() {
    while (flag) {
      try {
        Block block = blockQueue.take();
        loopProcessBlock(block);
      } catch (Exception e) {
        logger.error(e.getMessage());
        sleep(exceptionSleepTime);
      }
    }
  }

  private void loopProcessBlock(Block block) {
    while (flag) {
      long blockNum = block.getBlockHeader().getRawData().getNumber();
      try {
        dbManager.pushVerifiedBlock(new BlockCapsule(block));
        chainBaseManager.getDynamicPropertiesStore().saveLatestSolidifiedBlockNum(blockNum);
        logger
            .info("Success to process block: {}, blockQueueSize: {}.", blockNum, blockQueue.size());
        return;
      } catch (Exception e) {
        logger.error("Failed to process block {}.", new BlockCapsule(block), e);
        sleep(exceptionSleepTime);
        block = getBlockByNum(blockNum);
      }
    }
  }

  private Block getBlockByNum(long blockNum) {
    while (true) {
      try {
        long time = System.currentTimeMillis();
        Block block = databaseGrpcClient.getBlock(blockNum);
        long num = block.getBlockHeader().getRawData().getNumber();
        if (num == blockNum) {
          logger.info("Success to get block: {}, cost: {}ms.",
              blockNum, System.currentTimeMillis() - time);
          return block;
        } else {
          logger.warn("Get block id not the same , {}, {}.", num, blockNum);
          sleep(exceptionSleepTime);
        }
      } catch (Exception e) {
        logger.error("Failed to get block: {}, reason: {}.", blockNum, e.getMessage());
        sleep(exceptionSleepTime);
      }
    }
  }

  private long getLastSolidityBlockNum() {
    while (true) {
      try {
        long time = System.currentTimeMillis();
        long blockNum = databaseGrpcClient.getDynamicProperties().getLastSolidityBlockNum();
        logger.info("Get last remote solid blockNum: {}, remoteBlockNum: {}, cost: {}.",
            blockNum, remoteBlockNum, System.currentTimeMillis() - time);
        return blockNum;
      } catch (Exception e) {
        logger.error("Failed to get last solid blockNum: {}, reason: {}.", remoteBlockNum.get(),
            e.getMessage());
        sleep(exceptionSleepTime);
      }
    }
  }

  public void sleep(long time) {
    try {
      Thread.sleep(time);
    } catch (Exception e1) {
      logger.error(e1.getMessage());
    }
  }

  private void resolveCompatibilityIssueIfUsingFullNodeDatabase() {
    long lastSolidityBlockNum =
        chainBaseManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum();
    long headBlockNum = chainBaseManager.getHeadBlockNum();
    logger.info("headBlockNum:{}, solidityBlockNum:{}, diff:{}",
        headBlockNum, lastSolidityBlockNum, headBlockNum - lastSolidityBlockNum);
    if (lastSolidityBlockNum < headBlockNum) {
      logger.info("use fullNode database, headBlockNum:{}, solidityBlockNum:{}, diff:{}",
          headBlockNum, lastSolidityBlockNum, headBlockNum - lastSolidityBlockNum);
      chainBaseManager.getDynamicPropertiesStore().saveLatestSolidifiedBlockNum(headBlockNum);
    }
  }
}