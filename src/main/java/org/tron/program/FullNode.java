package org.tron.program;

import com.beust.jcommander.JCommander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;

public class FullNode {

  private static final Logger logger = LoggerFactory.getLogger("FullNode");

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) {

    Args cfgArgs = new Args();
    JCommander.newBuilder()
        .addObject(cfgArgs)
        .build()
        .parse(args);
    if (cfgArgs.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }
    Application appT = ApplicationFactory.create();
    appT.init(cfgArgs.getOutputDirectory(), new Args());
    RpcApiService rpcApiService = new RpcApiService(appT);
    appT.addService(rpcApiService);
    appT.addService(new WitnessService(appT));
    appT.startServices();
    appT.startup();
    rpcApiService.blockUntilShutdown();
  }
}
