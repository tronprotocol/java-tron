package org.tron.program;

import ch.qos.logback.classic.Level;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.overlay.client.DatabaseGrpcClient;
import org.tron.common.overlay.discover.DiscoverServer;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.overlay.server.ChannelManager;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.http.solidity.SolidityNodeHttpApiService;
import org.tron.protos.Protocol.Block;

@Slf4j
public class SolidityNode {

  private Manager dbManager;

  private Args cfgArgs;

  private DatabaseGrpcClient databaseGrpcClient;

  private AtomicLong ID = new AtomicLong();

  private Map<Long, Block> blockMap = Maps.newConcurrentMap();

  private LinkedBlockingDeque<Block> blockQueue = new LinkedBlockingDeque(10000);

  private LinkedBlockingDeque<Block> blockBakQueue = new LinkedBlockingDeque(10000);

  private volatile long remoteLastSolidityBlockNum = 0;

  private volatile long lastSolidityBlockNum;

  private long startTime = System.currentTimeMillis();

  private volatile boolean syncFlag = true;

  private volatile boolean flag = true;

  public SolidityNode(Manager dbManager, Args cfgArgs) {
    this.dbManager = dbManager;
    this.cfgArgs = cfgArgs;
    resolveCompatibilityIssueIfUsingFullNodeDatabase();
    lastSolidityBlockNum = dbManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum();
    ID.set(lastSolidityBlockNum);
    databaseGrpcClient = new DatabaseGrpcClient(cfgArgs.getTrustNodeAddr());
    remoteLastSolidityBlockNum = getLastSolidityBlockNum();
  }

  private void start() {
    try {
      for (int i = 0; i < 50; i++) {
        new Thread(() -> getSyncBlock()).start();
      }
      new Thread(() -> getAdvBlock()).start();
      new Thread(() -> pushBlock()).start();
      new Thread(() -> processBlock()).start();
      new Thread(() -> processTrx()).start();
      logger.info(
          "Success to start solid node, lastSolidityBlockNum: {}, ID: {}, remoteLastSolidityBlockNum: {}.",
          lastSolidityBlockNum, ID.get(), remoteLastSolidityBlockNum);
    } catch (Exception e) {
      logger.error("Failed to start solid node, address: {}.", cfgArgs.getTrustNodeAddr());
      System.exit(0);
    }
  }

  private void getSyncBlock() {
    long blockNum = getNextSyncBlockId();
    while (syncFlag) {
      try {
        if (blockNum == 0) {
          break;
        }
        if (blockMap.size() > 10000) {
          sleep(1000);
          continue;
        }
        Block block = getBlockByNum(blockNum);
        blockMap.put(blockNum, block);
        logger.info("Success to get sync block: {}.", blockNum);
        blockNum = getNextSyncBlockId();
      } catch (Exception e) {
        logger.error("Failed to get sync block {}.", blockNum);
        sleep(1000);
      }
    }
    logger.warn("Get sync block thread {} exit.", Thread.currentThread().getName());
  }

  synchronized long getNextSyncBlockId() {

    if (!syncFlag) {
      return 0;
    }

    if (ID.get() < remoteLastSolidityBlockNum) {
      return ID.incrementAndGet();
    }

    long lastNum = getLastSolidityBlockNum();
    if (lastNum - remoteLastSolidityBlockNum > 50) {
      remoteLastSolidityBlockNum = lastNum;
      return ID.incrementAndGet();
    }

    logger.warn("Sync mode switch to adv, ID = {}, lastNum = {}, remoteLastSolidityBlockNum = {}",
        ID.get(), lastNum, remoteLastSolidityBlockNum);

    syncFlag = false;

    return 0;
  }

  private void getAdvBlock() {
    while (syncFlag) {
      sleep(5000);
    }
    logger.warn("Get adv block thread start.");
    long blockNum = ID.incrementAndGet();
    while (flag) {
      try {
        if (blockNum > remoteLastSolidityBlockNum) {
          sleep(3000);
          remoteLastSolidityBlockNum = getLastSolidityBlockNum();
          continue;
        }
        Block block = getBlockByNum(blockNum);
        blockMap.put(blockNum, block);
        logger.info("Success to get adv block: {}.", blockNum);
        blockNum = ID.incrementAndGet();
      } catch (Exception e) {
        logger.error("Failed to get adv block {}.", blockNum);
        sleep(1000);
      }
    }
  }

  private Block getBlockByNum(long blockNum) throws Exception {
    Block block = databaseGrpcClient.getBlock(blockNum);
    if (block.getBlockHeader().getRawData().getNumber() != blockNum) {
      logger.warn("Get adv block id not the same , {}, {}.", blockNum,
          block.getBlockHeader().getRawData().getNumber());
      throw new Exception();
    }
    return block;
  }

  private long getLastSolidityBlockNum() {
    while (true) {
      try {
        long blockNum = databaseGrpcClient.getDynamicProperties().getLastSolidityBlockNum();
        logger.info("Get last remote solid blockNum: {}.", remoteLastSolidityBlockNum);
        return blockNum;
      } catch (Exception e) {
        logger.error("Failed to get last solid blockNum: {}.", remoteLastSolidityBlockNum);
        sleep(1000);
      }
    }
  }

  private void pushBlock() {
    while (flag) {
      try {
        Block block = blockMap.remove(lastSolidityBlockNum + 1);
        if (block == null) {
          sleep(1000);
          continue;
        }
        blockQueue.put(block);
        ++lastSolidityBlockNum;
      } catch (Exception e) {
      }
    }
  }

  private void processBlock() {
    while (flag) {
      try {
        Block block = blockQueue.take();
        loopProcessBlock(block);
        blockBakQueue.put(block);
        logger.info(
            "Success to process block: {}, blockMapSize: {}, blockQueueSize: {}, blockBakQueue: {}, cost {}.",
            block.getBlockHeader().getRawData().getNumber(),
            blockMap.size(),
            blockQueue.size(),
            blockBakQueue.size(),
            (System.currentTimeMillis() - startTime));
      } catch (Exception e) {
        logger.error(e.getMessage());
        sleep(100);
      }
    }
  }

  private void resolveCompatibilityIssueIfUsingFullNodeDatabase() {
    long lastSolidityBlockNum = dbManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum();
    long headBlockNum = dbManager.getHeadBlockNum();
    logger.info("headBlockNum:{}, solidityBlockNum:{}, diff:{}",
        headBlockNum, lastSolidityBlockNum, headBlockNum - lastSolidityBlockNum);
    if (lastSolidityBlockNum < headBlockNum) {
      logger.info("use fullnode database, headBlockNum:{}, solidityBlockNum:{}, diff:{}",
          headBlockNum, lastSolidityBlockNum, headBlockNum - lastSolidityBlockNum);
      dbManager.getDynamicPropertiesStore().saveLatestSolidifiedBlockNum(headBlockNum);
    }
  }

  private void loopProcessBlock(Block block) {
    while (true) {
      long blockNum = block.getBlockHeader().getRawData().getNumber();
      try {
        dbManager.pushVerifiedBlock(new BlockCapsule(block));
        dbManager.getDynamicPropertiesStore().saveLatestSolidifiedBlockNum(blockNum);
        return;
      } catch (Exception e) {
        logger.error("Failed to process block {}.", blockNum);
        try {
          sleep(100);
          block = databaseGrpcClient.getBlock(blockNum);
        } catch (Exception e1) {
          logger.error(e1.getMessage());
        }
      }
    }
  }

  private void processTrx() {
    while (flag) {
      try {
        Block block = blockBakQueue.take();
        BlockCapsule blockCapsule = new BlockCapsule(block);
        for (TransactionCapsule trx : blockCapsule.getTransactions()) {
          TransactionInfoCapsule ret;
          try {
            ret = dbManager.getTransactionHistoryStore().get(trx.getTransactionId().getBytes());
          } catch (Exception ex) {
            logger.warn("Failed to get trx: {}", trx.getTransactionId(), ex);
            continue;
          }
          ret.setBlockNumber(blockCapsule.getNum());
          ret.setBlockTimeStamp(blockCapsule.getTimeStamp());
          dbManager.getTransactionHistoryStore().put(trx.getTransactionId().getBytes(), ret);
        }
      } catch (Exception e) {
        logger.error(e.getMessage());
        sleep(100);
      }
    }
  }

  public void sleep(long time) {
    try {
      Thread.sleep(time);
    } catch (Exception e1) {
    }
  }

  /**
   * Start the SolidityNode.
   */
  public static void main(String[] args) throws InterruptedException {
    logger.info("Solidity node running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    Args cfgArgs = Args.getInstance();

    logger.info("index switch is {}",
        BooleanUtils.toStringOnOff(BooleanUtils.toBoolean(cfgArgs.getStorage().getIndexSwitch())));
    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)LoggerFactory
        .getLogger(Logger.ROOT_LOGGER_NAME);
    root.setLevel(Level.toLevel(cfgArgs.getLogLevel()));

    if (StringUtils.isEmpty(cfgArgs.getTrustNodeAddr())) {
      logger.error("Trust node not set.");
      return;
    }
    cfgArgs.setSolidityNode(true);

    ApplicationContext context = new TronApplicationContext(DefaultConfig.class);

    if (cfgArgs.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }
    Application appT = ApplicationFactory.create(context);
    FullNode.shutdown(appT);

    //appT.init(cfgArgs);
    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    appT.addService(rpcApiService);
    //http
    SolidityNodeHttpApiService httpApiService = context.getBean(SolidityNodeHttpApiService.class);
    appT.addService(httpApiService);

    appT.initServices(cfgArgs);
    appT.startServices();
//    appT.startup();

    //Disable peer discovery for solidity node
    DiscoverServer discoverServer = context.getBean(DiscoverServer.class);
    discoverServer.close();
    ChannelManager channelManager = context.getBean(ChannelManager.class);
    channelManager.close();
    NodeManager nodeManager = context.getBean(NodeManager.class);
    nodeManager.close();

    SolidityNode node = new SolidityNode(appT.getDbManager(), cfgArgs);
    node.start();

    rpcApiService.blockUntilShutdown();
  }
}
