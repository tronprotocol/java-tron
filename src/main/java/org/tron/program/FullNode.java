package org.tron.program;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.core.Constant;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;

@Slf4j
public class FullNode {
  /**
   * Start the FullNode.
   */
  public static void main(String[] args) {
    Args.setParam(args, Configuration.getByPath(Constant.NORMAL_CONF));
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
    if (cfgArgs.isWitness()) {
      appT.addService(new WitnessService(appT));
    }
    appT.initServices(cfgArgs);
    appT.startServices();
    appT.startup();
    rpcApiService.blockUntilShutdown();
  }
}
