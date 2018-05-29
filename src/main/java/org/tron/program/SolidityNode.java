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
import org.tron.common.overlay.discover.NodeManager;
import org.tron.common.overlay.discover.UDPListener;
import org.tron.common.overlay.server.ChannelManager;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadNumberBlockException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.ValidateBandwidthException;
import org.tron.core.exception.ValidateScheduleException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.services.RpcApiService;
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
      long lastSolidityBlockNum = dbManager.getDynamicPropertiesStore()
          .getLatestSolidifiedBlockNum();
      if (lastSolidityBlockNum < remoteLastSolidityBlockNum) {
        Block block = databaseGrpcClient.getBlock(lastSolidityBlockNum + 1);
        try {
          BlockCapsule blockCapsule = new BlockCapsule(block);
          dbManager.pushBlock(blockCapsule);
          dbManager.getDynamicPropertiesStore()
              .saveLatestSolidifiedBlockNum(lastSolidityBlockNum + 1);
        } catch (ValidateBandwidthException e) {
          throw new BadBlockException("validate Bandwidth exception");
        } catch (ValidateScheduleException e) {
          throw new BadBlockException("validate schedule exception");
        } catch (ValidateSignatureException e) {
          throw new BadBlockException("validate signature exception");
        } catch (ContractValidateException e) {
          throw new BadBlockException("ContractValidate exception");
        } catch (ContractExeException | UnLinkedBlockException e) {
          throw new BadBlockException("Contract Exectute exception");
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

    appT.initServices(cfgArgs);
    appT.startServices();
//    appT.startup();

    //Disable peer discovery for solidity node
    UDPListener udpListener = context.getBean(UDPListener.class);
    udpListener.close();
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
