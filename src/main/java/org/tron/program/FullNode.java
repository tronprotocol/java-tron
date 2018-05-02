package org.tron.program;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadBlockException;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;

@Slf4j
public class FullNode {

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) throws InterruptedException {
    logger.info("Full node running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    Args cfgArgs = Args.getInstance();

    if (cfgArgs.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }

    if (cfgArgs.isNeedReplay()) {
      String dataBaseDir = cfgArgs.getOutputDirectory();
      ReplayTool.cleanDb(dataBaseDir);
    }

    ApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);

    if (cfgArgs.isNeedReplay()) {
      Manager dbManager = context.getBean(Manager.class);
      try {
        if (cfgArgs.getReplayTo() > 0) {
          ReplayTool.replayBlock(dbManager, cfgArgs.getReplayTo());
        } else {
          ReplayTool.replayBlock(dbManager);
        }
      } catch (BadBlockException e) {
        logger.info("Replay failed", e.getMessage());
      }
    }

    Application appT = ApplicationFactory.create(context);
    shutdown(appT);
    //appT.init(cfgArgs);
    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    appT.addService(rpcApiService);
    if (cfgArgs.isWitness()) {
      appT.addService(new WitnessService(appT));
    }
    appT.initServices(cfgArgs);
    appT.startServices();
    appT.startup();
    rpcApiService.blockUntilShutdown();
  }

  private static void shutdown(final Application app) {
    logger.info("********register application shutdown ********");
    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
  }
}
