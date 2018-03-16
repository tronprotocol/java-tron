package org.tron.program;

import com.google.inject.Injector;
import com.google.inject.Provides;
import com.sun.org.apache.xpath.internal.Arg;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.api.WalletApi;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;

public class FullNode {

  private static final Logger logger = LoggerFactory.getLogger("FullNode");

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) {
    Config config = Configuration.getByPath(Constant.NORMAL_CONF);
    Args.setParam(args, config);
    Args cfgArgs = Args.getInstance();

    Injector module = ApplicationFactory.buildGuice(config, cfgArgs);
    if (cfgArgs.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }

    logger.info("Here is the help message." + cfgArgs.getOutputDirectory());
    Application application = ApplicationFactory.create();
    application.init(cfgArgs.getOutputDirectory(), cfgArgs);

    WalletApi wallet = new WalletApi(
      new Wallet(application),
      application.getDbManager().getAccountStore(),
      application.getDbManager().getWitnessStore()
    );
    RpcApiService rpcApiService = new RpcApiService(wallet, config.getInt("rpc.port"));
    application.addService(rpcApiService);

    if (cfgArgs.isWitness()) {
      application.addService(new WitnessService(application.getP2pNode(), application.getDbManager()));
    }

    application.initServices();
    application.startServices();
    application.startup();
    rpcApiService.blockUntilShutdown();
  }
}
