package org.tron.program;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.util.ObjectUtils;
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
import org.tron.core.exception.TronError;

@Slf4j(topic = "app")
public class FullNode {

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) {
    ExitManager.initExceptionHandler();
    checkJdkVersion();
    Args.setParam(args, "config.conf");
    CommonParameter parameter = Args.getInstance();

    LogService.load(parameter.getLogbackPath());

    if (parameter.isKeystoreFactory()) {
      KeystoreFactory.start();
      return;
    }
    if (parameter.isSolidityNode()) {
      logger.info("Solidity node is running.");
      if (ObjectUtils.isEmpty(parameter.getTrustNodeAddr())) {
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
