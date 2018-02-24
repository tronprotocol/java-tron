package org.tron.program;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;

public class FullNode {

  private static final Logger logger = LoggerFactory.getLogger("FullNode");

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) {
    Args.setParam(args);
    Args cfgArgs = Args.getInstance();
    if (cfgArgs.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }
    logger.info("Here is the help message." + cfgArgs.getOutputDirectory());
    Application appT = ApplicationFactory.create();
    appT.init(cfgArgs.getOutputDirectory(), cfgArgs);
    RpcApiService rpcApiService = new RpcApiService(appT);
    appT.addService(rpcApiService);
    appT.addService(new WitnessService(appT));
    appT.initServices(cfgArgs);
    appT.startServices();
    appT.startup();
    rpcApiService.blockUntilShutdown();
  }
}
