package org.tron.program;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.arch.Arch;
import org.tron.common.exit.ExitManager;
import org.tron.common.log.LogService;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.Metrics;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.config.args.CLIParameter;
import org.tron.core.config.args.CommandLineArguments;
import org.tron.core.exception.TronError;
import org.tron.core.services.admin.ipc.client.IpcClient;

public class FullNode {

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) {
    // Do not initialize loggers, Args, Arch, or ExitManager before dispatching attach mode:
    // their static initializers open the node's Logback appenders.
    CommandLineArguments arguments = new CommandLineArguments(args);
    if (arguments.isAttachMode()) {
      CLIParameter clientParameters = arguments.getParameters();
      IpcClient ipcClient = new IpcClient(clientParameters.ipcSocketFile);
      int exitCode = ipcClient.start(clientParameters.ipcExecCommand);
      if (exitCode != 0) {
        System.exit(exitCode);
      }
      return;
    }

    ExitManager.initExceptionHandler();
    checkJdkVersion();
    Args.setParam(arguments, "config.conf");
    CommonParameter parameter = Args.getInstance();
    LogService.load(parameter.getLogbackPath());
    Logger logger = LoggerFactory.getLogger("app");

    if (parameter.isKeystoreFactory()) {
      KeystoreFactory.start();
      return;
    }
    if (parameter.isSolidityNode()) {
      logger.info("Solidity node is running.");
      if (StringUtils.isEmpty(parameter.getTrustNodeAddr())) {
        throw new TronError(new IllegalArgumentException("Trust node is not set."),
            TronError.ErrCode.SOLID_NODE_INIT);
      }
    } else {
      logger.info("Full node running.");
      if (Args.getInstance().isDebug()) {
        logger.info("in debug mode, it won't check energy time");
      } else {
        logger.info("not in debug mode, it will check energy time");
      }
    }

    // init metrics first
    Metrics.init();

    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.setAllowCircularReferences(false);
    TronApplicationContext context =
        new TronApplicationContext(beanFactory);
    context.register(DefaultConfig.class);
    context.refresh();
    Application appT = ApplicationFactory.create(context);
    context.registerShutdownHook();
    appT.startup();
    if (parameter.isSolidityNode()) {
      SolidityNode node = context.getBean(SolidityNode.class);
      node.run();
    }
    appT.blockUntilShutdown();
  }

  private static void checkJdkVersion() {
    try {
      Arch.throwIfUnsupportedJavaVersion();
    } catch (UnsupportedOperationException e) {
      System.err.println(e.getMessage());
      throw new TronError(e, TronError.ErrCode.JDK_VERSION);
    }
  }
}
