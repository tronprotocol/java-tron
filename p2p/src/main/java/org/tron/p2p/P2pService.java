package org.tron.p2p;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.ConnectionPolicy;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.NodeManager;
import org.tron.p2p.dns.DnsManager;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.stats.P2pStats;
import org.tron.p2p.stats.StatsManager;
import org.tron.p2p.utils.NetUtil;

@Slf4j(topic = "net")
public class P2pService {

  private StatsManager statsManager = new StatsManager();
  private volatile boolean isShutdown = false;

  public void start(P2pConfig p2pConfig) {
    Parameter.p2pConfig = p2pConfig;
    ConnectionPolicy.replaceBlockedIps(p2pConfig.getBlockedIps());
    try {
      NodeManager.init();
      ChannelManager.init();
      DnsManager.init();
      Runtime.getRuntime().addShutdownHook(new Thread(this::close));
      logger.info("P2p service started");
    } catch (RuntimeException e) {
      try {
        close();
      } catch (RuntimeException cleanupError) {
        e.addSuppressed(cleanupError);
      }
      throw e;
    }
  }

  public void close() {
    if (isShutdown) {
      return;
    }
    isShutdown = true;
    DnsManager.close();
    NodeManager.close();
    ChannelManager.close();
    logger.info("P2p service closed");
  }

  public void register(P2pEventHandler p2PEventHandler) throws P2pException {
    Parameter.addP2pEventHandle(p2PEventHandler);
  }

  @Deprecated
  public void connect(InetSocketAddress address) {
    ChannelManager.connect(address);
  }

  public ChannelFuture connect(Node node, ChannelFutureListener future) {
    return ChannelManager.connect(node, future);
  }

  public boolean addActiveNode(InetSocketAddress address) {
    NetUtil.validateInetSocketAddress(address);
    if (ConnectionPolicy.isBlocked(address)) {
      logger.info("Reject adding active node {} because its IP is manually blocked", address);
      return false;
    }
    P2pConfig p2pConfig = Parameter.p2pConfig;
    List<InetSocketAddress> activeNodes = p2pConfig.getActiveNodes();
    boolean changed;
    synchronized (activeNodes) {
      changed = !activeNodes.contains(address) && activeNodes.add(address);
    }
    if (changed) {
      logger.info("Added active node {}", address);
    }
    return changed;
  }

  public boolean removeActiveNode(InetSocketAddress address) {
    NetUtil.validateInetSocketAddress(address);
    List<InetSocketAddress> activeNodes = Parameter.p2pConfig.getActiveNodes();
    boolean changed;
    synchronized (activeNodes) {
      changed = activeNodes.remove(address);
    }
    if (changed) {
      logger.info("Removed active node {}", address);
    }
    return changed;
  }

  public int disconnect(InetSocketAddress address) {
    NetUtil.validateInetSocketAddress(address);
    return ChannelManager.disconnect(address);
  }

  public int replaceBlockedIps(Set<InetAddress> blockedIps) {
    ConnectionPolicy.replaceBlockedIps(blockedIps);
    int disconnectedCount = ChannelManager.disconnectBlockedIps();
    logger.info("Replaced blocked IPs, size {}, disconnected channels {}",
        blockedIps.size(), disconnectedCount);
    return disconnectedCount;
  }

  public P2pStats getP2pStats() {
    return statsManager.getP2pStats();
  }

  public List<Node> getTableNodes() {
    return NodeManager.getTableNodes();
  }

  public List<Node> getConnectableNodes() {
    Set<Node> nodes = new HashSet<>();
    nodes.addAll(NodeManager.getConnectableNodes());
    nodes.addAll(DnsManager.getDnsNodes());
    return new ArrayList<>(nodes);
  }

  public List<Node> getAllNodes() {
    Set<Node> nodes = new HashSet<>();
    nodes.addAll(NodeManager.getAllNodes());
    nodes.addAll(DnsManager.getDnsNodes());
    return new ArrayList<>(nodes);
  }

  /**
   * @deprecated Node IDs are set during the libp2p handshake, and self-connection and
   *     duplicate-peer checks are handled by {@link ChannelManager#checkPeer(Channel)} before
   *     registration. Application code should validate node ID consistency instead of updating it.
   */
  @Deprecated
  public void updateNodeId(Channel channel, String nodeId) {
    ChannelManager.updateNodeId(channel, nodeId);
  }

  public int getVersion() {
    return Parameter.version;
  }
}
