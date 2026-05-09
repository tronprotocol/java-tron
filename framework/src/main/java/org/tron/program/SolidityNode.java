package org.tron.program;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Component;
import org.tron.common.client.DatabaseGrpcClient;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.TronError;
import org.tron.core.net.TronNetDelegate;
import org.tron.protos.Protocol.Block;

@Slf4j(topic = "app")
@Conditional(SolidityNode.SolidityCondition.class)
@Component
public class SolidityNode implements ApplicationListener<ContextClosedEvent> {

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private TronNetDelegate tronNetDelegate;

  private DatabaseGrpcClient databaseGrpcClient;

  private final AtomicLong ID = new AtomicLong();

  private final AtomicLong remoteBlockNum = new AtomicLong();

  private final LinkedBlockingDeque<Block> blockQueue = new LinkedBlockingDeque<>(100);

  private final int exceptionSleepTime = 1000;

  private volatile boolean flag = true;

  private final String getBlockName = "get-block";
  private final String processBlockName = "process-block";

  private ExecutorService getBlockExecutor;
  private ExecutorService processBlockExecutor;

  @PostConstruct
  private void init() {
    resolveCompatibilityIssueIfUsingFullNodeDatabase();
    ID.set(chainBaseManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum());
    getBlockExecutor = ExecutorServiceManager.newSingleThreadExecutor(getBlockName);
    processBlockExecutor = ExecutorServiceManager.newSingleThreadExecutor(processBlockName);
  }

  @Override
  public void onApplicationEvent(ContextClosedEvent event) {
    flag = false; // invoke earlier than @PreDestroy
  }

  public void close() {
    flag = false;
    if (databaseGrpcClient != null) {
      databaseGrpcClient.shutdown();
    }
    // Interrupt get-block immediately: it may be stuck in blockQueue.put() (full queue,
    // process-block stopped) or in a gRPC blocking stub call.
    // Do NOT interrupt process-block: let it finish its current pushVerifiedBlock naturally
    // (flag=false causes the while-loop to exit within 1-2 s) so the DB flush can complete
    // cleanly before ApplicationImpl.shutdown() tears down the underlying executor.
    getBlockExecutor.shutdownNow();
    ExecutorServiceManager.shutdownAndAwaitTermination(getBlockExecutor, getBlockName);
    ExecutorServiceManager.shutdownAndAwaitTermination(processBlockExecutor, processBlockName);
  }

  @PreDestroy
  private void shutdown() {
    close();
  }

  public void run() {
    try {
      databaseGrpcClient = new DatabaseGrpcClient(CommonParameter.getInstance().getTrustNodeAddr());
      remoteBlockNum.set(getLastSolidityBlockNum());

      getBlockExecutor.submit(this::getBlock);
      processBlockExecutor.submit(this::processSolidityBlock);
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
    while (flag && !tronNetDelegate.isHitDown()) {
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

  private void processSolidityBlock() {
    while (flag && !tronNetDelegate.isHitDown()) {
      try {
        Block block = blockQueue.poll(exceptionSleepTime, TimeUnit.MILLISECONDS);
        if (block == null) {
          continue;
        }
        loopProcessBlock(block);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.info("processSolidityBlock interrupted.");
        return;
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
        tronNetDelegate.pushVerifiedBlock(new BlockCapsule(block));
        if (!tronNetDelegate.isHitDown()) {
          chainBaseManager.getDynamicPropertiesStore().saveLatestSolidifiedBlockNum(blockNum);
          logger.info("Success to process block: {}, blockQueueSize: {}.",
              blockNum, blockQueue.size());
        }
        return;
      } catch (Exception e) {
        logger.error("Failed to process block {}.", new BlockCapsule(block), e);
        sleep(exceptionSleepTime);
        block = getBlockByNum(blockNum);
      }
    }
  }

  private Block getBlockByNum(long blockNum) {
    while (flag && !tronNetDelegate.isHitDown()) {
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
    //throw RuntimeException instead of return null to avoid NullPointException
    throw new RuntimeException("SolidityNode is closing.");
  }

  private long getLastSolidityBlockNum() {
    while (flag && !tronNetDelegate.isHitDown()) {
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
    return 0;
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

  static class SolidityCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return Args.getInstance().isSolidityNode();
    }
  }
}
