package org.tron.common.overlay.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.common.backup.BackupManager;
import org.tron.common.backup.BackupManager.BackupStatusEnum;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db.WitnessStore;
import org.tron.core.services.WitnessService;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
public class FastForward {

  @Autowired
  private ApplicationContext ctx;

  private Manager manager;

  private WitnessStore witnessStore;

  private NodeManager nodeManager;

  private ChannelManager channelManager;

  private BackupManager backupManager;

  private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  private Args args = Args.getInstance();
  private List<Node> fastForwardNodes = args.getFastForwardNodes();
  private byte[] witnessAddress = args.getLocalWitnesses().getWitnessAccountAddress();
  private int keySize = args.getLocalWitnesses().getPrivateKeys().size();

  public void init() {

    logger.info("Fast forward config, isWitness: {}, keySize: {}, fastForwardNodes: {}",
        args.isWitness(), keySize, fastForwardNodes.size());

    if (!args.isWitness() || keySize == 0 || fastForwardNodes.size() == 0) {
      return;
    }

    manager = ctx.getBean(Manager.class);
    witnessStore = ctx.getBean(WitnessStore.class);
    nodeManager = ctx.getBean(NodeManager.class);
    channelManager = ctx.getBean(ChannelManager.class);
    backupManager = ctx.getBean(BackupManager.class);

    if (args.getFastForwardNodes().size() > 0) {
      fastForwardNodes = args.getFastForwardNodes();
    }

    executorService.scheduleWithFixedDelay(() -> {
      try {
        if (witnessStore.get(witnessAddress) != null &&
            backupManager.getStatus().equals(BackupStatusEnum.MASTER) &&
            !WitnessService.isNeedSyncCheck()) {
          connect();
        } else {
          disconnect();
        }
      } catch (Throwable t) {
        logger.info("Execute failed.", t);
      }
    }, 0, 1, TimeUnit.MINUTES);
  }

  private void connect() {
    fastForwardNodes.forEach(node -> {
      InetAddress address = new InetSocketAddress(node.getHost(), node.getPort()).getAddress();
      channelManager.getActiveNodes().put(address, node);
    });
  }

  private void disconnect() {
    fastForwardNodes.forEach(node -> {
      InetAddress address = new InetSocketAddress(node.getHost(), node.getPort()).getAddress();
      channelManager.getActiveNodes().remove(address);
      channelManager.getActivePeers().forEach(channel -> {
        if (channel.getNode().equals(node)) {
          channel.disconnect(ReasonCode.RESET);
        }
      });
    });
  }

}
