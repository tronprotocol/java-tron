package org.tron.p2p.connection.business.detect;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.business.MessageProcess;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.detect.StatusMessage;
import org.tron.p2p.connection.socket.PeerClient;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.NodeManager;

@Slf4j(topic = "net")
public class NodeDetectService implements MessageProcess {

  private PeerClient peerClient;

  private Map<InetSocketAddress, NodeStat> nodeStatMap = new ConcurrentHashMap<>();

  @Getter
  private static final Cache<InetAddress, Long> badNodesCache = CacheBuilder
      .newBuilder().maximumSize(5000).expireAfterWrite(1, TimeUnit.HOURS).build();

  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
      BasicThreadFactory.builder().namingPattern("nodeDetectService").build());

  private final long NODE_DETECT_THRESHOLD = 5 * 60 * 1000;

  private final long NODE_DETECT_MIN_THRESHOLD = 30 * 1000;

  private final long NODE_DETECT_TIMEOUT = 2 * 1000;

  private final int MAX_NODE_SLOW_DETECT = 3;

  private final int MAX_NODE_NORMAL_DETECT = 10;

  private final int MAX_NODE_FAST_DETECT = 100;

  private final int MAX_NODES = 300;

  private final int MIN_NODES = 200;


  public void init(PeerClient peerClient) {
    if (!Parameter.p2pConfig.isNodeDetectEnable()) {
      return;
    }
    this.peerClient = peerClient;
    executor.scheduleWithFixedDelay(() -> {
      try {
        work();
      } catch (Exception t) {
        log.warn("Exception in node detect worker, {}", t.getMessage());
      }
    }, 1, 5, TimeUnit.SECONDS);
  }

  public void close() {
    executor.shutdown();
  }

  public void work() {
    trimNodeMap();
    if (nodeStatMap.size() < MIN_NODES) {
      loadNodes();
    }

    List<NodeStat> nodeStats = getSortedNodeStats();
    if (nodeStats.size() == 0) {
      return;
    }

    NodeStat nodeStat = nodeStats.get(0);
    if (nodeStat.getLastDetectTime() > System.currentTimeMillis() - NODE_DETECT_MIN_THRESHOLD) {
      return;
    }

    int n = MAX_NODE_NORMAL_DETECT;
    if (nodeStat.getLastDetectTime() > System.currentTimeMillis() - NODE_DETECT_THRESHOLD) {
      n = MAX_NODE_SLOW_DETECT;
    }

    n = Math.min(n, nodeStats.size());

    for (int i = 0; i < n; i++) {
      detect(nodeStats.get(i));
    }
  }

  public void trimNodeMap() {
    long now = System.currentTimeMillis();
    nodeStatMap.forEach((k, v) -> {
      if (!v.finishDetect() && v.getLastDetectTime() < now - NODE_DETECT_TIMEOUT) {
        nodeStatMap.remove(k);
        badNodesCache.put(k.getAddress(), System.currentTimeMillis());
      }
    });
  }

  private void loadNodes() {
    int size = nodeStatMap.size();
    int count = 0;
    List<Node> nodes = NodeManager.getConnectableNodes();
    for (Node node : nodes) {
      InetSocketAddress socketAddress = node.getPreferInetSocketAddress();
      if (socketAddress != null
          && !nodeStatMap.containsKey(socketAddress)
          && badNodesCache.getIfPresent(socketAddress.getAddress()) == null) {
        NodeStat nodeStat = new NodeStat(node);
        nodeStatMap.put(socketAddress, nodeStat);
        detect(nodeStat);
        count++;
        if (count >= MAX_NODE_FAST_DETECT || count + size >= MAX_NODES) {
          break;
        }
      }
    }
  }

  private void detect(NodeStat stat) {
    try {
      stat.setTotalCount(stat.getTotalCount() + 1);
      setLastDetectTime(stat);
      peerClient.connectAsync(stat.getNode(), true);
    } catch (Exception e) {
      log.warn("Detect node {} failed, {}",
          stat.getNode().getPreferInetSocketAddress(), e.getMessage());
      nodeStatMap.remove(stat.getSocketAddress());
    }
  }

  public synchronized void processMessage(Channel channel, Message message) {
    StatusMessage statusMessage = (StatusMessage) message;

    if (!channel.isActive()) {
      channel.setDiscoveryMode(true);
      channel.send(new StatusMessage());
      channel.getCtx().close();
      return;
    }

    InetSocketAddress socketAddress = channel.getInetSocketAddress();
    NodeStat nodeStat = nodeStatMap.get(socketAddress);
    if (nodeStat == null) {
      return;
    }

    long cost = System.currentTimeMillis() - nodeStat.getLastDetectTime();
    if (cost > NODE_DETECT_TIMEOUT
        || statusMessage.getRemainConnections() == 0) {
      badNodesCache.put(socketAddress.getAddress(), cost);
      nodeStatMap.remove(socketAddress);
    }

    nodeStat.setLastSuccessDetectTime(nodeStat.getLastDetectTime());
    setStatusMessage(nodeStat, statusMessage);

    channel.getCtx().close();
  }

  public void notifyDisconnect(Channel channel) {

    if (!channel.isActive()) {
      return;
    }

    InetSocketAddress socketAddress = channel.getInetSocketAddress();
    if (socketAddress == null) {
      return;
    }

    NodeStat nodeStat = nodeStatMap.get(socketAddress);
    if (nodeStat == null) {
      return;
    }

    if (nodeStat.getLastDetectTime() != nodeStat.getLastSuccessDetectTime()) {
      badNodesCache.put(socketAddress.getAddress(), System.currentTimeMillis());
      nodeStatMap.remove(socketAddress);
    }
  }

  private synchronized List<NodeStat> getSortedNodeStats() {
    List<NodeStat> nodeStats = new ArrayList<>(nodeStatMap.values());
    nodeStats.sort(Comparator.comparingLong(o -> o.getLastDetectTime()));
    return nodeStats;
  }

  private synchronized void setLastDetectTime(NodeStat nodeStat) {
    nodeStat.setLastDetectTime(System.currentTimeMillis());
  }

  private synchronized void setStatusMessage(NodeStat nodeStat, StatusMessage message) {
    nodeStat.setStatusMessage(message);
  }

  public synchronized List<Node> getConnectableNodes() {
    List<NodeStat> stats = new ArrayList<>();
    List<Node> nodes = new ArrayList<>();
    nodeStatMap.values().forEach(stat -> {
      if (stat.getStatusMessage() != null) {
        stats.add(stat);
      }
    });

    if (stats.isEmpty()) {
      return nodes;
    }

    stats.sort(Comparator.comparingInt(o -> -o.getStatusMessage().getRemainConnections()));
    stats.forEach(stat -> nodes.add(stat.getNode()));
    return nodes;
  }

}
