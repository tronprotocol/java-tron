package org.tron.core.config.args;

import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import com.typesafe.config.Config;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.Constant;
import org.tron.core.config.Configuration;
import org.tron.core.net.TronNetService;


@Slf4j(topic = "app")
@Component
public class DynamicArgs {
  private final CommonParameter parameter = Args.getInstance();

  private long lastModified = 0;

  private ScheduledExecutorService reloadExecutor = Executors.newSingleThreadScheduledExecutor();

  public void init() {
    if (parameter.isDynamicConfigEnable()) {
      logger.info("Start the dynamic loading configuration service");
      long checkInterval = parameter.getDynamicConfigCheckInterval();
      File config = getConfigFile();
      if (config == null) {
        return;
      }
      lastModified = config.lastModified();
      reloadExecutor.scheduleWithFixedDelay(() -> {
        try {
          run();
        } catch (Exception e) {
          logger.error("Exception caught when reloading configuration", e);
        }
      }, 10, checkInterval, TimeUnit.SECONDS);
    }
  }

  public void run() {
    File config = getConfigFile();
    if (config != null) {
      long lastModifiedTime = config.lastModified();
      if (lastModifiedTime > lastModified) {
        reload();
        lastModified = lastModifiedTime;
      }
    }
  }

  private File getConfigFile() {
    String confFilePath;
    if (isNoneBlank(parameter.getShellConfFileName())) {
      confFilePath = parameter.getShellConfFileName();
    } else  {
      confFilePath = Constant.TESTNET_CONF;
    }

    File confFile = new File(confFilePath);
    if (!confFile.exists()) {
      logger.warn("Configuration path is required! No such file {}", confFile);
      return null;
    }
    return confFile;
  }

  public void reload() {
    logger.debug("Reloading ... ");
    Config config = Configuration.getByFileName(parameter.getShellConfFileName(),
        Constant.TESTNET_CONF);

    updateActiveNodes(config);

    updateTrustNodes(config);
  }

  private void updateActiveNodes(Config config) {
    List<InetSocketAddress> newActiveNodes =
        Args.getInetSocketAddress(config, Constant.NODE_ACTIVE, true);
    parameter.setActiveNodes(newActiveNodes);
    List<InetSocketAddress> activeNodes = TronNetService.getP2pConfig().getActiveNodes();
    activeNodes.clear();
    activeNodes.addAll(newActiveNodes);
    logger.debug("p2p active nodes : {}",
        TronNetService.getP2pConfig().getActiveNodes().toString());
  }

  private void updateTrustNodes(Config config) {
    List<InetAddress> newPassiveNodes = Args.getInetAddress(config, Constant.NODE_PASSIVE);
    parameter.setPassiveNodes(newPassiveNodes);
    List<InetAddress> trustNodes = TronNetService.getP2pConfig().getTrustNodes();
    trustNodes.clear();
    trustNodes.addAll(newPassiveNodes);
    parameter.getActiveNodes().forEach(n -> trustNodes.add(n.getAddress()));
    parameter.getFastForwardNodes().forEach(f -> trustNodes.add(f.getAddress()));
    logger.debug("p2p trust nodes : {}",
        TronNetService.getP2pConfig().getTrustNodes().toString());
  }

  public void close() {
    logger.info("Closing the dynamic loading configuration service");
    reloadExecutor.shutdown();
  }
}