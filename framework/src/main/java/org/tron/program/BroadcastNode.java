package org.tron.program;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.exit.ExitManager;
import org.tron.common.log.LogService;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.Metrics;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.TronNetService;
import org.tron.program.broadcast.BroadcastRelay;

@Slf4j(topic = "app")
public class BroadcastNode {

  private TronApplicationContext context;
  private Application app;
  private TronNetService tronNetService;
  private TronNetDelegate tronNetDelegate;
  private Manager dbManager;

  /**
   * Start the BroadcastNode.
   */
  public static void main(String[] args) {
    ExitManager.initExceptionHandler();
    logger.info("Broadcast node running.");

    Args.setParam(args, Constant.TESTNET_CONF);
    CommonParameter parameter = Args.getInstance();

    LogService.load(parameter.getLogbackPath());

    if (Args.getInstance().isDebug()) {
      logger.info("in debug mode, it won't check energy time");
    } else {
      logger.info("not in debug mode, it will check energy time");
    }

    // init metrics first
    Metrics.init();

    BroadcastNode broadcastNode = new BroadcastNode();
    try {
      broadcastNode.startup();
      // Wait for peers
      // broadcastNode.waitForPeers();
      broadcastNode.runBroadcast();
    } catch (Exception e) {
      logger.error("BroadcastNode startup failed", e);
      System.exit(1);
    }
    broadcastNode.app.blockUntilShutdown();
  }

  private void startup() {
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.setAllowCircularReferences(false);
    context = new TronApplicationContext(beanFactory);
    context.register(DefaultConfig.class);
    context.refresh();
    context.registerShutdownHook();

    app = ApplicationFactory.create(context);
    app.startup();

    tronNetDelegate = context.getBean(TronNetDelegate.class);
    tronNetService = context.getBean(TronNetService.class);
    dbManager = context.getBean(Manager.class);
  }

  private void runBroadcast() throws InterruptedException {
    logger.info("Starting broadcast relay mode");
    BroadcastRelay broadcastRelay = new BroadcastRelay(tronNetService, dbManager);
    broadcastRelay.broadcastTransactions();
  }

  private void waitForPeers() {
    while (getBroadCastPeerCount() <= 0) {
      logger.warn("No available peers to broadcast, please wait");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  private int getBroadCastPeerCount() {
    return (int) tronNetDelegate.getActivePeer().stream()
        .filter(p -> !p.isNeedSyncFromPeer() && !p.isNeedSyncFromUs())
        .count();
  }

}
