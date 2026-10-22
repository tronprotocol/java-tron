package org.tron.program;

import com.beust.jcommander.ParameterException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.arch.Arch;
import org.tron.common.backup.BackupManager;
import org.tron.common.exit.ExitManager;
import org.tron.common.log.LogService;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
import org.tron.consensus.dpos.MaintenanceManager;
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
    CommandLineArguments arguments = handleCommandLine(args);
    if (arguments == null) {
      return;
    }

    ExitManager.initExceptionHandler();
    checkJdkVersion();
    Args.setParam(arguments, "config.conf");
    CommonParameter parameter = Args.getInstance();
    LogService.load(parameter.getLogbackPath());
    Logger logger = LoggerFactory.getLogger("app");

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

    if (parameter.isNodeMetricsEnable()) {
      logger.warn("legacy metrics stack (node.metricsEnable) is deprecated and will be "
          + "removed in a future major release; migrate to node.metrics.prometheus.enable");
    }

    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.setAllowCircularReferences(false);
    TronApplicationContext context =
        new TronApplicationContext(beanFactory);
    context.register(DefaultConfig.class);
    context.refresh();
    Application appT = ApplicationFactory.create(context);
    context.registerShutdownHook();
    appT.startup();
    // the genesis block id (chainId) is only available after context refresh (Manager.initGenesis)
    Metrics.info(MetricKeys.Info.NODE_INFO, Version.getVersion(),
        Args.getInstance().getChainId());
    // the witness snapshot reads consensus miners, assembled during appT.startup()
    context.getBean(MaintenanceManager.class).recordWitnessMetrics();
    context.getBean(BackupManager.class).recordNodeRole();
    if (parameter.isSolidityNode()) {
      SolidityNode node = context.getBean(SolidityNode.class);
      node.run();
    }
    appT.blockUntilShutdown();
  }

  /** Returns node arguments, or null when command-line handling has completed. */
  private static CommandLineArguments handleCommandLine(String[] args) {
    // Do not initialize loggers, Args, Arch, or ExitManager before dispatching attach mode:
    // their static initializers open the node's Logback appenders.
    CommandLineArguments arguments;
    boolean attachMode;
    try {
      arguments = new CommandLineArguments(args);
      attachMode = arguments.isAttachMode();
    } catch (ParameterException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
      return null;
    } catch (TronError e) {
      if (e.getErrCode() != TronError.ErrCode.PARAMETER_INIT) {
        throw e;
      }
      // Argument validators already printed the error; do not initialize node logging here.
      System.exit(1);
      return null;
    }
    if (!attachMode) {
      return arguments;
    }
    CLIParameter clientParameters = arguments.getParameters();
    IpcClient ipcClient = new IpcClient(clientParameters.ipcSocketFile);
    int exitCode = ipcClient.start(clientParameters.ipcExecCommand);
    if (exitCode != 0) {
      System.exit(exitCode);
    }
    return null;
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
