package org.tron.core.config.args;

import com.typesafe.config.Config;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.Configuration;
import org.tron.core.net.TronNetService;


@Slf4j(topic = "app")
@Component
public class DynamicArgs {
  private final CommonParameter parameter = Args.getInstance();

  private File configFile;
  private long lastModified = 0;

  private ScheduledExecutorService reloadExecutor;
  private final String esName = "dynamic-reload";

  @PostConstruct
  public void init() {
    if (parameter.isDynamicConfigEnable()) {
      reloadExecutor = ExecutorServiceManager.newSingleThreadScheduledExecutor(esName);
      logger.info("Start the dynamic loading configuration service");
      long checkInterval = parameter.getDynamicConfigCheckInterval();
      configFile = new File(Args.getConfigFilePath());
      if (!configFile.exists()) {
        logger.warn("Configuration path is required! No such file {}", configFile);
        return;
      }
      lastModified = configFile.lastModified();
      reloadExecutor.scheduleWithFixedDelay(() -> {
        try {
          run();
        } catch (Throwable e) {
          logger.error("Exception caught when reloading configuration", e);
        }
      }, 10, checkInterval, TimeUnit.SECONDS);
    }
  }

  public void run() {
    long lastModifiedTime = configFile.lastModified();
    if (lastModifiedTime > lastModified) {
      lastModified = lastModifiedTime;
      reload();
    }
  }

  public void reload() {
    logger.debug("Reloading ... ");
    Config config = Configuration.getByFileName(Args.getConfigFilePath());
    List<InetSocketAddress> active = Args.filterInetSocketAddress(
        config.getStringList("node.active"), true);
    List<InetSocketAddress> passive = Args.filterInetSocketAddress(
        config.getStringList("node.passive"), false);

    updateActiveNodes(active);
    updateTrustNodes(passive);
  }

  private void updateActiveNodes(List<InetSocketAddress> newActiveNodes) {
    parameter.setActiveNodes(newActiveNodes);
    List<InetSocketAddress> activeNodes = TronNetService.getP2pConfig().getActiveNodes();
    activeNodes.removeIf(node -> !newActiveNodes.contains(node));
    newActiveNodes.forEach(node -> {
      if (!activeNodes.contains(node)) {
        activeNodes.add(node);
      }
    });
    logger.debug("p2p active nodes : {}",
        TronNetService.getP2pConfig().getActiveNodes().toString());
  }

  private void updateTrustNodes(List<InetSocketAddress> passiveNodes) {
    List<InetAddress> newPassiveNodes = new java.util.ArrayList<>();
    for (InetSocketAddress sa : passiveNodes) {
      newPassiveNodes.add(sa.getAddress());
    }
    parameter.setPassiveNodes(newPassiveNodes);
    List<InetAddress> trustNodes = TronNetService.getP2pConfig().getTrustNodes();
    trustNodes.clear();
    trustNodes.addAll(newPassiveNodes);
    parameter.getActiveNodes().forEach(n -> trustNodes.add(n.getAddress()));
    parameter.getFastForwardNodes().forEach(f -> trustNodes.add(f.getAddress()));
    logger.debug("p2p trust nodes : {}",
        TronNetService.getP2pConfig().getTrustNodes().toString());
  }

  @PreDestroy
  public void close() {
    ExecutorServiceManager.shutdownAndAwaitTermination(reloadExecutor, esName);
  }
}
