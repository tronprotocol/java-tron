package org.tron.program;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.StringUtils;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
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
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.BadNumberBlockException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.NonCommonBlockException;
import org.tron.core.exception.OutOfSlotTimeException;
import org.tron.core.exception.ReceiptException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.TransactionTraceException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.ValidateScheduleException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.http.solidity.SolidityNodeHttpApiService;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.DynamicProperties;

@Slf4j
public class SolidityNode {

  private DatabaseGrpcClient databaseGrpcClient;
  private Manager dbManager;

  private ScheduledExecutorService syncExecutor = Executors.newSingleThreadScheduledExecutor();

  public void setDbManager(Manager dbManager) {
    this.dbManager = dbManager;
  }

  private void initGrpcClient(String addr) {
    try {
      databaseGrpcClient = new DatabaseGrpcClient(addr);
    } catch (Exception e) {
      logger.error("Failed to create database grpc client {}", addr);
      System.exit(0);
    }
  }

  private void shutdownGrpcClient() {
    if (databaseGrpcClient != null) {
      databaseGrpcClient.shutdown();
    }
  }

  private void syncLoop(Args args) {
//    while (true) {
//      try {
//        initGrpcClient(args.getTrustNodeAddr());
//        syncSolidityBlock();
//        shutdownGrpcClient();
//      } catch (Exception e) {
//        logger.error("Error in sync solidity block " + e.getMessage(), e);
//      }
//      try {
//        Thread.sleep(5000);
//      } catch (InterruptedException e) {
//        Thread.currentThread().interrupt();
//        e.printStackTrace();
//      }
//    }
  }

  private void syncSolidityBlock() throws BadBlockException {
    DynamicProperties remoteDynamicProperties = databaseGrpcClient.getDynamicProperties();
    long remoteLastSolidityBlockNum = remoteDynamicProperties.getLastSolidityBlockNum();
    while (true) {

//      try {
//        Thread.sleep(10000);
//      } catch (Exception e) {
//
//      }
      long lastSolidityBlockNum = dbManager.getDynamicPropertiesStore()
          .getLatestSolidifiedBlockNum();
      logger.info("sync solidity block, lastSolidityBlockNum:{}, remoteLastSolidityBlockNum:{}",
          lastSolidityBlockNum, remoteLastSolidityBlockNum);
      if (lastSolidityBlockNum < remoteLastSolidityBlockNum) {
        Block block = databaseGrpcClient.getBlock(lastSolidityBlockNum + 1);
        try {
          BlockCapsule blockCapsule = new BlockCapsule(block);
          dbManager.pushBlock(blockCapsule);
          for (TransactionCapsule trx : blockCapsule.getTransactions()) {
            TransactionInfoCapsule ret;
            try {
              ret = dbManager.getTransactionHistoryStore().get(trx.getTransactionId().getBytes());
            } catch (BadItemException ex) {
              logger.warn("", ex);
              continue;
            }
            ret.setBlockNumber(blockCapsule.getNum());
            ret.setBlockTimeStamp(blockCapsule.getTimeStamp());
            dbManager.getTransactionHistoryStore().put(trx.getTransactionId().getBytes(), ret);
          }
          dbManager.getDynamicPropertiesStore()
              .saveLatestSolidifiedBlockNum(lastSolidityBlockNum + 1);
        } catch (AccountResourceInsufficientException e) {
          throw new BadBlockException("validate AccountResource exception");
        } catch (ValidateScheduleException e) {
          throw new BadBlockException("validate schedule exception");
        } catch (ValidateSignatureException e) {
          throw new BadBlockException("validate signature exception");
        } catch (ContractValidateException e) {
          throw new BadBlockException("ContractValidate exception");
        } catch (ContractExeException | UnLinkedBlockException e) {
          throw new BadBlockException("Contract Execute exception");
        } catch (TaposException e) {
          throw new BadBlockException("tapos exception");
        } catch (DupTransactionException e) {
          throw new BadBlockException("dup exception");
        } catch (TooBigTransactionException e) {
          throw new BadBlockException("too big exception");
        } catch (TransactionExpirationException e) {
          throw new BadBlockException("expiration exception");
        } catch (BadNumberBlockException e) {
          throw new BadBlockException("bad number exception");
        } catch (ReceiptException e) {
          throw new BadBlockException("Receipt exception");
        } catch (NonCommonBlockException e) {
          throw new BadBlockException("non common exception");
        } catch (TransactionTraceException e) {
          throw new BadBlockException("TransactionTrace Exception");
        } catch (OutOfSlotTimeException e) {
          throw new BadBlockException("OutOfSlotTime Exception");
        }

      } else {
        break;
      }
    }
    logger.info("Sync with trust node completed!!!");
  }

  private void start(Args cfgArgs) {
    syncExecutor.scheduleWithFixedDelay(() -> {
      try {
        initGrpcClient(cfgArgs.getTrustNodeAddr());
        syncSolidityBlock();
        shutdownGrpcClient();
      } catch (Throwable t) {
        logger.error("Error in sync solidity block " + t.getMessage(), t);
      }
    }, 5000, 5000, TimeUnit.MILLISECONDS);
    //new Thread(() -> syncLoop(cfgArgs), logger.getName()).start();
  }

  /**
   * Start the SolidityNode.
   */
  public static void main(String[] args) throws InterruptedException {
    logger.info("Solidity node running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    Args cfgArgs = Args.getInstance();

    if (StringUtils.isEmpty(cfgArgs.getTrustNodeAddr())) {
      logger.error("Trust node not set.");
      return;
    }
    cfgArgs.setSolidityNode(true);

    ApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);

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

    SolidityNode node = new SolidityNode();
    node.setDbManager(appT.getDbManager());
    node.start(cfgArgs);

    rpcApiService.blockUntilShutdown();
  }
}
