package org.tron.program;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.Constant;
import org.tron.core.actuator.VMActuator;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.BlockIndexStore;
import org.tron.core.db.BlockStore;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.services.interfaceJsonRpcOnPBFT.JsonRpcServiceOnPBFT;
import org.tron.core.services.interfaceJsonRpcOnSolidity.JsonRpcServiceOnSolidity;
import org.tron.core.services.interfaceOnPBFT.RpcApiServiceOnPBFT;
import org.tron.core.services.interfaceOnPBFT.http.PBFT.HttpApiOnPBFTService;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;
import org.tron.core.services.interfaceOnSolidity.http.solidity.HttpApiOnSolidityService;
import org.tron.core.services.jsonrpc.FullNodeJsonRpcHttpService;
import org.tron.core.store.AccountStore;
import org.tron.core.store.TransactionRetStore;
import org.tron.core.db.Manager;

@Slf4j(topic = "app")
public class FullNode {

  public static final int dbVersion = 2;

  public static volatile boolean shutDownSign = false;


  private static Manager dbManager;
  private static TransactionRetStore transactionRetStore;
  private static BlockStore blockStore;
  private static BlockIndexStore blockIndexStore;
  private static AccountStore accountStore;


  public static void load(String path) {
    try {
      File file = new File(path);
      if (!file.exists() || !file.isFile() || !file.canRead()) {
        return;
      }
      LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
      JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(lc);
      lc.reset();
      configurator.doConfigure(file);
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
  }

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) {
    logger.info("Full node running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    CommonParameter parameter = Args.getInstance();

    load(parameter.getLogbackPath());

    if (parameter.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }

    if (Args.getInstance().isDebug()) {
      logger.info("in debug mode, it won't check energy time");
    } else {
      logger.info("not in debug mode, it will check energy time");
    }

    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.setAllowCircularReferences(false);
    TronApplicationContext context =
        new TronApplicationContext(beanFactory);
    context.register(DefaultConfig.class);

    context.refresh();
    Application appT = ApplicationFactory.create(context);
    shutdown(appT);

    System.out.println(" >>>>>>>>>>> start");

    dbManager = appT.getDbManager();
    blockStore = dbManager.getBlockStore();
    blockIndexStore = dbManager.getBlockIndexStore();
    transactionRetStore = dbManager.getTransactionRetStore();
    accountStore = dbManager.getAccountStore();

    final long headBlockNum = dbManager.getHeadBlockNum();
    System.out.println(" >>>>>>>>>>> headBlockNum" + headBlockNum);

    List<Long> blockNums = LongStream.rangeClosed(38582727L, 38583836L).boxed().collect(Collectors.toList());
    handleBlock(blockNums);

    while (!allOver()) {
      try {
        Thread.sleep(1000);
      } catch (Exception ex) {
        logger.error("", ex);
      }
    }

    System.out.println(" >>>>>>>>>>> main is end!!!!!!!!");
    context.destroy();
    context.close();
    System.exit(0);
  }

  public static void shutdown(final Application app) {
    logger.info("********register application shutdown hook********");
    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
  }

  private static void handleBlock(List<Long> blockNums) {
    for(long blockNum : blockNums){
      BlockCapsule blockCapsule = getBlockByNum(blockNum);
      dbManager.postBlockContractLogTrigger(blockCapsule);
    }
  }

  private static BlockCapsule getBlockByNum(long num) {
    BlockCapsule blockCapsule = null;
    try {
      blockCapsule = blockStore.get(blockIndexStore.get(num).getBytes());
    } catch (Exception e) {
      logger.error(" >>> get block error, num:{}", num);
    }
    return blockCapsule;
  }

  private static boolean allOver(){
    return dbManager.triggerCapsuleQueue.isEmpty();
  }
}
