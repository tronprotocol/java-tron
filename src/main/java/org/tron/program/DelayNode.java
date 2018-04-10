package org.tron.program;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;

@Slf4j
public class DelayNode {

  /**
   * Start the DelayNode.
   */
  public static void main(String[] args) throws InterruptedException {
    logger.info("DelayNode running.");
    Args.setParam(args, Constant.NORMAL_CONF);
    Args cfgArgs = Args.getInstance();
    cfgArgs.setDelayNode(true);

    ApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);

    if (cfgArgs.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }
    Application appT = ApplicationFactory.create(context);
    //appT.init(cfgArgs);
    RpcApiService rpcApiService = new RpcApiService(appT, context);
    appT.addService(rpcApiService);

    appT.initServices(cfgArgs);
    appT.startServices();
    appT.startup();

    rpcApiService.blockUntilShutdown();
  }
}
