package org.tron.p2p;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.NodeManager;
import org.tron.p2p.dns.DnsManager;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.stats.P2pStats;
import org.tron.p2p.stats.StatsManager;

@Slf4j(topic = "net")
public class P2pService {

  private StatsManager statsManager = new StatsManager();
  private volatile boolean isShutdown = false;

  public void start(P2pConfig p2pConfig) {
    Parameter.p2pConfig = p2pConfig;
    NodeManager.init();
    ChannelManager.init();
    DnsManager.init();
    log.info("P2p service started");

    Runtime.getRuntime().addShutdownHook(new Thread(this::close));
  }

  public void close() {
    if (isShutdown) {
      return;
    }
    isShutdown = true;
    DnsManager.close();
    NodeManager.close();
    ChannelManager.close();
    log.info("P2p service closed");
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

  public void updateNodeId(Channel channel, String nodeId) {
    ChannelManager.updateNodeId(channel, nodeId);
  }

  public int getVersion() {
    return Parameter.version;
  }
}
