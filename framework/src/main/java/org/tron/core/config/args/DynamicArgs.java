package org.tron.core.config.args;

import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import com.typesafe.config.Config;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
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

  private volatile boolean shutdown = false;

  public void init() {
    if (parameter.isDynamicConfigEnable()) {
      new Thread(this::start, "DynamicArgs").start();
    }
  }

  public void start() {
    WatchService watchService;
    Path path;
    String confFileName;
    try {
      logger.info("Start the dynamic loading configuration service");
      String confFile;
      if (isNoneBlank(parameter.getShellConfFileName())) {
        confFile = parameter.getShellConfFileName();
      } else  {
        confFile = Constant.TESTNET_CONF;
        //logger.warn("Configuration path is required!");
        //return;
      }

      File confDir = new File(confFile);
      if (!confDir.exists()) {
        logger.warn("Configuration path is required! No such file {}", confFile);
        return;
      }
      confFileName = confDir.getName();
      if (confFile.contains(File.separator)) {
        path = FileSystems.getDefault().getPath(confDir.getPath()).getParent();
      } else {
        File directory = new File("");
        path = FileSystems.getDefault().getPath(directory.getAbsolutePath());
      }

      logger.debug("confDirString = {}", confDir);
      watchService = FileSystems.getDefault().newWatchService();
      path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
      logger.debug("watch path : {}", path.toString());
    } catch (Exception e) {
      logger.error("Exception caught when register the watch key", e.getCause());
      return;
    }

    while (!shutdown) {
      try {
        WatchKey wk = watchService.take();
        long changeCount = 0;
        for (WatchEvent<?> event : wk.pollEvents()) {
          final Path changed = (Path)event.context();
          if (changed.endsWith(confFileName)) {
            reload();
            logger.info("The configuration was modified and we reloaded it");
          }
          changeCount++;
        }
        logger.debug("change count : {}", changeCount);

        boolean valid = wk.reset();
        if (!valid) {
          path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        }
      } catch (InterruptedException e) {
        logger.warn("WatchService was interrupted");
        break;
      } catch (IOException e) {
        logger.error("Exception caught when register the watch key", e.getCause());
        break;
      }
    }
  }

  public void reload() {
    logger.debug("Reloading ... ");
    Config config = Configuration.getByFileName(parameter.getShellConfFileName(),
        Constant.TESTNET_CONF);

    updateActiveNodes(config);

    updateTrustNodes(config);
  }

  private void updateActiveNodes(Config config) {
    if (parameter.isWitness() || parameter.isFastForward()) {
      return;
    }

    List<InetSocketAddress> lastActiveNodes = parameter.getActiveNodes();
    List<InetSocketAddress> newActiveNodes =
        Args.getInetSocketAddress(config, Constant.NODE_ACTIVE);
    parameter.setActiveNodes(newActiveNodes);
    parameter.getActiveNodes().forEach(n -> {
      if (!lastActiveNodes.contains(n)) {
        TronNetService.getP2pConfig().getActiveNodes().add(n);
        if (!TronNetService.getP2pConfig().getTrustNodes().contains(n.getAddress())) {
          TronNetService.getP2pConfig().getTrustNodes().add(n.getAddress());
        }
      }
    });

    lastActiveNodes.forEach(ln -> {
      if (!parameter.getActiveNodes().contains(ln)) {
        TronNetService.getP2pConfig().getActiveNodes().remove(ln);
        TronNetService.getP2pConfig().getTrustNodes().remove(ln.getAddress());
      }
    });
    logger.debug("p2p active nodes : {}",
        TronNetService.getP2pConfig().getActiveNodes().toString());
  }

  private void updateTrustNodes(Config config) {
    if (parameter.isWitness() || parameter.isFastForward()) {
      return;
    }

    List<InetAddress> lastPassiveNodes = parameter.getPassiveNodes();
    List<InetAddress> newPassiveNodes = Args.getInetAddress(config, Constant.NODE_PASSIVE);
    parameter.setPassiveNodes(newPassiveNodes);
    parameter.getPassiveNodes().forEach(n -> {
      if (!lastPassiveNodes.contains(n)
          || !TronNetService.getP2pConfig().getTrustNodes().contains(n)) {
        TronNetService.getP2pConfig().getTrustNodes().add(n);
      }
    });

    lastPassiveNodes.forEach(ln -> {
      if (!parameter.getPassiveNodes().contains(ln)) {
        TronNetService.getP2pConfig().getTrustNodes().remove(ln);
      }
    });
    logger.debug("p2p trust nodes : {}", TronNetService.getP2pConfig().getTrustNodes().toString());
  }

  public void close() {
    logger.info("Closing watchService ...");
    shutdown = true;
  }
}